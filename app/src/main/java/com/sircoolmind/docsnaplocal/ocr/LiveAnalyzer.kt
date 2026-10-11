package com.sircoolmind.docsnaplocal.ocr

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

/**
 * Reads camera preview frames. The frame stays open until ML Kit is done with it, so with
 * CameraX's keep-only-latest strategy, frames that arrive meanwhile are dropped.
 */
class LiveAnalyzer(
    private val reader: TextReader,
    private val onFrame: (LiveFrame) -> Unit,
) : ImageAnalysis.Analyzer {

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(image: ImageProxy) {
        val media = image.image
        if (media == null) {
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
