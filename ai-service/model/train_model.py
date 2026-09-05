import json
import os
import joblib
import numpy as np
import pandas as pd
from datetime import datetime, timezone
from sklearn.ensemble import RandomForestClassifier
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import accuracy_score, classification_report, roc_auc_score
from sklearn.model_selection import StratifiedKFold, cross_val_predict, cross_val_score
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import StandardScaler
from dotenv import load_dotenv
from pymongo import MongoClient

load_dotenv()

MONGO_URI = os.getenv("MONGO_URI", "mongodb://localhost:27017/sayless")
DB_NAME = os.getenv("MONGO_DB", "sayless")
TASKS_COLL = os.getenv("MONGO_TASKS_COLLECTION", "tasks")
MODEL_PATH = os.getenv("MODEL_PATH", "model/task_model.pkl")

def _is_done(status: str) -> int:
    return 1 if status == "DONE" else 0

def _days_until(d):
    if d is None:
        return 0.0
    now = datetime.now(timezone.utc)
    if d.tzinfo is None:
        d = d.replace(tzinfo=timezone.utc)
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
    df["is_overdue"] = (df["days_until_deadline"] < 0).astype(float)
    df["days_overdue"] = (-df["days_until_deadline"]).clip(lower=0)
    df["label_done"] = df["status"].apply(_is_done)
    df["title_len"] = df["title"].apply(lambda s: len(s or ""))

    thirty_days_ago = datetime.now(timezone.utc).timestamp() - 30*86400
    df["created_ts"] = df["createdAt"].apply(lambda d: d.timestamp() if isinstance(d, datetime) else None)
    df["is_recent"] = df["created_ts"].notna() & (df["created_ts"] >= thirty_days_ago)

    by_user = df.groupby("assignedTo").agg(
        user_total_tasks_all=("status", "count"),
        user_done_all=("label_done", "sum"),
        user_title_len_sum=("title_len", "sum"),
        user_recent_all=("is_recent", "sum"),
    ).reset_index()

    features = df.merge(by_user, on="assignedTo", how="left")
    features[["user_total_tasks_all", "user_done_all", "user_title_len_sum", "user_recent_all"]] = \
        features[["user_total_tasks_all", "user_done_all", "user_title_len_sum", "user_recent_all"]].fillna(0.0)

    features["user_total_tasks"] = features["user_total_tasks_all"] - 1
    features["user_done"] = features["user_done_all"] - features["label_done"]
    features["user_completion_rate"] = (features["user_done"] + 1) / (features["user_total_tasks"] + 2)

    has_other_tasks = features["user_total_tasks"] > 0
    safe_denominator = features["user_total_tasks"].where(has_other_tasks, 1)
    title_len_sum_loo = features["user_title_len_sum"] - features["title_len"]
    features["user_avg_title_len"] = np.where(has_other_tasks, title_len_sum_loo / safe_denominator, 0.0)

    features["recent_activity_30d"] = features["user_recent_all"] - features["is_recent"].astype(int)

    features["assigned_flag"] = (features["createdBy"] != features["assignedTo"]).astype(float)

    #drop rows without status labels
    features = features.dropna(subset=["label_done"])

    return features[[
        "text_len", "days_until_deadline", "is_overdue", "days_overdue", "assigned_flag",
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

    skf = StratifiedKFold(n_splits=5, shuffle=True, random_state=42)
    fold_accuracies = cross_val_score(pipeline, X, y, cv=skf, scoring="accuracy")
    oof_proba = cross_val_predict(pipeline, X, y, cv=skf, method="predict_proba")[:, 1]
    oof_pred = (oof_proba >= 0.5).astype(int)

    cv_accuracy_mean = float(fold_accuracies.mean())
    cv_accuracy_std = float(fold_accuracies.std())
    cv_roc_auc = float(roc_auc_score(y, oof_proba))
    report = classification_report(y, oof_pred, output_dict=True)

    rf_comparison = RandomForestClassifier(n_estimators=300, random_state=42)
    rf_fold_accuracies = cross_val_score(rf_comparison, X, y, cv=skf, scoring="accuracy")
    rf_oof_proba = cross_val_predict(rf_comparison, X, y, cv=skf, method="predict_proba")[:, 1]
    rf_comparison_metrics = {
        "note": "measured for comparison only - the deployed model is the logistic regression pipeline above",
        "cv_accuracy_mean": float(rf_fold_accuracies.mean()),
        "cv_accuracy_std": float(rf_fold_accuracies.std()),
        "cv_roc_auc": float(roc_auc_score(y, rf_oof_proba)),
    }

    pipeline.fit(X, y)

    os.makedirs(os.path.dirname(MODEL_PATH), exist_ok=True)
    joblib.dump({
        "model": pipeline,
        "feature_names": list(X.columns)
    }, MODEL_PATH)

    metrics = {
        "trained_at": datetime.now(timezone.utc).isoformat(),
        "n_samples": len(df),
        "n_folds": skf.get_n_splits(),
        "cv_accuracy_mean": cv_accuracy_mean,
        "cv_accuracy_std": cv_accuracy_std,
        "cv_roc_auc": cv_roc_auc,
        "classification_report": report,
        "random_forest_comparison": rf_comparison_metrics,
    }
    metrics_path = os.path.join(os.path.dirname(MODEL_PATH), "metrics.json")
    with open(metrics_path, "w") as f:
        json.dump(metrics, f, indent=2)

    print(f"Model was trained and saved in {MODEL_PATH} with {len(df)} samples")
    print(f"5-fold CV accuracy: {cv_accuracy_mean:.3f} +/- {cv_accuracy_std:.3f}")
    print(f"5-fold CV ROC-AUC: {cv_roc_auc:.3f}")
    print(f"random forest comparison (not shipped): accuracy {rf_comparison_metrics['cv_accuracy_mean']:.3f} +/- {rf_comparison_metrics['cv_accuracy_std']:.3f}, ROC-AUC {rf_comparison_metrics['cv_roc_auc']:.3f}")
    print(f"Metrics written to {metrics_path}")

if __name__ == "__main__":
    train()
