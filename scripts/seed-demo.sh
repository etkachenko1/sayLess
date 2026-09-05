#!/usr/bin/env bash
# Requires: curl, jq
#
# Usage:
#   scripts/seed-demo.sh [BASE_URL] [--reset]
#
# The demo account's credentials are public, so anyone can log into it and
# change its tasks. This script sets it up and puts it back.
#
# Without --reset: fills in whatever's missing (accounts, friend link, tasks)
# and leaves existing tasks alone. Safe to run repeatedly.
#
# With --reset: deletes every task the demo account created, then reseeds from
# scratch. Suggested nightly:
#   0 4 * * * /path/to/sayLess/scripts/seed-demo.sh https://sayless.site --reset >> /var/log/seed-demo.log 2>&1
set -euo pipefail

BASE_URL="https://sayless.site"
RESET=false
for arg in "$@"; do
  case "$arg" in
    --reset) RESET=true ;;
    *) BASE_URL="$arg" ;;
  esac
done

DEMO_USERNAME="demo"
DEMO_PASSWORD="Demo12345!"
DEMO_EMAIL="demo@sayless.site"

FRIEND_USERNAME="demo_friend"
FRIEND_PASSWORD="DemoFriend12345!"
FRIEND_EMAIL="demo-friend@sayless.site"

register() {
  local username="$1" email="$2" password="$3"
  local body
  body=$(jq -n --arg u "$username" --arg e "$email" --arg p "$password" \
    '{username: $u, email: $e, password: $p}')
  curl -s -o /dev/null -X POST "$BASE_URL/auth/register" \
    -H "Content-Type: application/json" -d "$body"
}

login() {
  local username="$1" password="$2"
  local body
  body=$(jq -n --arg u "$username" --arg p "$password" '{username: $u, password: $p}')
  curl -s -X POST "$BASE_URL/auth/login" \
    -H "Content-Type: application/json" -d "$body" | jq -r '.token // empty'
}

create_task() {
  local token="$1" title="$2" description="$3" deadline="$4" assigned_to="${5:-}"
  local body
  if [ -n "$assigned_to" ]; then
    body=$(jq -n --arg t "$title" --arg d "$description" --arg dl "$deadline" --arg a "$assigned_to" \
      '{title: $t, description: $d, deadline: $dl, assignedTo: $a}')
  else
    body=$(jq -n --arg t "$title" --arg d "$description" --arg dl "$deadline" \
      '{title: $t, description: $d, deadline: $dl}')
  fi
  curl -s -X POST "$BASE_URL/tasks" \
    -H "Authorization: Bearer $token" -H "Content-Type: application/json" -d "$body" \
    | jq -r '.id // empty'
}

set_status() {
  local token="$1" task_id="$2" status="$3"
  local body
  body=$(jq -n --arg s "$status" '{status: $s}')
  curl -s -o /dev/null -X PATCH "$BASE_URL/tasks/$task_id/status" \
    -H "Authorization: Bearer $token" -H "Content-Type: application/json" -d "$body"
}

days_from_now() {
  date -u -d "$1 days" +"%Y-%m-%dT%H:%M:%S.000Z"
}

echo "Registering demo accounts (a 'username taken' response here is expected on re-runs)..."
register "$DEMO_USERNAME" "$DEMO_EMAIL" "$DEMO_PASSWORD"
register "$FRIEND_USERNAME" "$FRIEND_EMAIL" "$FRIEND_PASSWORD"

DEMO_TOKEN=$(login "$DEMO_USERNAME" "$DEMO_PASSWORD")
FRIEND_TOKEN=$(login "$FRIEND_USERNAME" "$FRIEND_PASSWORD")

if [ -z "$DEMO_TOKEN" ] || [ -z "$FRIEND_TOKEN" ]; then
  echo "Could not log in as $DEMO_USERNAME and/or $FRIEND_USERNAME - aborting" >&2
  exit 1
fi

FRIEND_ID=$(curl -s "$BASE_URL/friends/search?username=$FRIEND_USERNAME" \
  -H "Authorization: Bearer $DEMO_TOKEN" | jq -r '.[0].id // empty')
DEMO_ID=$(curl -s "$BASE_URL/friends/search?username=$DEMO_USERNAME" \
  -H "Authorization: Bearer $FRIEND_TOKEN" | jq -r '.[0].id // empty')

if [ -z "$FRIEND_ID" ] || [ -z "$DEMO_ID" ]; then
  echo "Could not resolve demo/demo_friend user ids - aborting" >&2
  exit 1
fi

ALREADY_FRIENDS=$(curl -s "$BASE_URL/friends/accepted" -H "Authorization: Bearer $DEMO_TOKEN" \
  | jq -r --arg id "$FRIEND_ID" 'any(.[]?; .id == $id)')

if [ "$ALREADY_FRIENDS" = "true" ]; then
  echo "$DEMO_USERNAME and $FRIEND_USERNAME are already friends - skipping."
else
  curl -s -o /dev/null -X POST "$BASE_URL/friends/request?receiverId=$FRIEND_ID" \
    -H "Authorization: Bearer $DEMO_TOKEN"
  curl -s -o /dev/null -X POST "$BASE_URL/friends/accept?requesterId=$DEMO_ID" \
    -H "Authorization: Bearer $FRIEND_TOKEN"
  echo "Connected $DEMO_USERNAME and $FRIEND_USERNAME as friends."
fi

if [ "$RESET" = "true" ]; then
  echo "Resetting $DEMO_USERNAME: deleting the tasks it created..."
  curl -s "$BASE_URL/tasks" -H "Authorization: Bearer $DEMO_TOKEN" \
    | jq -r --arg me "$DEMO_ID" '.[] | select(.createdById == $me) | .id' \
    | while read -r task_id; do
        curl -s -o /dev/null -X DELETE "$BASE_URL/tasks/$task_id" \
          -H "Authorization: Bearer $DEMO_TOKEN"
      done
else
  EXISTING_TASKS=$(curl -s "$BASE_URL/tasks" -H "Authorization: Bearer $DEMO_TOKEN" | jq 'length')
  if [ "$EXISTING_TASKS" -gt 0 ]; then
    echo "$DEMO_USERNAME already has $EXISTING_TASKS tasks - skipping task seeding."
    exit 0
  fi
fi

echo "Creating demo tasks..."

t1=$(create_task "$DEMO_TOKEN" "Write project README" "Cover setup, architecture, and the security pass." "$(days_from_now 4)")
t2=$(create_task "$DEMO_TOKEN" "Review pull request" "Check the new auth flow before it merges." "$(days_from_now 1)" "$FRIEND_ID")
t3=$(create_task "$DEMO_TOKEN" "Fix login page bug" "Password field loses focus on mobile Safari." "$(days_from_now -1)")
t4=$(create_task "$DEMO_TOKEN" "Deploy staging build" "Cut a release candidate for QA." "$(days_from_now -3)")
t5=$(create_task "$DEMO_TOKEN" "Update onboarding docs" "New teammate starts Monday." "$(days_from_now 2)" "$FRIEND_ID")
t6=$(create_task "$DEMO_TOKEN" "Plan next sprint" "Draft the backlog for the next two weeks." "$(days_from_now 7)")

set_status "$DEMO_TOKEN" "$t3" "IN_PROGRESS"
set_status "$DEMO_TOKEN" "$t4" "DONE"
set_status "$DEMO_TOKEN" "$t5" "DONE"

echo "Seeded $DEMO_USERNAME with 6 tasks and 1 friend connection."
