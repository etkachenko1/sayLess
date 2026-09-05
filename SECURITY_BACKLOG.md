# Security backlog

An OWASP Top 10 pass across every service in this repo. Findings that were exploitable got fixed
immediately; the rest is tracked here rather than left as an unwritten TODO. 

This file is the complete record, including what was deliberately left alone and why.

## Fixed

**Gateway path traversal bypassed the JWT check.** `/auth/**` is exempt from the gateway's own JWT
verification, since login and register have to be public. But the path was concatenated into the
outbound URL unsanitized, and the HTTP client normalizes `..` segments, so `GET /auth/../users/123`
arrived at auth-service as `GET /users/123` with no token. The same trick defeated the `/ai/train`
denylist.

The first fix decoded once, which caught `..` and `%2e%2e` but not `%25252e%25252e`. That one reached the backend as a mangled string and was only rejected because Spring Security's HTTP firewall caught it, which is a downstream framework covering for an upstream check. `_is_path_safe` now decodes to a fixed point and rejects anything with a residual `%`.

**IDOR on `/predict`.** The target user ID came from the request body rather than the JWT, so any
authenticated user could pull another user's completion aggregates. Forcing the target to always be
the caller would have broken a real case: a task's creator checking the prediction for something
they'd assigned to someone else. The request now carries a `task_id`, and the service verifies the
caller is the task's creator or assignee before deciding whose data to aggregate.

**`GET /users/{id}` leaked emails.** The endpoint is deliberately public, since task-service and
friend-service call it without a token for username lookups. It returned the full user object,
email included, to anyone with a guessable ID. Now it returns the full profile only when the caller
is authenticated as that user, and the same minimal `id`+`username` shape `/search` already used to
everyone else.

**`/ai/train` had no authorization.** The gateway denylist was never the fix. It had a trailing-slash bypass, and more to the point ai-service was reachable on the host at `0.0.0.0:8084` with `/train` requiring nothing but any valid JWT. A throwaway account could trigger a real retrain. The host port mapping is gone, and `/train` now checks an `X-Operator-Secret` header against an env-sourced secret with `hmac.compare_digest`. This app has no role system, and building one for a single operator endpoint is scope the app doesn't otherwise need, so the shared secret is an honest stand-in rather than a pattern to reuse anywhere user-facing.

**MongoDB and Kafka both ran unauthenticated.** Anything that could reach the Mongo service had full
read/write on every collection, and anything that could reach the broker could produce events that
notification-service would act on. Both now require credentials, from the same `Secret`/`.env` pattern already used for `JWT_SECRET`.

**Seed script created accounts with a known password.** `populate_db.py` inserted ten users with a
hardcoded bcrypt hash for a password documented in the source. It now refuses to run without
`ALLOW_DB_SEEDING=true` and generates a random password per user, printed once and never stored.

**Mongo and Kafka were bound to `0.0.0.0` in compose**, and ai-service's port was published too. All
three are internal-only now.

**Registration had a double-submit race.** The Sign Up button had no in-flight guard, so a double-click fired two concurrent `register()` calls. The bcrypt write on the first could resolve after the duplicate rejection on the second, letting the success state overwrite an error that had already landed. The backend's duplicate check races safely; the frontend just had nothing stopping the race from starting.

**Smaller items closed in the same pass:** `react-router-dom` bumped past its high-severity CVEs,
`kafka:latest` pinned, Kafka's `JsonDeserializer` narrowed from `*` to the actual event package,
`frame-ancestors` added to the CSP, status enum validated so a bad value returns 400 rather than 500,`UriComponentsBuilder` replacing string concatenation in both `UserClient` classes, every  container switched to a non-root user with matching `securityContext` in the k8s manifests.

**Login lockout re-keyed.** It was keyed on username alone, so anyone who knew a username could lock
it out from anywhere, and the tracking dict was only pruned when a key was re-checked. Now keyed on
`(username, IP)` in a bounded TTL cache, with `X-Forwarded-For` trusted only when the connection comes from a configured proxy address. Unset by default, so local development works unchanged and an
unconfigured deployment fails closed.

This is a mitigation. Pair-keying still allows locking out a victim if the attacker
rotates IPs. Exponential backoff or a CAPTCHA would be the answer with real users involved.

**`check-username` endpoint removed, and user enumeration is closed.** Nothing called it anymore, so
deleting it closes the fast, quiet enumeration path entirely. Registration still returns distinct
"username taken" and "email taken" messages, which is a deliberate keep: the specific messages are
better for legitimate users, and enumerating through registration is slow and logged in a way the
dedicated endpoint wasn't.

## Deferred

**JWT in `localStorage`.** Readable by any JS on the page. The textbook fix is an `HttpOnly` cookie,
but that breaks the STOMP `CONNECT` header auth this app uses and introduces CSRF surface that would
need its own handling. Documenting the tradeoff rather than doing a half-migration.

**JWTs can't be revoked.** Stateless 24h tokens, no logout or blacklist endpoint. A leaked token stays valid for its full lifetime. Fixing it properly means a revocation store, which is real scope.

**No TLS on the k8s Ingress.** Moot for the deployed instance, which runs compose behind a reverse
proxy that terminates TLS. Still a real gap if the Minikube path is ever used for anything reachable.

**No rate limit on `/predict`.** Access is scoped to tasks the caller can see, so the remaining
concern is someone hammering it across all their tasks to scrape aggregates faster than intended.

**Verbose errors from ai-service.** `/predict` and `/train` return `str(e)` on a 500, echoing raw
sklearn and pymongo messages back to the caller. Should log server-side and return something generic.

**JJWT pinned to 0.11.5** across the four Java services, using the deprecated
`parserBuilder()`/`parseClaimsJws()` API. Upgrade to 0.12.x means migrating to
`Jwts.parser().verifyWith(key).build().parseSignedClaims(token)`.

**Auth failures logged with `System.out.println`.** The same pattern in auth, task, and friend-service, and as plain absence of logging in gateway-service, ai-service, and notification-service's STOMP interceptor. Worth fixing once across all of them rather than service by service, and worth adding a log line on every 403 branch in `TaskController` and `FriendController` so authorization probing is visible too.

**Task-existence oracle on `/predict`.** Returns 404 for a task that doesn't exist and 403 for one that does but isn't yours, so a caller can tell the difference. The textbook fix is 404 for both, which makes debugging worse for not much payoff.

**Unescaped `sed` substitution** of `VITE_API_URL`/`VITE_WS_URL` into the CSP in the frontend
Dockerfile. Build args rather than runtime input, so not attacker-controlled today.

## No action needed

The ones that were already correct: ownership checks in task, friend, and
notification-service CRUD paths; STOMP handshake authentication and per-user destinations that can't
be cross-subscribed; JWT secrets env-sourced and never committed; all Mongo access through
parameterized Spring Data methods; no `dangerouslySetInnerHTML`, `eval`, or other XSS sink in the
frontend.

## A note from the fixes

Unmapped paths under `/auth/**` now return 403 rather than 404. Spring Security's `permitAll` lets the request through, but the resulting "no handler found" is forwarded internally to `/error`, which isn't permitAll and gets denied. Pre-existing, not something these changes caused.