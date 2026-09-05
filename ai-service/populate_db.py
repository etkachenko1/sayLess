"""
AI population script
populate_db.py — Seed MongoDB with synthetic task data for AI model training.

Usage (requires ALLOW_DB_SEEDING=true, refuses to run otherwise, since this
inserts real accounts and is only meant for a local/dev database):
    ALLOW_DB_SEEDING=true python populate_db.py           # append data (skips existing usernames)
    ALLOW_DB_SEEDING=true python populate_db.py --clear   # drop users / tasks / friends first

Generates 250 tasks across 10 users with varied completion profiles so that
LogisticRegression has meaningful signal in every feature:
    text_len, days_until_deadline, assigned_flag,
    user_total_tasks, user_completion_rate, user_avg_title_len, recent_activity_30d
"""

import os
import secrets
import sys
import random
import bcrypt
from datetime import datetime, timezone, timedelta
from bson import ObjectId
from pymongo import MongoClient
from dotenv import load_dotenv

load_dotenv()

if os.getenv("ALLOW_DB_SEEDING") != "true":
    print(
        "Refusing to run: this script inserts real user accounts and is only meant "
        "for a local/dev database. Set ALLOW_DB_SEEDING=true to run it, e.g.:\n\n"
        "  docker compose exec -e ALLOW_DB_SEEDING=true ai-service python populate_db.py\n"
    )
    sys.exit(1)

MONGO_URI   = os.getenv("MONGO_URI", "mongodb://localhost:27017/sayless")
DB_NAME     = os.getenv("MONGO_DB", "sayless")
TASKS_COLL  = os.getenv("MONGO_TASKS_COLLECTION", "tasks")
CLEAR       = "--clear" in sys.argv

random.seed(42)

# ── helpers ──────────────────────────────────────────────────────────────────

def _now():
    return datetime.now(timezone.utc)

def _days_ago(n):
    return _now() - timedelta(days=n)

def _days_from_now(n):
    return _now() + timedelta(days=n)

# ── user profiles ─────────────────────────────────────────────────────────────
# Each profile controls: completion probability and how many tasks to generate.
# Varying these produces the user_completion_rate variance the model needs.

def _random_password() -> str:
    return secrets.token_urlsafe(12)

def _hash_password(password: str) -> str:
    return bcrypt.hashpw(password.encode("utf-8"), bcrypt.gensalt()).decode("utf-8")

USERS = [
    {"username": "alex_hunter",   "email": "alex@sayless.dev",    "bio": "Gets things done.",       "profile": "high"},
    {"username": "sam_reeves",    "email": "sam@sayless.dev",     "bio": "Consistent deliverer.",   "profile": "high"},
    {"username": "jordan_cole",   "email": "jordan@sayless.dev",  "bio": "Team player.",            "profile": "medium"},
    {"username": "taylor_banks",  "email": "taylor@sayless.dev",  "bio": "Steady worker.",          "profile": "medium"},
    {"username": "drew_nash",     "email": "drew@sayless.dev",    "bio": "Motivated.",              "profile": "medium"},
    {"username": "morgan_west",   "email": "morgan@sayless.dev",  "bio": "Variable focus.",         "profile": "low"},
    {"username": "casey_fox",     "email": "casey@sayless.dev",   "bio": "Procrastinator.",         "profile": "low"},
    {"username": "blake_cross",   "email": "blake@sayless.dev",   "bio": "Sporadic contributor.",   "profile": "low"},
    {"username": "riley_storm",   "email": "riley@sayless.dev",   "bio": "Just getting started.",   "profile": "new"},
    {"username": "avery_quinn",   "email": "avery@sayless.dev",   "bio": "Learning the ropes.",     "profile": "new"},
]

# Probability that a task is DONE (rest split between IN_PROGRESS and TODO)
DONE_PROB = {"high": 0.72, "medium": 0.50, "low": 0.22, "new": 0.35}
TASK_COUNT = {"high": 40, "medium": 28, "low": 22, "new": 10}

# ── task content pools ────────────────────────────────────────────────────────
# Mix of short, medium, and long titles gives variance in text_len.

