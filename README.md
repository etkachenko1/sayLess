# SayLess

A collaborative task manager built as seven independently deployable services: task and friend management, JWT auth, a logistic-regression completion-likelihood model, and live board updates pushed over Kafka + WebSocket instead of polling.
 
Live at **[sayless.site](https://sayless.site)**. There's a demo account on the login page if you'd rather not register, and opening it in two tabs is the quickest way to see the board sync.

## Architecture

```mermaid
flowchart LR
    subgraph Client
        FE[React + TS frontend]
    end

    FE -- REST, JWT --> GW[gateway-service<br/>FastAPI]
    GW --> AUTH[auth-service]
    GW --> TASK[task-service]
    GW --> FRIEND[friend-service]
    GW --> AI[ai-service]
    GW --> NOTIF_REST[notification-service<br/>REST]

    TASK -- publishes --> KAFKA[(Kafka)]
    FRIEND -- publishes --> KAFKA
    KAFKA -- consumes --> NOTIF[notification-service<br/>Kafka listener]
    NOTIF -- STOMP over WebSocket --> FE

    AUTH --> DB[(MongoDB)]
    TASK --> DB
    FRIEND --> DB
    NOTIF --> DB
    AI --> DB
```

> **Note:** `gateway-service` is a plain HTTP reverse proxy and can't handle a WebSocket protocol upgrade, so the STOMP connection routes around it in every deployment. What differs is who does the routing. Docker Compose on its own has no proxy in front of the services, so the frontend is built knowing `notification-service`'s address directly (`VITE_WS_URL`, separate from `VITE_API_URL`). Minikube's Ingress and the production Caddy setup do it on the frontend's behalf, which is why those paths look like a single origin even though the same split is happening.

### Services

| Service | Stack | Port (compose) | Purpose |
|---|---|---|---|
| `frontend` | React + TypeScript + Vite | 5173 | Dashboard, auth pages, live board |
| `gateway-service` | FastAPI | 8080 | Routes REST traffic, validates JWTs, rate-limits (100 req/min/IP) |
| `auth-service` | Spring Boot 3.5.6 | 8081 | Registration/login, JWT issuance, profile |
| `task-service` | Spring Boot 3.5.6 | 8082 | Task CRUD, assignment, publishes task events to Kafka |
| `friend-service` | Spring Boot 3.5.6 | 8083 | Friend requests, publishes friend events to Kafka |
| `notification-service` | Spring Boot 4.1.0 | 8085 | Kafka consumer, persists notifications, pushes over STOMP |
| `ai-service` | FastAPI + scikit-learn | internal only | Task-completion likelihood prediction |

(`notification-service` runs a newer Spring Boot line than the other three. I know that's going to bite me eventually; I just haven't had a reason to align them yet, and I'd rather do it deliberately than as a drive-by while I'm in there for something else.)

## Real-time board sync

Task visibility is per-user (`findByCreatedByOrAssignedTo`), so updates go to specific users over `/user/queue/tasks`, not broadcast. Every mutation (create, reassign, update, complete, delete) publishes a full task snapshot to Kafka. `notification-service` forwards it to whoever's affected, including the previous assignee on a reassignment, so a reassigned task actually disappears from their board instead of lingering. Incoming events are merged by `taskId`, duplicates and stale events are dropped, and a reconnect triggers a full resync so a dropped socket doesn't leave the board stale.

### Measured performance

Numbers from local load/latency testing (scripts in [`loadtest/`](loadtest/)), not estimates:

- **~22,200 Kafka events/minute at p95 90 ms**: 15 concurrent virtual users hitting `task-service` directly for 60s (create → reassign → complete per iteration), zero failures, and confirmed the consumer side kept pace via Kafka consumer-group lag (0 lag after the run, not just that requests were accepted).
- **17 ms median / 31 ms p95 end-to-end WebSocket push latency**: time from `task-service` publishing an event to a second, independent STOMP client receiving the corresponding board update.

Both are local, single-broker, single-partition-per-topic numbers. Meaningful for this project's scale, not a claim about production infrastructure.

## AI: task-completion prediction

`ai-service` serves a logistic regression pipeline behind `/predict`, trained on engineered features (combined title+description length, days until deadline, overdue flag/magnitude, and leave-one-out per-user completion history: each row's own label is excluded from its own aggregate features, since including it was an earlier bug that inflated accuracy). Auto-trains on startup if no model file is present.

Current measured result (5-fold cross-validated, `ai-service/model/metrics.json`):

- **0.82 ROC-AUC**, **74% accuracy** on 256 records
- A random-forest comparison was measured too (~91% CV accuracy) and deliberately not shipped. The measurement itself is sound; cross-validation already controls for overfitting. The real reason is the training data: `populate_db.py`'s synthetic generator picks each task's deadline *after* it's already picked the task's status, and `DONE` tasks are allowed deadlines up to 120 days in the past while `TODO`/`IN_PROGRESS` tasks are almost never more than two weeks overdue. A tree splits on that immediately.
- The linear model learned the same thing, just less efficiently, and the coefficients show it: `is_overdue` at −1.00 against `days_overdue` at +1.74. Being late at all drops the score, but each additional day pushes it back up, so a task two months overdue scores *higher* than one that missed by a day. That's the generator's rule, not anything about how tasks actually get finished.

## Getting started

### Docker Compose (fastest path)

```bash
# from the repo root
cat <<EOF > .env
JWT_SECRET=$(openssl rand -hex 32)
MONGO_ROOT_USERNAME=sayless_admin
MONGO_ROOT_PASSWORD=$(openssl rand -hex 16)
KAFKA_USERNAME=sayless_kafka
KAFKA_PASSWORD=$(openssl rand -hex 16)
EOF
docker compose up --build
```

Mongo and Kafka both require these credentials to start, and `MONGO_URI` for every service is built from `MONGO_ROOT_USERNAME`/`MONGO_ROOT_PASSWORD` directly in `docker-compose.yml`, so there's nothing else to configure. If you're regenerating `.env` for a stack that already has a Mongo volume, Mongo only creates its root user from these values on a *fresh* data directory, so run `docker compose down -v` first or the new credentials won't match what's actually in the database.

Then seed the database. The model needs enough data to train on, and 5-fold cross-validation needs at least five examples of each class, so a handful of tasks isn't enough:

```bash
docker compose exec -e ALLOW_DB_SEEDING=true ai-service python populate_db.py
```

The script refuses to run without `ALLOW_DB_SEEDING=true`, it inserts real accounts with real (freshly-generated, printed once, never stored) passwords, so it's opt-in on purpose rather than something that could run against a real database by accident.

- Frontend: http://localhost:5173
- Gateway: http://localhost:8080

### Minikube

```bash
minikube start
MONGO_PW=$(openssl rand -hex 16)
kubectl create secret generic auth-secret \
  --from-literal=JWT_SECRET=$(openssl rand -hex 32) \
  --from-literal=MONGO_ROOT_USERNAME=sayless_admin \
  --from-literal=MONGO_ROOT_PASSWORD=$MONGO_PW \
  --from-literal=MONGO_URI=mongodb://sayless_admin:$MONGO_PW@mongo:27017/sayless?authSource=admin \
  --from-literal=KAFKA_USERNAME=sayless_kafka \
  --from-literal=KAFKA_PASSWORD=$(openssl rand -hex 16)
./redeploy.ps1   # builds every image and applies k8s/, then rolls out
minikube tunnel  # needs Administrator on Windows, since it's changing the routing table, not binding a low port
```

Mongo only creates its root user from `MONGO_ROOT_USERNAME`/`PASSWORD` on a fresh PVC. If you're turning this on for a cluster that already has data, `kubectl delete pvc mongo-pvc` and reseed rather than expecting the new credentials to retrofit an existing volume.

Add `127.0.0.1 sayless.local` to your hosts file, since the Ingress routes on that hostname, and the app won't resolve without it. Then visit http://sayless.local.

See [`k8s/secret.example.yaml`](k8s/secret.example.yaml) for the secret's shape if you'd rather write the manifest by hand.

### Production
 
The live instance runs the docker-compose stack on a single VPS behind Caddy, which terminates TLS and handles certificates. Caddy routes `/ws` straight to `notification-service` and everything else through the gateway, the same split the Minikube Ingress does. Only Caddy publishes ports; every other service is reachable on the internal Docker network alone.
 
`.env.production.example` documents every variable the deployed stack expects. `scripts/seed-demo.sh` sets up the public demo account, and takes a `--reset` flag that wipes and reseeds it, since its credentials are public and visitors can change its tasks. That runs nightly on the host.
 
The Kubernetes manifests are maintained and work against Minikube, but a managed cluster costs more per month than this project justifies and doesn't change anything a visitor would notice.
 
## Notable engineering decisions

**WebSocket client library, not the server, caused an intermittent connection failure.** This one surfaced only through the Kubernetes ingress, never locally, so I assumed it was an ingress problem first: checked the timeout annotations, adjusted them, watched it fail intermittently anyway. The actual cause was one layer down, in the browser: `sockjs-client` was deriving its connection timeout from the response time of a lightweight `/info` probe (~2 ms) instead of the real handshake, so under the extra latency the ingress added, the client gave up before the WebSocket upgrade could ever finish, even though the server was healthy the whole time. Fixed by dropping SockJS for a native WebSocket connection instead of continuing to tune timeouts around a symptom one layer removed from the cause.

**Docker Compose's WebSocket connection was silently misconfigured, and I only caught it while writing these setup instructions.** The frontend's WebSocket URL fell back to `VITE_API_URL` (the gateway) whenever `VITE_WS_URL` wasn't set, and it never was: not in the Dockerfile, not in `docker-compose.yml`, not in `frontend/.env`. The gateway can't handle a protocol upgrade at all, so live board sync and the notification bell were completely broken in Docker Compose specifically, while working fine through Minikube's Ingress. I'd already written the load-test scripts and measured real throughput and latency numbers against this exact stack without ever noticing, because those scripts talk to `notification-service` directly and never go near the gateway. Found it with a raw WebSocket handshake against the gateway (`404 {"detail":"Unknown service 'ws'"}`) and by grepping the actual served JS bundle, which only referenced the gateway's port. Fixed by wiring a separate `VITE_WS_URL` through the same build-arg path `VITE_API_URL` already used.

**`ai-service`'s auto-train path had never run until the first real deployment.** `main.py` only calls `train()` when no model file exists on disk, and every environment before this one already had a `model/task_model.pkl` baked into the image from an earlier manual run, so the check was always false. The first genuinely fresh environment hit that path immediately and it crashed. `train_model.py` reads `MONGO_TASKS_COLLECTION` and `MODEL_PATH` with no defaults, unlike `main.py`'s versions of the same two variables, and neither was set in `docker-compose.yml`, so `client[db][None]` threw before training could start. A stale artifact had been standing in for code that would have failed the moment anyone ran it.

## Security: an OWASP Top 10 pass

Ran a structured audit across every service. Found three exploitable issues, all fixed: a path traversal in the gateway that let requests skip the JWT check entirely, an IDOR on the AI service's `/predict` endpoint exposing another user's aggregates, and a public endpoint returning user emails to anyone with a guessable ID. Also closed two infrastructure gaps with nothing standing between an attacker and the data, since MongoDB and Kafka were both running unauthenticated.

One finding validated a decision made for an unrelated reason. The traversal bypass could only ever reach endpoints that were already unauthenticated at the service level, because `/auth/../tasks/...` still hits `task-service`'s own independent `JwtAuthFilter` and gets rejected there. Those duplicated filters, which I'd called out as "not DRY" above, are the reason this didn't go further.

Every finding, what was fixed, and what was deliberately deferred is in [`SECURITY_BACKLOG.md`](SECURITY_BACKLOG.md).

## Known gaps / deliberately deferred

- **Kubernetes isn't the deployed path.** Production is docker-compose behind Caddy on one VPS. The manifests are maintained and run against Minikube, but a managed cluster isn't a cost this project justifies.
- **Gateway doesn't forward identity.** Each downstream service re-validates the JWT independently rather than trusting a header set by the gateway, so there's duplication across `auth-service`/`task-service`/`friend-service`/`notification-service`'s near-identical JWT filter classes. Not DRY, but each service stays independent.
- **Test coverage is uneven.** `auth-service` has real regression tests for the auth/IDOR fixes; `task-service`, `friend-service`, and the frontend currently don't have any, and there's no CI pipeline yet.
- **No production-grade secrets management.** All services share one `JWT_SECRET` via `.env`/Kubernetes `Secret`: fine for local dev, not for anything with real users.