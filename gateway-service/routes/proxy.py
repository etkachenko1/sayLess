from fastapi import APIRouter, Request, HTTPException, status, Response
import httpx
from utils.jwt_utils import verify_jwt

router = APIRouter()

SERVICES = {
    "auth": "http://localhost:8081",
    "tasks": "http://localhost:8082",
    "friends": "http://localhost:8083",
    "ai": "http://localhost:8084"
}

async def forward_request(service_name: str, path: str, request: Request):
    base_url = SERVICES.get(service_name)
    if not base_url:
        raise HTTPException(status_code = 404, detail = f"Unknown service '{service_name}'")
    
    #validate jwt unless its auth
    if service_name != "auth":
        auth_header = request.headers.get("authorization")
        if not auth_header or not auth_header.startswith("Bearer "):
            raise HTTPException(status_code = status.HTTP_401_UNAUTHORIZED, detail = "Missing token")
        token = auth_header.split(" ")[1]
        verify_jwt(token)

    async with httpx.AsyncClient() as client:
        res = await client.request(
            request.method,
            f"{base_url}/{path}",
            headers = dict(request.headers),
            content= await request.body()
        )
        return Response(
            content = res.content,
            status_code = res.status_code,
            headers = dict(res.headers)
        )

@router.api_route("/{service_name}/{path:path}", methods=["GET", "POST", "PUT", "PATCH", "DELETE"])
async def proxy(service_name: str, path: str, request: Request):
    #rate limiting is handled at the app level in main.py
    return await forward_request(service_name, path, request)