from fastapi import FastAPI
from routes.proxy import router as proxy_router

app = FastAPI()

@app.get("/")
def home():
    return {"message": "Gateway running on port 8080"}

app.include_router(proxy_router)
