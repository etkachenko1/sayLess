from datetime import datetime, timedelta, timezone

from model.train_model import build_training_frame


def _task(title, status, assigned_to, created_by="creator", days_ago=1, deadline=None):
    return {
        "title": title,
        "description": "",
        "status": status,
        "deadline": deadline,
        "createdBy": created_by,
        "assignedTo": assigned_to,
        "createdAt": datetime.now(timezone.utc) - timedelta(days=days_ago),
        "updatedAt": datetime.now(timezone.utc) - timedelta(days=days_ago),
    }


def test_completion_rate_excludes_the_row_own_label():
    tasks = [
        _task("aaa", "DONE", "u1"),
        _task("bb", "TODO", "u1"),
        _task("cccc", "DONE", "u1"),
    ]
    df = build_training_frame(tasks).reset_index(drop=True)

    assert (df["user_total_tasks"] == 2).all()

    row_a = df.iloc[0]
    row_b = df.iloc[1]
    row_c = df.iloc[2]

    assert row_a["user_completion_rate"] == 0.5
    assert row_b["user_completion_rate"] == 0.75
    assert row_c["user_completion_rate"] == 0.5


def test_completion_rate_would_differ_if_leakage_were_present():
    tasks = [
        _task("aaa", "DONE", "u1"),
        _task("bb", "TODO", "u1"),
        _task("cccc", "DONE", "u1"),
    ]
    df = build_training_frame(tasks).reset_index(drop=True)

    leaky_rate_for_row_a = (2 + 1) / (3 + 2)  # what the old formula gave when row A counted its own DONE
    assert df.iloc[0]["user_completion_rate"] != leaky_rate_for_row_a


def test_avg_title_len_excludes_the_row_own_title():
    tasks = [
        _task("aaa", "DONE", "u1"),
        _task("bb", "TODO", "u1"),
        _task("cccc", "DONE", "u1"),
    ]
    df = build_training_frame(tasks).reset_index(drop=True)

    assert df.iloc[0]["user_avg_title_len"] == 3.0
    assert df.iloc[1]["user_avg_title_len"] == 3.5
    assert df.iloc[2]["user_avg_title_len"] == 2.5


def test_single_task_user_falls_back_to_neutral_prior():
    tasks = [_task("single", "DONE", "u2")]
    df = build_training_frame(tasks).reset_index(drop=True)

    assert df.iloc[0]["user_total_tasks"] == 0
    assert df.iloc[0]["user_completion_rate"] == 0.5
    assert df.iloc[0]["user_avg_title_len"] == 0.0


def test_is_overdue_and_days_overdue():
    now = datetime.now(timezone.utc)
    tasks = [
        _task("past due", "TODO", "u4", deadline=now - timedelta(days=5)),
        _task("not due yet", "TODO", "u4", deadline=now + timedelta(days=5)),
    ]
    df = build_training_frame(tasks).reset_index(drop=True)

    overdue_row = df[df["is_overdue"] == 1.0].iloc[0]
    future_row = df[df["is_overdue"] == 0.0].iloc[0]

    assert overdue_row["days_overdue"] > 4.9
    assert future_row["days_overdue"] == 0.0


def test_recent_activity_excludes_the_row_itself():
    tasks = [
        _task("recent1", "DONE", "u3", days_ago=1),
        _task("recent2", "TODO", "u3", days_ago=2),
        _task("old", "DONE", "u3", days_ago=40),
    ]
    df = build_training_frame(tasks).reset_index(drop=True)

    assert df.iloc[0]["recent_activity_30d"] == 1
    assert df.iloc[1]["recent_activity_30d"] == 1
    assert df.iloc[2]["recent_activity_30d"] == 2
