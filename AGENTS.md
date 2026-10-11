# AGENTS.md

DocSnap for Android: a native Android app for on-device OCR. Read `docs/spec.md` before building
anything; it holds the design as built and the build plan. Decisions live in `docs/adr/`.

## Hard rules

- **No network.** `AndroidManifest.xml` declares `android.permission.CAMERA` and nothing else.
  Never add `INTERNET`, and never add a dependency that downloads models at runtime (no unbundled
  ML Kit, no Play services document scanner). Check the merged manifest after adding any library:
  `bash scripts/check-apk.sh`. Libraries that request `INTERNET` or `ACCESS_NETWORK_STATE`
  (ML Kit's datatransport, media3) are stripped in the manifest with `tools:node="remove"`; do the
  same for any new one, never ship the permission.
- **Nothing persists.** Images live in memory or `cacheDir` and are deleted when the user leaves
  the result screen. No database, no gallery writes.
- **arm64-v8a only.** Main test device: Samsung Galaxy S24 Ultra.
- **compileSdk 36.** AndroidX libraries are pinned to versions that build against it; newer
  Compose, core and lifecycle releases need 37. Raise it only as a deliberate change.

## Building

The build runs in WSL (`Ubuntu-24.04`) with the Android SDK at `/opt/android-sdk` and JDK 17 at
`/usr/lib/jvm/java-17-openjdk-amd64`. Run `build-local.bat` from Windows; it calls
`./gradlew assembleDebug` in WSL, runs `scripts/check-apk.sh` (permissions and ABIs), and opens the
APK folder. Unit tests: `./gradlew testDebugUnitTest` in WSL.

## Releases

See ADR 0002. Bump `versionName` and `versionCode` (by one) in `app/build.gradle.kts`, update the
version in `README.md` and `docs/spec.md`, commit, and tag `v<versionName>` (annotated). The
debug APK is uploaded to the GitHub Release as `DocSnap-v<versionName>.apk`; it must be signed
with the same WSL debug keystore as earlier releases.

## Agent skills

### Issue tracker

Issues are tracked in GitHub Issues on SirCoolMind/DocSnapAndroid, managed with the `gh` CLI. See `docs/agents/issue-tracker.md`.

### Triage labels

Uses the five default triage labels: needs-triage, needs-info, ready-for-agent, ready-for-human, wontfix. See `docs/agents/triage-labels.md`.

### Domain docs

Single-context: one `CONTEXT.md` and `docs/adr/` at the repo root. See `docs/agents/domain.md`.

## Git

Commit to `main` in small steps. Push only when the owner asks. No `Co-Authored-By` trailers in
commit messages.
