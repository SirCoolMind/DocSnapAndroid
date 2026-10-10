# DocSnap Android: spec and build plan

Agreed on 2026-10-11. Terms are defined in `CONTEXT.md`; the main decision is ADR 0001.

## Facts

- App name **DocSnap Android**, package `com.sircoolmind.docsnaplocal`.
- `minSdk 29`, `targetSdk 36`, `compileSdk 36`, arm64-v8a only, portrait-locked, English UI
  (all strings in resources so Malay can be added later).
- Owner device: Galaxy S24 Ultra (Android 14+).
- Permissions: `CAMERA` only.

## Screens

### Camera (the app opens here)

- Bottom mode switch **Scan | Live**, like the stock camera's Photo / Video.
- Gallery import icon in a corner (Photo Picker, no storage permission). An import goes through
  the Scan flow.
- Flashlight toggle and pinch-to-zoom in both modes (CameraX picks the telephoto lens itself).
- Tap-to-focus in Scan mode only; Live mode uses continuous autofocus because a tap means Freeze.
- Asks for camera permission on first launch, with a plain explanation if denied.

### Live mode

- `ImageAnalysis` frames (about 720p) go to ML Kit; text blocks are boxed over the preview.
  Drop frames while one is being read.
- **Tap anywhere to Freeze**: show the frozen frame with the last live result immediately, then
  capture a full-res still, re-read it, and swap the result in when ready. Keep any selection
  that still makes sense, or clear it if mapping is unclear.
- Selection: tap a word, drag across words in reading order (across lines), long-press for a line,
  double-tap for a block.
- **Selection bar**: Copy, Select all, Share. Copy shows a toast and stays frozen.
- Back button or the shutter button unfreezes back to Live.

### Scan mode

- Capture a full-res photo (or import one).
- OpenCV finds the **Corners** on the still photo (no live outline in v1). Show them on the
  image; the user drags any corner to adjust. If none are found, start from the image's edges.
- **Flatten** with a perspective warp, then OCR the flattened page.

### Result screen

- Toggle between **image with overlay** (same selection behaviour as Live freeze) and
  **editable text** (fix OCR mistakes before copying).
- Buttons: Copy all, Share text.
- Leaving the screen deletes the images.

## Orientation

- Pass the camera's `rotationDegrees` to ML Kit.
- Gallery imports: honour EXIF orientation.
- Sideways or upside-down photos: turn **Upright** before OCR. Port the idea from DocSnap's
  `D:\Projects\DocSnap\static\js\upright.js`, or try the four rotations with ML Kit and keep the
  one with the most confident, most plentiful text, whichever is simpler and works.

## Privacy

- No `INTERNET` permission, verified on the built APK.
- Nothing is saved: images in memory or `cacheDir`, deleted on leaving the result screen and on
  app start.

## Tests

- JVM unit tests (`app/src/test`) for pure logic: reading-order selection, drag range, line and
  block selection, image-to-screen coordinate mapping (including rotation and crop/fit),
  corner ordering and corner clamping.
- Camera, OCR quality and gestures are tested by hand on the S24 Ultra.

## Build plan

Each step ends with a working debug APK and a commit.

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

## Later (not v1)

- Scan history (text + thumbnail, app-private). The owner asked to be reminded.
- Live document outline in the Scan viewfinder.
- PDF export, receipt parsing, Malay UI, Chinese script.
