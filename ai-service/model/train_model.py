import os
import joblib
import numpy as np
import pandas as pd
from datetime import datetime, timezone
from sklearn.linear_model import LogisticRegression
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import StandardScaler
from dotenv import load_dotenv
from pymongo import MongoClient

load_dotenv()

MONGO_URI = os.getenv("MONGO_URI")
DB_NAME = os.getenv("MONGO_DB")
TASKS_COLL = os.getenv("MONGO_TASKS_COLLECTION")
MODEL_PATH = os.getenv("MODEL_PATH")

def _is_done(status: str) -> int:
    return 1 if status == "DONE" else 0

def _days_until(d):
    if d is None:
        return 0.0
    now = datetime.now(timezone.utc)
    diff = (d - now).total_seconds() / 86400.0
    return float(diff)

def _safe_len(s):
    return float(len(s or ""))

def _to_datetime(i):
    return i if isinstance(i, datetime) else None

def build_training_frame(tasks: list[dict]) -> pd.DataFrame:
    #extract fields for each task
    df = pd.DataFrame([{
        "title": t.get("title"),
        "description": t.get("description"),
        "status": t.get("status"),
        "deadline": _to_datetime(t.get("deadline")),
        "createdBy": t.get("createdBy"),
        "assignedTo": t.get("assignedTo"),
        "createdAt": _to_datetime(t.get("createdAt")),
        "updatedAt": _to_datetime(t.get("updatedAt")),
    } for t in tasks])

    if df.empty:
        return df

    #prepare data
    df["text"] = (df["title"].fillna("") + " " + df["description"].fillna("")).astype(str)
    df["text_len"] = df["text"].apply(_safe_len)
    df["days_until_deadline"] = df["deadline"].apply(_days_until)
    df["label_done"] = df["status"].apply(_is_done)


    #user aggregates
    by_user = df.groupby("assignedTo").agg(
        user_total_tasks=("status", "count"),
        user_done=("label_done", "sum"),
        user_avg_title_len=("title", lambda s: np.mean([len(x or "") for x in s])),
    ).reset_index()
    by_user["user_completion_rate"] = (by_user["user_done"] + 1) / (by_user["user_total_tasks"] + 2)  # Laplace smoothing

    #recent 30d activity, the most active are more likely to complete task
    thirty_days_ago = datetime.now(timezone.utc).timestamp() - 30*86400
    df["created_ts"] = df["createdAt"].apply(lambda d: d.timestamp() if isinstance(d, datetime) else None)
    recent = df[df["created_ts"].notna() & (df["created_ts"] >= thirty_days_ago)] \
        .groupby("createdBy").size().reset_index(name="recent_activity_30d")
    #combine
    features = df.merge(by_user, on="assignedTo", how="left") \
                 .merge(recent, on="assignedTo", how="left")

    #fill empty vals from users with 0 history
    features[["user_total_tasks","user_done","user_avg_title_len","user_completion_rate","recent_activity_30d"]] = \
        features[["user_total_tasks","user_done","user_avg_title_len","user_completion_rate","recent_activity_30d"]].fillna(0.0)

    #if you assign a task to yourself is 0.0, to someone else is 1.0
    features["assigned_flag"] = (features["createdBy"] != features["assignedTo"]).astype(float)

    #drop rows without status labels
    features = features.dropna(subset=["label_done"])

    return features[[
        "text_len", "days_until_deadline", "assigned_flag",
        "user_total_tasks", "user_completion_rate", "user_avg_title_len",
        "recent_activity_30d", "label_done", "assignedTo"
    ]]

def train():
    client = MongoClient(MONGO_URI)
    coll = client[DB_NAME][TASKS_COLL]

    tasks = list(coll.find({}, {
        "title": 1, "description": 1, "status": 1, "deadline": 1,
        "createdBy": 1, "assignedTo": 1, "createdAt": 1, "updatedAt": 1
    }))

    #make the training table
    df = build_training_frame(tasks)
    #validate that there are both done and not done examples
    if df.empty or df["label_done"].sum() == 0 or df["label_done"].sum() == len(df):
        raise RuntimeError("Not enough labeled variety to train")

    X = df.drop(columns=["label_done", "assignedTo"])
    y = df["label_done"].astype(int)

    pipeline = Pipeline(steps=[
        ("scale", StandardScaler()), #normalizes numeric values
        ("clf", LogisticRegression(max_iter=200))
    ])
    pipeline.fit(X, y)

    os.makedirs(os.path.dirname(MODEL_PATH), exist_ok=True)
    joblib.dump({
        "model": pipeline,
        "feature_names": list(X.columns)
    }, MODEL_PATH)

    print(f"Model was trained and saved in {MODEL_PATH} with {len(df)} samples")

if __name__ == "__main__":
    train()
