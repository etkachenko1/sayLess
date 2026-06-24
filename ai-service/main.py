import os
import joblib
import numpy as np
from datetime import datetime, timezone, timedelta
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from dotenv import load_dotenv
from pymongo import MongoClient
from schemas import PredictRequest, PredictResponse

load_dotenv()

MONGO_URI  = os.getenv("MONGO_URI", "mongodb://localhost:27017/sayless")
DB_NAME    = os.getenv("MONGO_DB", "sayless")
TASKS_COLL = os.getenv("MONGO_TASKS_COLLECTION", "tasks")
MODEL_PATH = os.getenv("MODEL_PATH", "model/task_model.pkl")

app = FastAPI(title="SayLess AI Service", version="1.0")

#CORS configuration
app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:5173", "http://localhost:3000"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

model = None
feature_names = []

if os.path.exists(MODEL_PATH):
    bundle = joblib.load(MODEL_PATH)
    model = bundle["model"]
    feature_names = bundle["feature_names"]
else:
    print(f"model not found at {MODEL_PATH}. /predict will return error until trained")

#mongo client
mongo = MongoClient(MONGO_URI)
coll = mongo[DB_NAME][TASKS_COLL]

def _user_aggregates(user_id: str):
    #calculates user stats on every API request for now
    tasks = list(coll.find({"assignedTo": user_id}, {
        "title": 1, "status": 1, "createdAt": 1
    }))

    total = len(tasks)
    done = sum(1 for t in tasks if t.get("status") == "DONE")
    avg_title_len = 0.0
    if total > 0:
        avg_title_len = float(np.mean([len((t.get("title") or "")) for t in tasks]))
    completion_rate = (done + 1) / (total + 2)  # same smoothing

    # recent activity last 30 days
    cutoff = datetime.now(timezone.utc) - timedelta(days=30)
    recent_30d = 0
    for t in tasks:
        cat = t.get("createdAt")

        if isinstance(cat, datetime):
            if cat.tzinfo is None:
                cat = cat.replace(tzinfo=timezone.utc)

            if cat >= cutoff:
                recent_30d += 1

    return {
        "user_total_tasks": float(total),
        "user_completion_rate": float(completion_rate),
        "user_avg_title_len": float(avg_title_len),
        "recent_activity_30d": float(recent_30d),
    }

def _days_until(deadline: datetime) -> float:
    now = datetime.now(timezone.utc)
    if deadline.tzinfo is None:
        deadline = deadline.replace(tzinfo=timezone.utc)
    return (deadline - now).total_seconds() / 86400.0

@app.get("/")
def root():
    return {"message": "AI service is working"}

@app.post("/predict", response_model=PredictResponse)
def predict(body: PredictRequest):
    if model is None:
        raise HTTPException(status_code=503, detail="model_unavailable")
    
    agg = _user_aggregates(body.user_id)

    #if user has no history, dafault to 0.50 probability
    if agg["user_total_tasks"] == 0:
        return PredictResponse(likelihood=0.5)

    #gather all numberic inputs for logistic regression
    features = {
        "text_len": float(len(body.text or "")),
        "days_until_deadline": float(_days_until(body.deadline)),
        "assigned_flag": float(1.0 if body.assigned_by != body.user_id else 0.0),
        "user_total_tasks": agg["user_total_tasks"],
        "user_completion_rate": agg["user_completion_rate"],
        "user_avg_title_len": agg["user_avg_title_len"],
        "recent_activity_30d": agg["recent_activity_30d"],
    }

    #keep order consistent with training
    X = np.array([[features[name] for name in feature_names]], dtype=float)
    try:
        prob = model.predict_proba(X)[0, 1]
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

    return PredictResponse(likelihood=float(round(prob, 2)))

@app.post("/train")
def train_now():
    #retrain model from live DB without restarting the service
    from model.train_model import train  # lazy import
    try:
        train()
        #reload after training
        global model, feature_names
        bundle = joblib.load(MODEL_PATH)
        model = bundle["model"]
        feature_names = bundle["feature_names"]
        return {"status": "ok", "message": "Retrained and reloaded"}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
