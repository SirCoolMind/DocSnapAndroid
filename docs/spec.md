# DocSnap Android: spec and build plan

Agreed on 2026-10-11; updated on 2026-10-12 to match v0.1.0beta as built. Terms are defined in
`CONTEXT.md`; the main decision is ADR 0001.

## Facts

- App name **DocSnap for Android** (launcher label "DocSnap Android"), package
  `com.sircoolmind.docsnaplocal`. Current version **v0.1.0beta** (`versionName` `0.1.0beta`,
  `versionCode` 2).
- `minSdk 29`, `targetSdk 36`, `compileSdk 36`, arm64-v8a only, portrait-locked, English UI
  (all strings in resources so Malay can be added later).
- Owner device: Galaxy S24 Ultra (Android 14+).
- Permissions: `CAMERA` only.
- Toolchain: Gradle 9.8.1, Android Gradle Plugin 9.4.1 (built-in Kotlin), Kotlin 2.4.21, Jetpack
  Compose, CameraX 1.6, ML Kit Text Recognition 16.0.1 (bundled Latin model), OpenCV 4.14.
  AndroidX libraries are pinned to the newest versions that build against `compileSdk 36`; newer
  Compose, core and lifecycle releases need 37.

## Screens

### Camera (the app opens here)

- Bottom mode switch **Scan | Live**, like the stock camera's Photo / Video.
- Gallery import icon bottom-left (Photo Picker, no storage permission). An import goes through
  the Scan flow.
- Flashlight toggle bottom-right and pinch-to-zoom in both modes; the zoom ratio shows while
  zoomed. CameraX picks the telephoto lens itself.
- Tap-to-focus in Scan mode only; Live mode uses continuous autofocus because a tap means Freeze.
- The viewfinder shows the whole 4:3 frame (fit, not crop), so what is shown is what is read.
- Asks for camera permission on first launch, with a plain explanation if denied and a way to
  Settings once Android stops asking.
- A **⋮** menu in the viewfinder's top-right corner (hidden while frozen) holds **About**: name,
  version, tech stack, the no-internet note and the GitHub link. The link is handed to the
  browser; the app itself never touches the network.

### Live mode

- `ImageAnalysis` frames (about 1280 x 960) go to ML Kit; text blocks are boxed over the preview.
  Drop frames while one is being read, and read at most five frames a second.
- **Tap anywhere to Freeze**: show the frozen frame with the last live result immediately, then
  capture a full-res still, re-read it, and swap the result in when ready. A selection made
  meanwhile is kept only if its words appear unambiguously in the new reading; otherwise it is
  cleared.
- Selection: tap a word, drag across words in reading order (across lines), long-press for a line,
  double-tap for a block. Long-press, double-tap and Freeze vibrate.
- **Selection bar**: the selected text (up to three lines, scrolls when longer), then Copy,
  Select all, Share. Copy stays frozen. On Android 13+ the system's own clipboard preview
  confirms the copy; older versions show a toast quoting what was copied.
- Back button or the shutter button unfreezes back to Live.

### Scan mode

- Capture a full-res photo (capped near 12 MP) or import one.
- OpenCV finds the **Corners** on the still photo (no live outline in v1): Canny edges on an 800 px
  copy, falling back to an Otsu threshold. Show them on the image; the user drags any corner,
  with a **magnifier** showing the area under the finger. If none are found, start from the
  image's edges and say so.
- **Flatten** with a perspective warp, turn it **Upright**, then OCR the page.

### Result screen

- Toggle between **image with overlay** (same selection behaviour as Live freeze) and
  **editable text** (fix OCR mistakes before copying).
- Buttons: Copy all, Share text. Both use the text as edited.
- Leaving the screen drops the images.

## Orientation

- Pass the camera's `rotationDegrees` to ML Kit; still photos are turned upright in memory.
- Gallery imports: honour EXIF orientation, including mirrored values.
- **Upright**: before OCR, a 1280 px copy of the flattened page is read at 0, 90, 180 and 270
  degrees; the turn with the highest score wins (letters and digits in level lines, weighted by
  ML Kit's confidence). Ties keep the page as it is.

## Battery

- After **one minute without a touch** the camera turns off and the viewfinder says so. The first
  touch only wakes it; the flashlight comes back if it was on.
- After a Freeze's full-res re-read, the camera stays off until unfreeze.
- The analysis stream runs only in Live mode.
- The camera is released on the corners and result screens, when the screen sleeps and when the
  app goes to the background.

## Privacy

- No `INTERNET` permission, verified on the built APK by `scripts/check-apk.sh`. Libraries bring
  in `INTERNET` (ML Kit's datatransport telemetry) and `ACCESS_NETWORK_STATE` (media3, via
  CameraX); the manifest strips both with `tools:node="remove"`, so Android blocks that traffic.
- Nothing is saved: images live in memory, and `cacheDir` is emptied on app start and whenever the
  camera screen is shown again.

## Tests

- JVM unit tests (`app/src/test`, 44 tests) for pure logic: reading-order selection, drag range,
  line and block selection, selection remap after Freeze, selected-text snippets, image-to-screen
  coordinate mapping (including rotation and crop/fit), magnifier clipping, corner ordering and
  clamping, flattened size, upright scoring, import sample size and the Live read throttle.
- Camera, OCR quality, gestures and battery behaviour are tested by hand on the S24 Ultra.

## Build plan

Each step ended with a working debug APK and a commit. All are done.

1. **Skeleton**: Gradle (Kotlin DSL, version catalog, wrapper), Compose, one empty screen,
   manifest with `CAMERA` only, `.gitattributes` forcing LF for `gradlew`, `.gitignore`,
   `build-local.bat`. Verify the build in WSL.
2. **Camera**: permission flow, CameraX preview, Scan | Live switch, flashlight, pinch-to-zoom,
   tap-to-focus in Scan.
3. **Live OCR**: ML Kit on analysis frames, block boxes overlay, coordinate mapping with tests.
4. **Freeze and select**: freeze on tap, full-res re-read swap, selection model with tests,
   selection bar, copy / share.
5. **Scan**: capture, OpenCV corner detection, corner drag UI, flatten, OCR.
6. **Result screen**: overlay / text toggle, Copy all, Share, gallery import, EXIF, upright.
7. **Hardening**: temp file cleanup, permission check on the APK, size check, README.

Added after the plan, for v0.1.0beta: corner magnifier, zoom readout, haptics, selected-text
preview in the selection bar, About dialog, battery measures.

## Later (not v1)

- Scan history (text + thumbnail, app-private). The owner asked to be reminded.
- Live document outline in the Scan viewfinder.
- A smaller, shrunk and signed release build (the debug APK is about 78 MB).
- PDF export, receipt parsing, Malay UI, Chinese script.
