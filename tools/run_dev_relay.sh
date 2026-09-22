#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

HOST="${VORDAIN_DEV_RELAY_HOST:-127.0.0.1}"
PORT="${VORDAIN_DEV_RELAY_PORT:-8081}"

cat <<EOF
Starting Vordain Guard local dev relay.

Unauthenticated cleartext debug service. The safe default is loopback only.
The public Android configuration does not connect to this HTTP service.
Keep the relay on loopback and never expose its port to the internet.
Default local URL: http://127.0.0.1:${PORT}
Bind: ${HOST}:${PORT}

EOF

./gradlew :backend:relay-api:run --args="${HOST} ${PORT}"