TITLES = [
    # short (≈4–12 chars)
    "Fix bug", "Write test", "Review PR", "Update docs", "Deploy",
    "Check logs", "Send email", "Call client", "Pay bill", "Run report",
    # medium (≈20–50 chars)
    "Implement user authentication flow",
    "Refactor database connection pool",
    "Add pagination to task list endpoint",
    "Set up CI/CD pipeline for staging",
    "Write unit tests for friend service",
    "Design new onboarding screen mockup",
    "Migrate users collection to new schema",
    "Optimize slow MongoDB aggregation query",
    "Add input validation to registration form",
    "Create Kafka topic for task updates",
    "Integrate WebSocket notifications in frontend",
    "Prepare weekly progress report for manager",
    "Research Redis caching strategies",
    "Complete gym workout session",
    "Schedule dentist appointment",
    "Back up all production database snapshots",
    # long (≈60–120 chars)
    "Investigate and resolve production memory leak in notification service causing OOM crashes",
    "Build and deploy complete Docker Compose stack for local development environment testing",
    "Design and implement end-to-end encryption for task sharing between users in the platform",
    "Conduct full security audit of JWT token validation logic across all microservices",
    "Set up Kubernetes cluster on GKE with autoscaling policies and per-service resource limits",
    "Write comprehensive API documentation covering all public REST endpoints with curl examples",
    "Implement real-time activity feed using WebSocket STOMP and SockJS integration in React",
    "Create automated backup and restore procedures for all MongoDB collections with alerts",
]

DESCRIPTIONS = [
    "High priority — needs to be done ASAP.",
    "Assigned during sprint planning. Estimated 2 hours.",
    "Part of the Q3 roadmap initiative.",
    "Blocking other team members — handle first.",
    "Low-effort but important for compliance.",
    "Should pair with another teammate on this one.",
    "Technical debt item from last quarter.",
    "Customer-facing feature — extra care needed.",
    "Internal tooling improvement.",
    "Follow-up from last week's meeting.",
    "Straightforward — no blockers expected.",
    "Requires access to production database.",
    "Needs design review before implementation starts.",
    "Quick win — under an hour.",
    "Complex — may need to break into subtasks.",
    "",  # some tasks intentionally have no description
    "",
    "",
]

# ── task generation ───────────────────────────────────────────────────────────

def _random_deadline(status: str) -> datetime:
    if status == "DONE":
        # completed tasks: deadline was in the recent past or near future
        return random.choice([
            _days_ago(random.randint(1, 120)),
            _days_from_now(random.randint(1, 30)),
        ])
    if status == "IN_PROGRESS":
        return random.choice([
            _days_ago(random.randint(1, 14)),   # slightly overdue
            _days_from_now(random.randint(1, 21)),
        ])
    # TODO — mostly future, occasionally overdue
    weights = [0.7, 0.2, 0.1]
    choice = random.choices(["future_far", "future_near", "overdue"], weights)[0]
    if choice == "future_far":
        return _days_from_now(random.randint(14, 120))
    if choice == "future_near":
        return _days_from_now(random.randint(1, 13))
    return _days_ago(random.randint(1, 10))


def _random_created_at(deadline: datetime) -> datetime:
    delta = random.randint(1, 30)
    candidate = deadline - timedelta(days=delta)
    earliest = _days_ago(200)
    return max(candidate, earliest)


def make_tasks_for_user(user_id: str, all_user_ids: list, profile: str) -> list:
    count = TASK_COUNT[profile]
    done_p = DONE_PROB[profile]
    tasks = []

    for _ in range(count):
        r = random.random()
        if r < done_p:
            status = "DONE"
        elif r < done_p + 0.22:
            status = "IN_PROGRESS"
        else:
            status = "TODO"

        # ~30 % of tasks are assigned BY someone else (assigned_flag = 1.0)
        if random.random() < 0.30:
            creator = random.choice([u for u in all_user_ids if u != user_id])
        else:
            creator = user_id

        deadline   = _random_deadline(status)
        created_at = _random_created_at(deadline)
        updated_at = created_at + timedelta(hours=random.randint(1, 72))

        tasks.append({
            "_id":         ObjectId(),
            "title":       random.choice(TITLES),
            "description": random.choice(DESCRIPTIONS),
            "status":      status,
            "assignedTo":  user_id,
            "createdBy":   creator,
            "deadline":    deadline,
            "createdAt":   created_at,
            "updatedAt":   updated_at,
        })
    return tasks

