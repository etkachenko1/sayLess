import os

os.environ.setdefault("JWT_SECRET", "test-only-secret-not-used-anywhere-else")
os.environ.setdefault("AI_OPERATOR_SECRET", "test-only-operator-secret-not-used-anywhere-else")
os.environ.setdefault("MONGO_URI", "mongodb://localhost:27017/sayless")
os.environ.setdefault("MONGO_DB", "sayless")
os.environ.setdefault("MONGO_TASKS_COLLECTION", "tasks")
os.environ.setdefault("MODEL_PATH", "model/task_model.pkl")

from fastapi.testclient import TestClient
from jose import jwt

import main
from main import app

client = TestClient(app)


def _token(user_id="user-1"):
    return jwt.encode({"sub": user_id}, os.environ["JWT_SECRET"], algorithm="HS256")


def _auth_header(user_id="user-1"):
    return {"Authorization": f"Bearer {_token(user_id)}"}


def _predict_body(task_id="507f1f77bcf86cd799439011"):
    return {
        "task_id": task_id,
        "text": "write the report",
        "deadline": "2026-12-31T00:00:00Z",
    }


def test_train_without_operator_secret_is_rejected():
    res = client.post("/train")
    assert res.status_code == 403


def test_train_with_wrong_operator_secret_is_rejected():
    res = client.post("/train", headers={"X-Operator-Secret": "not-the-real-secret"})
    assert res.status_code == 403


def test_train_ignores_a_regular_user_jwt():
    # /train is operator-only - a normal user's valid JWT shouldn't grant access
    res = client.post("/train", headers=_auth_header("user-1"))
    assert res.status_code == 403


def test_predict_without_token_is_rejected():
    res = client.post("/predict", json=_predict_body())
    assert res.status_code == 401


def test_predict_for_a_task_the_caller_has_no_relation_to_is_rejected(monkeypatch):
    monkeypatch.setattr(main.coll, "find_one", lambda query: {
        "createdBy": "creator-id",
        "assignedTo": "assignee-id",
    })

    res = client.post("/predict", json=_predict_body(), headers=_auth_header("someone-else"))

    assert res.status_code == 403


def test_predict_for_a_task_the_caller_created_is_allowed_past_authorization(monkeypatch):
    monkeypatch.setattr(main.coll, "find_one", lambda query: {
        "createdBy": "creator-id",
        "assignedTo": "assignee-id",
    })

    res = client.post("/predict", json=_predict_body(), headers=_auth_header("creator-id"))

    # a caller who clears the authorization check still gets 503 rather than a real prediction: that's
    # the deterministic signal that they got past the 403/404 checks.
    assert res.status_code == 503


def test_predict_for_a_task_the_caller_is_assigned_to_is_allowed_past_authorization(monkeypatch):
    monkeypatch.setattr(main.coll, "find_one", lambda query: {
        "createdBy": "creator-id",
        "assignedTo": "assignee-id",
    })

    res = client.post("/predict", json=_predict_body(), headers=_auth_header("assignee-id"))

    assert res.status_code == 503


def test_predict_for_a_nonexistent_task_is_404(monkeypatch):
    monkeypatch.setattr(main.coll, "find_one", lambda query: None)

    res = client.post("/predict", json=_predict_body(), headers=_auth_header("user-1"))

    assert res.status_code == 404
