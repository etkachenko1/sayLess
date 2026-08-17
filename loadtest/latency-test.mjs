// Measures end-to-end WebSocket push latency: the time between task-service
// stamping updatedAt (the instant it publishes to Kafka) and a real STOMP
// client receiving the corresponding /user/queue/tasks push. Uses the same
// @stomp/stompjs client the frontend uses, with the `ws` package standing in
// for the browser's WebSocket. Everything runs on one machine, so there's no
// clock skew to account for.
//
// Usage: cd loadtest && npm install && npm run latency

import { Client } from "@stomp/stompjs";
import WebSocket from "ws";

const AUTH_BASE = process.env.AUTH_BASE || "http://localhost:8081";
const TASK_BASE = process.env.TASK_BASE || "http://localhost:8082";
const WS_URL = process.env.WS_URL || "ws://localhost:8085/ws";
const ITERATIONS = Number(process.env.ITERATIONS || 60);
const PER_EVENT_TIMEOUT_MS = 5000;
const PAUSE_BETWEEN_MS = 150;

const RUN_ID = `${Date.now()}`;

function decodeUserId(token) {
  const payload = token.split(".")[1];
  const json = Buffer.from(payload, "base64url").toString("utf8");
  return JSON.parse(json).sub;
}

async function registerAndLogin(username) {
  const email = `${username}@sayless.local`;
  const password = "LoadTest123";

  const reg = await fetch(`${AUTH_BASE}/auth/register`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ username, email, password }),
  });
  if (reg.status !== 200) {
    throw new Error(`register failed for ${username}: ${reg.status} ${await reg.text()}`);
  }

  const login = await fetch(`${AUTH_BASE}/auth/login`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ username, password }),
  });
  if (login.status !== 200) {
    throw new Error(`login failed for ${username}: ${login.status} ${await login.text()}`);
  }

  const { token } = await login.json();
  return { username, token, userId: decodeUserId(token) };
}

function connectListener(token) {
  return new Promise((resolve, reject) => {
    const pending = new Map(); // taskId -> { resolve, reject, timer }

    const client = new Client({
      brokerURL: WS_URL,
      webSocketFactory: () => new WebSocket(WS_URL, { headers: { Authorization: `Bearer ${token}` } }),
      connectHeaders: { Authorization: `Bearer ${token}` },
      reconnectDelay: 0,
      onConnect: () => {
        client.subscribe("/user/queue/tasks", (message) => {
          const receivedAt = Date.now();
          const event = JSON.parse(message.body);
          if (event.type !== "upsert") return;
          const waiter = pending.get(event.task.taskId);
          if (!waiter) return;
          clearTimeout(waiter.timer);
          pending.delete(event.task.taskId);
          waiter.resolve({ receivedAt, publishedAt: Date.parse(event.task.updatedAt) });
        });
        resolve({ client, pending });
      },
      onStompError: (frame) => reject(new Error(`STOMP error: ${frame.headers.message}`)),
      onWebSocketError: (err) => reject(err),
    });

    client.activate();
  });
}

function awaitPush(pending, taskId) {
  return new Promise((resolve, reject) => {
    const timer = setTimeout(() => {
      pending.delete(taskId);
      reject(new Error(`timed out waiting for push for task ${taskId}`));
    }, PER_EVENT_TIMEOUT_MS);
    pending.set(taskId, { resolve, timer });
  });
}

function percentile(sorted, p) {
  const idx = Math.min(sorted.length - 1, Math.ceil((p / 100) * sorted.length) - 1);
  return sorted[idx];
}

async function main() {
  console.log(`run id ${RUN_ID}: registering actor + listener users`);
  const actor = await registerAndLogin(`loadtest_lat_${RUN_ID}_actor`);
  const listener = await registerAndLogin(`loadtest_lat_${RUN_ID}_listener`);

  const { client, pending } = await connectListener(listener.token);
  console.log("listener connected and subscribed to /user/queue/tasks");

  const latencies = [];
  const failures = [];
  let lastTaskId = null;

  for (let i = 0; i < ITERATIONS; i++) {
    const createNew = i % 2 === 0 || lastTaskId === null;
    try {
      let waitPromise;
      let taskId;

      if (createNew) {
        const res = await fetch(`${TASK_BASE}/tasks`, {
          method: "POST",
          headers: { "Content-Type": "application/json", Authorization: `Bearer ${actor.token}` },
          body: JSON.stringify({
            title: `latency test ${RUN_ID}-${i}`,
            description: "Created during a latency test run",
            deadline: new Date(Date.now() + 86400000).toISOString(),
            assignedTo: listener.userId,
          }),
        });
        const body = await res.json();
        taskId = body.id;
        waitPromise = awaitPush(pending, taskId);
        lastTaskId = taskId;
      } else {
        taskId = lastTaskId;
        waitPromise = awaitPush(pending, taskId);
        await fetch(`${TASK_BASE}/tasks/${taskId}/status`, {
          method: "PATCH",
          headers: { "Content-Type": "application/json", Authorization: `Bearer ${actor.token}` },
          body: JSON.stringify({ status: "DONE" }),
        });
        lastTaskId = null;
      }

      const { receivedAt, publishedAt } = await waitPromise;
      latencies.push(receivedAt - publishedAt);
    } catch (err) {
      failures.push(err.message);
    }
    await new Promise((r) => setTimeout(r, PAUSE_BETWEEN_MS));
  }

  client.deactivate();

  latencies.sort((a, b) => a - b);
  const median = latencies.length
    ? latencies.length % 2 === 1
      ? latencies[(latencies.length - 1) / 2]
      : (latencies[latencies.length / 2 - 1] + latencies[latencies.length / 2]) / 2
    : NaN;

  console.log("\n--- WebSocket push latency results ---");
  console.log(`samples: ${latencies.length} / ${ITERATIONS} (${failures.length} failed/timed out)`);
  if (latencies.length) {
    console.log(`median: ${median} ms`);
    console.log(`p95: ${percentile(latencies, 95)} ms`);
    console.log(`min: ${latencies[0]} ms, max: ${latencies[latencies.length - 1]} ms`);
  }
  if (failures.length) {
    console.log("failures:", failures.slice(0, 5));
  }
  console.log(
    `\nclean up before the next /train call: docker exec -i mongo mongosh sayless < loadtest/cleanup.mongo.js`
  );

  process.exit(failures.length > ITERATIONS * 0.05 ? 1 : 0);
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