# ── friend pairs ──────────────────────────────────────────────────────────────

def make_friends(user_ids: list) -> list:
    seen, records = set(), []
    for uid in user_ids:
        others = [u for u in user_ids if u != uid]
        for fid in random.sample(others, min(3, len(others))):
            pair = tuple(sorted([uid, fid]))
            if pair in seen:
                continue
            seen.add(pair)
            records.append({
                "_id":         ObjectId(),
                "requesterId": uid,
                "receiverId":  fid,
                "status":      "ACCEPTED",
                "createdAt":   _days_ago(random.randint(5, 90)),
            })
    return records

# ── main ──────────────────────────────────────────────────────────────────────

def populate():
    client = MongoClient(MONGO_URI)
    db = client[DB_NAME]

    if CLEAR:
        db["users"].drop()
        db[TASKS_COLL].drop()
        db["friends"].drop()
        print("Cleared: users, tasks, friends")

    # ── users ──
    user_ids: list[str] = []
    new_users = 0
    generated_credentials: list[tuple[str, str]] = []
    for u in USERS:
        existing = db["users"].find_one({"username": u["username"]})
        if existing:
            user_ids.append(str(existing["_id"]))
            continue
        oid = ObjectId()
        password = _random_password()
        db["users"].insert_one({
            "_id":        oid,
            "username":   u["username"],
            "email":      u["email"],
            "password":   _hash_password(password),
            "bio":        u["bio"],
            "profilePic": None,
        })
        user_ids.append(str(oid))
        generated_credentials.append((u["username"], password))
        new_users += 1

    print(f"Users ready : {len(user_ids)} total  ({new_users} newly inserted)")
    if generated_credentials:
        print("Generated passwords for newly inserted users (shown once, not stored anywhere):")
        for username, password in generated_credentials:
            print(f"  {username}: {password}")

    # map ObjectId string → profile
    uid_to_profile: dict[str, str] = {}
    for rec in db["users"].find({"username": {"$in": [u["username"] for u in USERS]}}):
        name = rec["username"]
        match = next((u for u in USERS if u["username"] == name), None)
        if match:
            uid_to_profile[str(rec["_id"])] = match["profile"]

    # ── tasks ──
    all_tasks: list[dict] = []
    for uid, profile in uid_to_profile.items():
        all_tasks.extend(make_tasks_for_user(uid, list(uid_to_profile.keys()), profile))

    if all_tasks:
        db[TASKS_COLL].insert_many(all_tasks)

    done_n   = sum(1 for t in all_tasks if t["status"] == "DONE")
    inprog_n = sum(1 for t in all_tasks if t["status"] == "IN_PROGRESS")
    todo_n   = sum(1 for t in all_tasks if t["status"] == "TODO")
    print(f"Tasks inserted : {len(all_tasks)}  "
          f"(DONE={done_n}, IN_PROGRESS={inprog_n}, TODO={todo_n})")

    # ── friends ──
    existing_friends = db["friends"].count_documents({})
    if existing_friends == 0:
        friends = make_friends(user_ids)
        if friends:
            db["friends"].insert_many(friends)
        print(f"Friend pairs inserted : {len(friends)}")
    else:
        print(f"Friends already exist : {existing_friends} records — skipping")

    # ── summary ──
    total_db   = db[TASKS_COLL].count_documents({})
    done_db    = db[TASKS_COLL].count_documents({"status": "DONE"})
    nondone_db = total_db - done_db
    print()
    print("── DB state after population ──────────────────────")
    print(f"  tasks total  : {total_db}")
    print(f"  DONE         : {done_db}  ({done_db/total_db*100:.1f} %)" if total_db else "  (no tasks)")
    print(f"  non-DONE     : {nondone_db}")
    print(f"  users        : {db['users'].count_documents({})}")
    print(f"  friends      : {db['friends'].count_documents({})}")
    print()
    print("Next step:  python model/train_model.py")

if __name__ == "__main__":
    populate()
