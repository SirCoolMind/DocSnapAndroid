# AGENTS.md

DocSnap Android: a native Android app for on-device OCR. Read `docs/spec.md` before building
anything; it holds the agreed design and the build plan. Decisions live in `docs/adr/`.

## Hard rules

- **No network.** `AndroidManifest.xml` declares `android.permission.CAMERA` and nothing else.
  Never add `INTERNET`, and never add a dependency that downloads models at runtime (no unbundled
  ML Kit, no Play services document scanner). Check the merged manifest after adding any library:
  `aapt2 dump permissions app/build/outputs/apk/debug/app-debug.apk`.
- **Nothing persists.** Images live in memory or `cacheDir` and are deleted when the user leaves
  the result screen. No database, no gallery writes.
- **arm64-v8a only.** Single owner device: Samsung Galaxy S24 Ultra.

## Building

The build runs in WSL (`Ubuntu-24.04`) with the Android SDK at `/opt/android-sdk` and JDK 17 at
`/usr/lib/jvm/java-17-openjdk-amd64`. Run `build-local.bat` from Windows; it calls
`./gradlew assembleDebug` in WSL, runs `scripts/check-apk.sh` (permissions and ABIs), and opens the
APK folder. The owner copies the APK to the phone
manually. Unit tests: `./gradlew testDebugUnitTest` in WSL.

## Agent skills

### Issue tracker

Issues are tracked in GitHub Issues on SirCoolMind/DocSnapAndroid, managed with the `gh` CLI. See `docs/agents/issue-tracker.md`.

### Triage labels

Uses the five default triage labels: needs-triage, needs-info, ready-for-agent, ready-for-human, wontfix. See `docs/agents/triage-labels.md`.

### Domain docs

Single-context: one `CONTEXT.md` and `docs/adr/` at the repo root. See `docs/agents/domain.md`.

## Git

Commit to `main` in small steps. Push only when the owner asks.
