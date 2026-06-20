#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
download_dir="${HOME}/tablet-download"
skip_build="false"
version="0.1.0-beta.1"

for arg in "$@"; do
    case "${arg}" in
        --skip-build)
            skip_build="true"
            ;;
        *)
            echo "Unknown argument: ${arg}" >&2
            echo "Usage: tools/export_beta_apks.sh [--skip-build]" >&2
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
child_out="${download_dir}/vordain-guard-child-${version}.apk"
parent_out="${download_dir}/vordain-guard-parent-${version}.apk"

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

cp "${child_apk}" "${child_out}"
cp "${parent_apk}" "${parent_out}"

(
    cd "${download_dir}"
    sha256sum \
        "$(basename "${child_out}")" \
        "$(basename "${parent_out}")" > SHA256SUMS
)

echo "Beta APKs copied to ${download_dir}"
ls -lh "${child_out}" "${parent_out}" "${download_dir}/SHA256SUMS"
echo "Serve locally for tablet download with:"
echo "cd ${download_dir} && python3 -m http.server 8080 --bind 192.168.68.81"
