import os
from fastapi import APIRouter, Request, HTTPException, status, Response
import httpx
from dotenv import load_dotenv
from utils.jwt_utils import verify_jwt

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

#retrains and hot-swaps the live model - not something any authenticated user should be able to trigger
#remotely, so it's kept off the gateway's public route map entirely (direct/internal access only)
_BLOCKED_ROUTES = {("ai", "train")}

async def forward_request(service_name: str, path: str, request: Request):
    base_url = SERVICES.get(service_name)
    if not base_url:
        raise HTTPException(status_code=404, detail=f"Unknown service '{service_name}'")
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

@router.api_route("/{service_name}", methods=["GET", "POST", "PUT", "PATCH", "DELETE"])
async def proxy_collection(service_name: str, request: Request):
    #handles collection endpoints: /tasks, /friends, /auth
    return await forward_request(service_name, "", request)

@router.api_route("/{service_name}/{path:path}", methods=["GET", "POST", "PUT", "PATCH", "DELETE"])
async def proxy(service_name: str, path: str, request: Request):
    # rate limiting is handled at the app level in main.py
    return await forward_request(service_name, path, request)
