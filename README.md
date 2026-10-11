# DocSnap Android

On-device OCR for Android. Capture and scan documents, or point the camera at text and tap words to
copy them. Nothing leaves the phone: the app has no internet permission.

Status: v1 feature-complete, testing on the owner's Galaxy S24 Ultra. Design: [docs/spec.md](docs/spec.md),
decisions: [docs/adr/](docs/adr/), terms: [CONTEXT.md](CONTEXT.md).

## What it does

- **Scan mode**: take a photo (or import one with the gallery icon), adjust the four corners, and
  read the flattened page. The result screen shows the page with selectable words, or the text
  for editing, with Copy all and Share text.
- **Live mode**: text blocks are boxed as you point the camera. Tap to freeze, then select words:
  tap a word, drag across words, long-press for a line, double-tap for a block. Back or the shutter
  returns to Live.
- Flashlight and pinch-to-zoom in both modes; tap-to-focus in Scan mode.

## Privacy

- The only permission is `CAMERA`. Libraries ask for `INTERNET` (ML Kit's telemetry) and
  `ACCESS_NETWORK_STATE` (media3); the manifest strips both, so Android blocks any network use.
- The OCR model (ML Kit Text Recognition v2, Latin) is bundled in the APK. Nothing is downloaded.
- Photos stay in memory and are dropped when you leave a scan. The cache folder is emptied on app
  start and after each scan. Nothing is written to the gallery.

## Build

Builds run in WSL (`Ubuntu-24.04`) with the Android SDK at `/opt/android-sdk` and JDK 17.

- Windows: run `build-local.bat`. It builds the debug APK, runs `scripts/check-apk.sh` (fails if
  the APK has any permission other than `CAMERA` or any ABI other than arm64-v8a), and opens the
  APK folder.
- Unit tests, in WSL: `./gradlew testDebugUnitTest`.

Copy `app/build/outputs/apk/debug/app-debug.apk` to the phone and install it.

Libraries are pinned to the newest versions that build against `compileSdk 36` (the spec's
value); newer Compose, core and lifecycle releases need `compileSdk 37`.

## Testing on the phone

Unit tests cover the pure logic: coordinate mapping, selection, corner ordering, upright scoring
and import sizing. Everything with the camera, OCR quality and gestures is checked by hand:

1. First launch asks for the camera; denying shows an explanation and a way to Settings.
2. Scan: shutter, corners found on a page on a contrasting surface, corners draggable, Read text.
3. Scan a page held sideways or upside down: the result should come out upright.
4. Import a photo from the gallery, including one taken in portrait.
5. Result: switch Image / Text, edit text, Copy all, Share text; Back returns to the camera.
6. Live: boxes follow text; tap freezes; the full-res result swaps in; tap, drag, long-press and
   double-tap select; Copy / Select all / Share; Back or shutter unfreezes.
7. Flashlight, pinch-to-zoom in both modes, tap-to-focus in Scan.
8. Settings > Apps > DocSnap Android > Permissions lists Camera only.
