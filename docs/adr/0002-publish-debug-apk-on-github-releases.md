# 2. Publish the debug APK on GitHub Releases

- Status: accepted
- Date: 2026-10-12
- Amends: ADR 0001, point 5 ("Personal sideloaded build ... No store")

## Context

ADR 0001 planned a personal build, copied to the owner's phone by hand. With v0.1.0beta the owner
wants others to be able to download the APK from the GitHub repository. There is still no store
listing, and the build runs in WSL as before.

## Decision

1. Each release is a git tag `v<versionName>` (first: `v0.1.0beta`) with a GitHub Release that
   carries the APK, named `DocSnap-v<versionName>.apk`.
2. The released APK is the debug build, signed with the WSL debug keystore
   (`~/.android/debug.keystore`, certificate SHA-256
   `91277ad83d02e79e6fb480671eba444868e9de50a74666f0152a518f7dc3e2bf`).
3. `versionCode` goes up by one for every release, so Android installs it as an update.
4. Every released APK passes `scripts/check-apk.sh` (Camera the only permission, arm64-v8a only).

## Consequences

- Users can only update in place if every release is signed with the same key. The debug keystore
  must be backed up; if it is lost, users have to uninstall before installing a new release.
- A debug build is debuggable and unshrunk (about 78 MB). A release build with its own signing
  key, shrinking and a smaller APK is listed under "Later" in the spec; switching keys then means
  one uninstall for existing users.
- The privacy promise is unchanged and still checkable on each APK.
