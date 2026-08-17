// Cleans up whatever the load and latency test scripts left behind — users,
// tasks, notifications — matched by the "loadtest_" prefix both scripts use.
// Should run after every test, and definitely before the model gets
// retrained, so none of this fake data ends up in the training set.
//
// Usage (from repo root, with the docker-compose stack running):
//   docker exec -i mongo mongosh sayless < loadtest/cleanup.mongo.js

const loadTestUsers = db.users.find({ username: /^loadtest_/ }).toArray();
const userIds = loadTestUsers.map((u) => u._id.toString());

print(`found ${userIds.length} load-test users`);

if (userIds.length > 0) {
  const taskResult = db.tasks.deleteMany({
    $or: [{ createdBy: { $in: userIds } }, { assignedTo: { $in: userIds } }],
  });
  print(`deleted ${taskResult.deletedCount} tasks`);

  const notificationResult = db.notifications.deleteMany({ userId: { $in: userIds } });
  print(`deleted ${notificationResult.deletedCount} notifications`);

  const userResult = db.users.deleteMany({ _id: { $in: loadTestUsers.map((u) => u._id) } });
  print(`deleted ${userResult.deletedCount} users`);
} else {
  print('nothing to clean up');
}
