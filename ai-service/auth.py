import hmac
import os
from jose import jwt, JWTError
from fastapi import Header, HTTPException, status
from dotenv import load_dotenv

load_dotenv()

SECRET_KEY = os.getenv("JWT_SECRET")
if not SECRET_KEY:
    raise RuntimeError("JWT_SECRET is missing, please define it in .env")
ALGORITHM = "HS256"

OPERATOR_SECRET = os.getenv("AI_OPERATOR_SECRET")


def require_jwt(authorization: str = Header(default=None)):
    if not authorization or not authorization.startswith("Bearer "):
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Missing bearer token")
    token = authorization[len("Bearer "):]
    try:
        return jwt.decode(token, SECRET_KEY, algorithms=[ALGORITHM])
    except JWTError:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Invalid or expired token")


def require_operator_secret(x_operator_secret: str = Header(default=None)):
    #operator-only endpoints, there's no user role system in
    #this app, so this is a shared secret rather than a per-user permission check
    if not OPERATOR_SECRET:
        raise HTTPException(status_code=status.HTTP_503_SERVICE_UNAVAILABLE, detail="Operator endpoint not configured")
    if not x_operator_secret or not hmac.compare_digest(x_operator_secret, OPERATOR_SECRET):
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Not authorized")
