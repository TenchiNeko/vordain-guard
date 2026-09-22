#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${repo_root}"

echo "Running Vordain Guard beta candidate verification..."

fail() {
    echo "FAIL: $1" >&2
    exit 1
}

if git ls-files local.properties | grep -q .; then
    fail "local.properties is tracked"
fi

if git ls-files "*.apk" | grep -q .; then
    fail "an APK file is tracked"
fi

if [[ ! -f LICENSE ]] || ! grep -q "Apache License" LICENSE; then
    fail "root Apache-2.0 LICENSE is missing"
fi

if grep -R -n \
    --exclude-dir=.git \
    --exclude="*.jar" \
    "192\.168\.68\.81" .; then
    fail "a development-specific LAN address remains in the current tree"
fi

if grep -R -n \
    --include="AndroidManifest.xml" \
    'usesCleartextTraffic="true"' apps; then
    fail "an Android manifest permits cleartext traffic"
fi

for snapshot in \
    apps/parent-app/src/main/java/com/vordain/guard/parent/ParentDebugStateSnapshot.kt \
    apps/child-app/src/main/java/com/vordain/guard/child/ChildDebugStateSnapshot.kt; do
    if ! grep -q 'const val DEFAULT_RELAY_BASE_URL = ""' "${snapshot}"; then
        fail "an Android app ships with a default relay URL: ${snapshot}"
    fi
done

if ! grep -q '127\.0\.0\.1' tools/run_dev_relay.sh ||
    ! grep -q '127\.0\.0\.1' \
        backend/relay-api/src/main/kotlin/com/vordain/guard/backend/relayapi/DevRelayServerMain.kt; then
    fail "local dev relay does not default to loopback"
fi

./gradlew \
    test \
    :apps:parent-app:assembleDebug \
    :apps:parent-app:assembleRelease \
    :apps:child-app:assembleDebug \
    :apps:child-app:assembleRelease \
    :vpn:service:assembleDebug \
    :vpn:service:assembleRelease \
    :apps:parent-app:lintDebug \
    :apps:child-app:lintDebug \
    :vpn:service:lintDebug

echo "PASS: Vordain Guard beta candidate verification completed."
