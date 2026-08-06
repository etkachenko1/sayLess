import os

os.environ.setdefault("JWT_SECRET", "test-only-secret-not-used-anywhere-else")
os.environ.setdefault("MONGO_URI", "mongodb://localhost:27017/sayless")
os.environ.setdefault("MONGO_DB", "sayless")
os.environ.setdefault("MONGO_TASKS_COLLECTION", "tasks")
os.environ.setdefault("MODEL_PATH", "model/task_model.pkl")

from fastapi.testclient import TestClient
from jose import jwt

from main import app

client = TestClient(app)


def _token(user_id="user-1"):
    return jwt.encode({"sub": user_id}, os.environ["JWT_SECRET"], algorithm="HS256")


def test_train_without_token_is_rejected():
    res = client.post("/train")
    assert res.status_code == 401


def test_train_with_invalid_token_is_rejected():
    res = client.post("/train", headers={"Authorization": "Bearer not-a-real-token"})
    assert res.status_code == 401


def test_predict_without_token_is_rejected():
    body = {
        "user_id": "user-1",
        "text": "write the report",
        "deadline": "2026-12-31T00:00:00Z",
        "assigned_by": "user-2",
    }
    res = client.post("/predict", json=body)
    assert res.status_code == 401
