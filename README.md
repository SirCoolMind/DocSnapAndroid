# DocSnap for Android

On-device OCR for Android. Scan a document, or point the camera at text and tap words to copy
them. Nothing leaves the phone: the app has no internet permission.

**Version v0.1.0beta.** A beta built for and tested on a Samsung Galaxy S24 Ultra.

## Download and install

1. Download `DocSnap-v0.1.0beta.apk` from the
   [Releases page](https://github.com/SirCoolMind/DocSnapAndroid/releases).
2. Open it on your phone. Android asks you to allow installing apps from the browser or file
   manager you opened it with; allow it for that app.
3. Open **DocSnap Android** and allow the camera.

Requirements: Android 10 or newer on a 64-bit ARM phone (arm64-v8a), which covers almost every
phone from recent years. The app is not on Google Play.

## What it does

- **Scan mode**: take a photo, or import one with the gallery icon. DocSnap finds the page's
  corners (drag any corner to adjust, with a magnifier under your finger), flattens the page,
  turns it upright if it was sideways or upside down, and reads it. The result shows the page
  with selectable words, or the text for editing, with **Copy all** and **Share text**.
- **Live mode**: text is boxed as you point the camera. **Tap to freeze**, then select words:
  tap a word, drag across words, long-press for a line, double-tap for a block. The selection bar
  shows what you selected, with **Copy**, **Select all** and **Share**. Back or the shutter
  returns to Live.
- Flashlight and pinch-to-zoom in both modes; tap-to-focus in Scan mode.
- **⋮ → About** shows the version and a link to this page.

## Privacy

- The only permission is **Camera**. You can check this in Settings → Apps → DocSnap Android →
  Permissions.
- The app has no internet permission, so Android blocks any network use. Some libraries ask for
  network permissions (Google ML Kit's telemetry, a CameraX dependency); the app's manifest
  removes them, and every build is checked for it.
- The OCR model is bundled in the app; nothing is downloaded.
- Photos stay in memory and are dropped when you leave a scan. Nothing is saved to your gallery
  or storage.

## Battery

- The camera turns off after a minute without a touch; tap anywhere to resume.
- While you read a frozen frame, the camera is off.
- Live mode reads at most five frames a second.

## How it is built

- Kotlin and Jetpack Compose, CameraX for the camera.
- [Google ML Kit Text Recognition v2](https://developers.google.com/ml-kit/vision/text-recognition/v2/android),
  bundled Latin model, running on the phone.
- [OpenCV](https://opencv.org/) for page corners and flattening.

Design: [docs/spec.md](docs/spec.md). Decisions: [docs/adr/](docs/adr/). Terms:
[CONTEXT.md](CONTEXT.md).

## Build it yourself

Builds run in WSL (Ubuntu 24.04) with the Android SDK at `/opt/android-sdk` and JDK 17.

- On Windows, run `build-local.bat`. It builds the debug APK, runs `scripts/check-apk.sh` (fails
  if the APK has any permission other than Camera, or any ABI other than arm64-v8a), and opens
  the APK folder.
- In WSL: `./gradlew assembleDebug`, then `bash scripts/check-apk.sh`.
- Unit tests: `./gradlew testDebugUnitTest`.

The APK lands in `app/build/outputs/apk/debug/app-debug.apk`.

## Known limits

- Latin script only (English, Malay and similar). No Chinese or Tamil yet.
- Corner detection works best with the page on a contrasting surface.
- The APK is large (about 78 MB): it is an unshrunk build carrying ML Kit and OpenCV.
- Beta: tested on one phone. Please report problems in
  [Issues](https://github.com/SirCoolMind/DocSnapAndroid/issues).
