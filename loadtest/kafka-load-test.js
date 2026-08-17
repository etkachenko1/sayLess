import http from 'k6/http';
import { check } from 'k6';
import { Counter } from 'k6/metrics';
import encoding from 'k6/encoding';

// Hitting task-service/auth-service directly instead of the gateway — every
// VU here shares one IP, so the gateway's 100 req/min limiter would just be
// the thing we measure instead of Kafka.
const AUTH_BASE = __ENV.AUTH_BASE || 'http://host.docker.internal:8081';
const TASK_BASE = __ENV.TASK_BASE || 'http://host.docker.internal:8082';

const VUS = 15;
const DURATION = '60s';
export const RUN_ID = `${Date.now()}`;

export const options = {
  vus: VUS,
  duration: DURATION,
  thresholds: {
    http_req_failed: ['rate<0.01'],
  },
};

const kafkaEvents = new Counter('kafka_events_published');

function decodeUserId(token) {
  const payload = token.split('.')[1];
  const bytes = encoding.b64decode(payload, 'rawurl');
  const json = JSON.parse(String.fromCharCode.apply(null, new Uint8Array(bytes)));
  return json.sub;
}

export function setup() {
  const users = [];
  for (let i = 0; i < VUS; i++) {
    const username = `loadtest_${RUN_ID}_${i}`;
    const email = `${username}@sayless.local`;
    const password = 'LoadTest123';

    const reg = http.post(
      `${AUTH_BASE}/auth/register`,
      JSON.stringify({ username, email, password }),
      { headers: { 'Content-Type': 'application/json' } }
    );
    if (reg.status !== 200) {
      throw new Error(`setup: register failed for ${username}: ${reg.status} ${reg.body}`);
    }

    const login = http.post(
      `${AUTH_BASE}/auth/login`,
      JSON.stringify({ username, password }),
      { headers: { 'Content-Type': 'application/json' } }
    );
    if (login.status !== 200) {
      throw new Error(`setup: login failed for ${username}: ${login.status} ${login.body}`);
    }

    const token = login.json('token');
    users.push({ username, token, userId: decodeUserId(token) });
  }
  console.log(`setup: registered ${users.length} load-test users with run id ${RUN_ID}`);
  return { users };
}

export default function (data) {
  const u = data.users[(__VU - 1) % data.users.length];
  const headers = {
    'Content-Type': 'application/json',
    Authorization: `Bearer ${u.token}`,
  };

  // 1. POST /tasks -> TaskCreatedEvent
  const createRes = http.post(
    `${TASK_BASE}/tasks`,
    JSON.stringify({
      title: `load test task ${__VU}-${__ITER}`,
      description: 'created by kafka-load-test.js',
      deadline: new Date(Date.now() + 86400000).toISOString(),
      assignedTo: u.userId,
    }),
    { headers }
  );
  const created = check(createRes, { 'create: status 200': (r) => r.status === 200 });
  if (created) kafkaEvents.add(1);
  if (!created) return;

  const taskId = createRes.json('id');

  // 2. PUT /tasks/{id} reassigning to the next user in the pool -> TaskAssignedEvent
  const buddy = data.users[__VU % data.users.length];
  const reassignRes = http.put(
    `${TASK_BASE}/tasks/${taskId}`,
    JSON.stringify({ assignedTo: buddy.userId }),
    { headers }
  );
  const reassigned = check(reassignRes, { 'reassign: status 200': (r) => r.status === 200 });
  if (reassigned) kafkaEvents.add(1);

  // 3. PATCH /tasks/{id}/status DONE -> TaskCompletedEvent (creator can still complete after reassignment)
  const completeRes = http.patch(
    `${TASK_BASE}/tasks/${taskId}/status`,
    JSON.stringify({ status: 'DONE' }),
    { headers }
  );
  const completed = check(completeRes, { 'complete: status 200': (r) => r.status === 200 });
  if (completed) kafkaEvents.add(1);
}

export function teardown(data) {
  console.log(
    `teardown: run id ${RUN_ID} finished. ${data.users.length} load-test users and their tasks ` +
      `still exist in MongoDB — run loadtest/cleanup.js with this run id before any model retrain.`
  );
}
