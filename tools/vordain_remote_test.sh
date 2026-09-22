#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${VORDAIN_RELAY_URL:-http://127.0.0.1:8081}"
SERVER_DEVICE_ID="${VORDAIN_REMOTE_TEST_SERVER_ID:-server-debug-device}"
PARENT_DEVICE_ID="${VORDAIN_PARENT_DEVICE_ID:-parent-debug-device}"
CHILD_DEVICE_ID="${VORDAIN_CHILD_DEVICE_ID:-child-debug-device}"

usage() {
  cat <<USAGE
Usage: $0 <command>

Commands:
  health
  stats
  parent-health
  child-health
  parent-send-policy
  child-fetch-policy
  child-import-policy
  child-send-status
  parent-fetch-status
  child-start-guard
  child-stop-guard
  summary
USAGE
}

now_millis() {
  date +%s%3N
}

queue_command() {
  local target="$1"
  local type="$2"
  local id="remote-test-$(now_millis)-$type"
  local created
  created="$(now_millis)"
  local payload
  payload=$(cat <<PAYLOAD
VORDAIN_DEBUG_REMOTE_TEST_COMMAND_V1
commandId=$id
type=$type
sourceDeviceId=$SERVER_DEVICE_ID
targetDeviceId=$target
createdAtMillis=$created
status=PENDING
warning=Local debug remote test harness only. Production builds use no active controller.
PAYLOAD
)
  curl --silent --show-error --fail \
    --request POST \
    --header "Content-Type: text/plain; charset=utf-8" \
    --data-binary "$payload" \
    "$BASE_URL/debug/v1/test-commands"
  printf '\nqueued=%s target=%s type=%s\n' "$id" "$target" "$type"
}

case "${1:-}" in
  health)
    curl --silent --show-error --fail "$BASE_URL/health"
    printf '\n'
    ;;
  stats)
    curl --silent --show-error --fail "$BASE_URL/debug/v1/stats"
    ;;
  parent-health)
    queue_command "$PARENT_DEVICE_ID" "PARENT_RELAY_HEALTH_CHECK"
    ;;
  child-health)
    queue_command "$CHILD_DEVICE_ID" "CHILD_RELAY_HEALTH_CHECK"
    ;;
  parent-send-policy)
    queue_command "$PARENT_DEVICE_ID" "PARENT_SEND_POLICY_BUNDLE"
    ;;
  child-fetch-policy)
    queue_command "$CHILD_DEVICE_ID" "CHILD_FETCH_POLICY_BUNDLE"
    ;;
  child-import-policy)
    queue_command "$CHILD_DEVICE_ID" "CHILD_IMPORT_LATEST_POLICY_BUNDLE"
    ;;
  child-send-status)
    queue_command "$CHILD_DEVICE_ID" "CHILD_SEND_STATUS_BUNDLE"
    ;;
  parent-fetch-status)
    queue_command "$PARENT_DEVICE_ID" "PARENT_FETCH_CHILD_STATUS_MESSAGES"
    ;;
  child-start-guard)
    queue_command "$CHILD_DEVICE_ID" "CHILD_START_BASIC_DNS_GUARD"
    ;;
  child-stop-guard)
    queue_command "$CHILD_DEVICE_ID" "CHILD_STOP_BASIC_DNS_GUARD"
    ;;
  summary)
    curl --silent --show-error --fail \
      "$BASE_URL/debug/v1/test-results?targetDeviceId=$SERVER_DEVICE_ID"
    ;;
  ""|-h|--help)
    usage
    ;;
  *)
    usage
    exit 2
    ;;
esac
