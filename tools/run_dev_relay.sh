#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

HOST="${VORDAIN_DEV_RELAY_HOST:-0.0.0.0}"
PORT="${VORDAIN_DEV_RELAY_PORT:-8081}"

cat <<EOF
Starting Vordain Guard local dev relay.

Local debug only. Use on a trusted local network and do not expose this port to the internet.
Default URL for tablet testing: http://192.168.68.81:${PORT}
Bind: ${HOST}:${PORT}

EOF

./gradlew :backend:relay-api:run --args="${HOST} ${PORT}"
