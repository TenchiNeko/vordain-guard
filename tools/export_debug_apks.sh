#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
download_dir="${HOME}/tablet-download"
skip_build="false"

for arg in "$@"; do
    case "${arg}" in
        --skip-build)
            skip_build="true"
            ;;
        *)
            echo "Unknown argument: ${arg}" >&2
            echo "Usage: tools/export_debug_apks.sh [--skip-build]" >&2
            exit 2
            ;;
    esac
done

mkdir -p "${download_dir}"

if [[ "${skip_build}" != "true" ]]; then
    (
        cd "${repo_root}"
        ./gradlew :apps:parent-app:assembleDebug :apps:child-app:assembleDebug
    )
fi

child_apk="${repo_root}/apps/child-app/build/outputs/apk/debug/child-app-debug.apk"
parent_apk="${repo_root}/apps/parent-app/build/outputs/apk/debug/parent-app-debug.apk"

if [[ ! -f "${child_apk}" ]]; then
    echo "Missing child APK: ${child_apk}" >&2
    echo "Run without --skip-build or build :apps:child-app:assembleDebug first." >&2
    exit 1
fi

if [[ ! -f "${parent_apk}" ]]; then
    echo "Missing parent APK: ${parent_apk}" >&2
    echo "Run without --skip-build or build :apps:parent-app:assembleDebug first." >&2
    exit 1
fi

cp "${child_apk}" "${download_dir}/vordain-guard-child-debug.apk"
cp "${parent_apk}" "${download_dir}/vordain-guard-parent-debug.apk"

(
    cd "${download_dir}"
    sha256sum \
        vordain-guard-child-debug.apk \
        vordain-guard-parent-debug.apk > SHA256SUMS
)

echo "Debug APKs copied to ${download_dir}"
echo "Serve locally for tablet download with:"
echo "cd ${download_dir} && python3 -m http.server 8080 --bind YOUR_LAN_IP"
