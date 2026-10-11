#!/usr/bin/env bash
# Checks the built APK against the hard rules in AGENTS.md: CAMERA is the only permission
# (plus androidx's signature-only receiver permission, which never leaves the app) and
# arm64-v8a is the only ABI. Prints the APK size. Exits non-zero on any violation.
set -euo pipefail

apk="${1:-app/build/outputs/apk/debug/app-debug.apk}"
aapt2="$(ls "${ANDROID_HOME:-/opt/android-sdk}"/build-tools/*/aapt2 | sort -V | tail -1)"
package="$("$aapt2" dump packagename "$apk")"
allowed=("android.permission.CAMERA" "$package.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION")

status=0
permissions="$("$aapt2" dump permissions "$apk" | sed -n "s/^uses-permission: name='\([^']*\)'.*/\1/p")"
for permission in $permissions; do
    ok=0
    for a in "${allowed[@]}"; do [ "$permission" = "$a" ] && ok=1; done
    if [ "$ok" = 0 ]; then
        echo "FAIL: unexpected permission $permission"
        status=1
    fi
done

abis="$(unzip -l "$apk" | awk '{print $4}' | sed -n 's|^lib/\([^/]*\)/.*|\1|p' | sort -u | tr '\n' ' ')"
if [ "$abis" != "arm64-v8a " ]; then
    echo "FAIL: unexpected ABIs: $abis"
    status=1
fi

echo "Permissions: $(echo $permissions)"
echo "ABIs: $abis"
echo "APK size: $(( $(stat -c %s "$apk") / 1024 / 1024 )) MB"
[ "$status" = 0 ] && echo "APK check passed"
exit "$status"
