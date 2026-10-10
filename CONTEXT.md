# Context

Glossary for DocSnap Android. Use these terms in code, issues and tests.

- **Live mode**: the camera viewfinder with text blocks boxed in real time, analysed from
  low-resolution preview frames.
- **Freeze**: tapping in Live mode stops the viewfinder on one frame so words can be selected.
  It shows the last live result at once, then swaps in a **full-res re-read**.
- **Full-res re-read**: a still photo captured at freeze time and read again by OCR, replacing
  the live result when it is ready.
- **Scan mode**: capture (or import) a photo of a document, clean it up, and read it.
- **Corners**: the four points of the document outline, found by OpenCV and adjustable by drag.
- **Flatten**: the perspective warp that turns the area inside the corners into a flat page.
- **Result screen**: after a scan, shows the flattened page either as an **image with overlay**
  or as **editable text**, with Copy all and Share.
- **Selection**: a run of words in **reading order** (block, then line, then word). Tap selects a
  word, drag extends, long-press selects a line, double-tap selects a block.
- **Selection bar**: the floating Copy / Select all / Share bar shown while words are selected.
- **Upright**: turning a sideways or upside-down image so text reads normally before OCR.
