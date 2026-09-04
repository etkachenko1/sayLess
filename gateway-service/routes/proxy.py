import json
import os
import time
from collections import defaultdict
from urllib.parse import unquote
from fastapi import APIRouter, Request, HTTPException, status, Response
import httpx
from dotenv import load_dotenv
from utils.jwt_utils import verify_jwt
from utils.limiter import limiter

load_dotenv()

router = APIRouter()

SERVICES = {
    "auth":    os.getenv("AUTH_SERVICE_URL",    "http://localhost:8081/auth"),
    "tasks":   os.getenv("TASKS_SERVICE_URL",   "http://localhost:8082/tasks"),
    "friends": os.getenv("FRIENDS_SERVICE_URL", "http://localhost:8083/friends"),
    "ai":      os.getenv("AI_SERVICE_URL",      "http://localhost:8084"),
    "users":   os.getenv("USERS_SERVICE_URL",   "http://localhost:8081/users"),
    "notifications":   os.getenv("NOTIFICATIONS_SERVICE_URL",   "http://localhost:8085/notifications"),
}

_TIMEOUT = httpx.Timeout(25.0)

_BLOCKED_ROUTES = {("ai", "train")}

def _is_path_safe(path: str) -> bool:
    if not path:
        return True
    decoded = path
    for _ in range(10):
        next_decoded = unquote(decoded)
        if next_decoded == decoded:
            break
        decoded = next_decoded
    else:
        return False  # didn't converge within 10 passes, treat as unsafe
    if "%" in decoded:
        return False  # residual percent-encoding after fixed-point decode
    return ".." not in decoded.split("/")

async def forward_request(service_name: str, path: str, request: Request):
    base_url = SERVICES.get(service_name)
    if not base_url:
        raise HTTPException(status_code=404, detail=f"Unknown service '{service_name}'")
    if not _is_path_safe(path):
        raise HTTPException(status_code=400, detail="Invalid path")
    if (service_name, path) in _BLOCKED_ROUTES:
        raise HTTPException(status_code=404, detail=f"Unknown service '{service_name}'")

    if service_name != "auth":
        auth_header = request.headers.get("authorization")
        if not auth_header or not auth_header.startswith("Bearer "):
            raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Missing token")
        token = auth_header.split(" ")[1]
        verify_jwt(token)

    forwarded_headers = {
        k: v for k, v in request.headers.items()
        if k.lower() != "host"
    }

    async with httpx.AsyncClient(timeout=_TIMEOUT) as client:
        try:
            res = await client.request(
                request.method,
                f"{base_url}/{path}" if path else base_url,
                headers=forwarded_headers,
                content=await request.body(),
                params=dict(request.query_params),
            )
        except httpx.TimeoutException:
            raise HTTPException(status_code=504, detail=f"Service '{service_name}' timed out")
        except httpx.ConnectError:
            raise HTTPException(status_code=503, detail=f"Service '{service_name}' is unavailable")

    return Response(
        content=res.content,
        status_code=res.status_code,
        headers=dict(res.headers),
    )

_LOGIN_FAILURE_WINDOW_SECONDS = 60
_LOGIN_FAILURE_LIMIT = 10
_login_failures_by_username: dict[str, list[float]] = defaultdict(list)

def _normalize_username(username: str) -> str:
    return username.strip().lower()

def _is_locked_out(key: str) -> bool:
    now = time.monotonic()
    attempts = _login_failures_by_username[key]
    attempts[:] = [t for t in attempts if now - t < _LOGIN_FAILURE_WINDOW_SECONDS]
    return len(attempts) >= _LOGIN_FAILURE_LIMIT

def _record_login_failure(key: str) -> None:
    _login_failures_by_username[key].append(time.monotonic())

@router.post("/auth/login")
@limiter.limit("10/minute")
async def proxy_login(request: Request):
    body = await request.body()
    username = None
    try:
        username = json.loads(body).get("username")
    except (ValueError, AttributeError):
        pass

    key = _normalize_username(username) if username else None
    if key and _is_locked_out(key):
        raise HTTPException(status_code=429, detail="Too many failed login attempts for this account. Try again later.")

    response = await forward_request("auth", "login", request)
    if key and response.status_code == status.HTTP_401_UNAUTHORIZED:
        _record_login_failure(key)
    return response

@router.api_route("/{service_name}", methods=["GET", "POST", "PUT", "PATCH", "DELETE"])
async def proxy_collection(service_name: str, request: Request):
    #handles collection endpoints: /tasks, /friends, /auth
    return await forward_request(service_name, "", request)

@router.api_route("/{service_name}/{path:path}", methods=["GET", "POST", "PUT", "PATCH", "DELETE"])
async def proxy(service_name: str, path: str, request: Request):
    # rate limiting is handled at the app level in main.py
    return await forward_request(service_name, path, request)
