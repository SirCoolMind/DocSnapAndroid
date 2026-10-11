package com.sircoolmind.docsnaplocal.ocr

import android.os.SystemClock
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.sircoolmind.docsnaplocal.geometry.Box
import com.sircoolmind.docsnaplocal.geometry.rotateBox
import com.sircoolmind.docsnaplocal.geometry.rotatedSize

/**
 * One Live mode result. [page] is in the upright analysis frame's coordinates; [visible] is the
 * part of that frame shown in the viewfinder.
 */
class LiveFrame(val page: OcrPage, val visible: Box)

/** Shortest time between Live reads (at most five a second); OCR is the heaviest work the app does. */
private const val MIN_READ_INTERVAL_MS = 200L

/**
 * Reads camera preview frames. The frame stays open until ML Kit is done with it, so with
 * CameraX's keep-only-latest strategy, frames that arrive meanwhile are dropped. Frames are
 * also skipped to keep to at most five reads a second.
 */
class LiveAnalyzer(
    private val reader: TextReader,
    private val onFrame: (LiveFrame) -> Unit,
) : ImageAnalysis.Analyzer {

    private val throttle = FrameThrottle(MIN_READ_INTERVAL_MS)

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(image: ImageProxy) {
        val media = image.image
        if (media == null || !throttle.tryAcquire(SystemClock.elapsedRealtime())) {
            image.close()
            return
        }
        val rotation = image.imageInfo.rotationDegrees
        val width = image.width.toFloat()
        val height = image.height.toFloat()
        val crop = image.cropRect.let { Box(it.left.toFloat(), it.top.toFloat(), it.right.toFloat(), it.bottom.toFloat()) }
        val upright = rotatedSize(width, height, rotation)
        val visible = rotateBox(crop, width, height, rotation)

        reader.process(InputImage.fromMediaImage(media, rotation))
            .addOnSuccessListener { onFrame(LiveFrame(it.toOcrPage(upright.x, upright.y), visible)) }
            .addOnCompleteListener { image.close() }
    }
}
