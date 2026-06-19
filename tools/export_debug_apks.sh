#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
download_dir="${HOME}/tablet-download"

mkdir -p "${download_dir}"

(
    cd "${repo_root}"
    ./gradlew :apps:parent-app:assembleDebug :apps:child-app:assembleDebug
)

cp "${repo_root}/apps/child-app/build/outputs/apk/debug/child-app-debug.apk" \
    "${download_dir}/vordain-guard-child-debug.apk"
cp "${repo_root}/apps/parent-app/build/outputs/apk/debug/parent-app-debug.apk" \
    "${download_dir}/vordain-guard-parent-debug.apk"

(
    cd "${download_dir}"
    sha256sum \
        vordain-guard-child-debug.apk \
        vordain-guard-parent-debug.apk > SHA256SUMS
)

echo "Debug APKs copied to ${download_dir}"
echo "Serve locally for tablet download with:"
echo "cd ${download_dir} && python3 -m http.server 8080 --bind 192.168.68.81"
