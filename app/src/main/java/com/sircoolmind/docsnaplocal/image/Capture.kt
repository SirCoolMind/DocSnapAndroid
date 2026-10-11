package com.sircoolmind.docsnaplocal.image

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.view.CameraController
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Takes a full-res photo, kept in memory only, cropped to what the viewfinder showed and
 * turned upright.
 */
suspend fun CameraController.takeUprightPhoto(executor: Executor): Bitmap =
    suspendCancellableCoroutine { cont ->
        takePicture(
            executor,
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val result = runCatching { image.use { it.toUprightBitmap() } }
                    result.onSuccess { cont.resume(it) }.onFailure { cont.resumeWithException(it) }
                }

                override fun onError(exception: ImageCaptureException) {
                    cont.resumeWithException(exception)
                }
            },
        )
    }

private fun ImageProxy.toUprightBitmap(): Bitmap {
    val full = toBitmap()
    val crop = cropRect
    val cropped = if (crop.width() in 1 until full.width || crop.height() in 1 until full.height) {
        Bitmap.createBitmap(full, crop.left, crop.top, crop.width(), crop.height())
    } else {
        full
    }
    return rotate(cropped, imageInfo.rotationDegrees)
}

/** Turns [bitmap] clockwise by [degrees]; returns it unchanged for 0. */
fun rotate(bitmap: Bitmap, degrees: Int): Bitmap {
    if (degrees % 360 == 0) return bitmap
    val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}
