# 1. A native app with bundled, on-device OCR and no network permission

- Status: accepted; point 5 amended by ADR 0002 (APK published on GitHub Releases)
- Date: 2026-10-11

## Context

The owner wants Google Lens-style OCR on a Samsung Galaxy S24 Ultra without any image or text
leaving the phone. Two features: capture and scan a document, and live OCR where words can be
tapped and copied. The sister project DocSnap (`D:\Projects\DocSnap`) already does OCR in the
browser with Tesseract.js, but a browser is too slow for smooth live OCR with tap-to-select.

## Decision

1. **Native Kotlin app**, Jetpack Compose UI, CameraX. A separate repo; ideas (such as DocSnap's
   upright detection in `static/js/upright.js`) may be ported, code is not shared.
2. **ML Kit Text Recognition v2, bundled Latin model** (`com.google.mlkit:text-recognition`, not
   the `play-services-mlkit-*` variant). The model ships in the APK and runs in the app's process.
   It returns block / line / word boxes, which tap-to-select needs. English and Malay only.
3. **OpenCV** (arm64 only) for document edge detection and perspective flattening. Not the ML Kit
   Document Scanner, which runs inside Google Play services, outside this app's permissions.
4. **No `INTERNET` permission, ever.** Android then blocks all network access from the app,
   including any library telemetry. Camera is the only permission; gallery import uses the
   Photo Picker.
5. **Personal sideloaded build.** Debug APK built in WSL, copied to the phone by hand. No store.

## Consequences

- The privacy promise is checkable from Settings → Apps → Permissions.
- ML Kit is proprietary, so the app could not go on F-Droid. Fine for personal use.
- APK carries roughly 4 MB of ML Kit model plus 10–15 MB of OpenCV.
- Scripts other than Latin (Chinese, Tamil) are out of scope; adding Chinese means another bundled
  ML Kit model.
- Tesseract was rejected: too slow for live frames and weaker on angled photos.
