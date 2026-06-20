#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${repo_root}"

echo "Running Vordain Guard beta candidate verification..."

./gradlew \
    :core:dev-relay:test \
    :backend:relay-api:test \
    :core:sync-bundle:test \
    :core:alert-center:test \
    :core:audit-log:test \
    :features:bypass-risk:test \
    :core:intelligence:test \
    :vpn:packet:test \
    :vpn:lab:test \
    :vpn:session:test \
    :apps:parent-app:assembleDebug \
    :apps:child-app:assembleDebug \
    :vpn:service:assembleDebug \
    :apps:parent-app:lintDebug \
    :apps:child-app:lintDebug \
    :vpn:service:lintDebug

if git ls-files local.properties | grep -q .; then
    echo "FAIL: local.properties is tracked" >&2
    exit 1
fi

if git ls-files "*.apk" | grep -q .; then
    echo "FAIL: APK file is tracked" >&2
    exit 1
fi

if find . -maxdepth 2 -iname "LICENSE*" -print | grep -q .; then
    echo "FAIL: root LICENSE file exists" >&2
    exit 1
fi

echo "PASS: Vordain Guard beta candidate verification completed."
