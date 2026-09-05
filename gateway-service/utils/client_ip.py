import os
from fastapi import Request

TRUSTED_PROXY_IP = os.getenv("TRUSTED_PROXY_IP")

def client_ip(request: Request) -> str:
    direct_ip = request.client.host if request.client else "unknown"
    if TRUSTED_PROXY_IP and direct_ip == TRUSTED_PROXY_IP:
        forwarded = request.headers.get("x-forwarded-for")
        if forwarded:
            return forwarded.split(",")[0].strip()
    return direct_ip
