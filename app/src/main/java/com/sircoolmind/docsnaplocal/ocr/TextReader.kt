package com.sircoolmind.docsnaplocal.ocr

import android.graphics.Bitmap
import android.graphics.Rect
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.sircoolmind.docsnaplocal.geometry.Box
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.Closeable
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** ML Kit text recognition with the bundled Latin model; runs entirely on the phone. */
class TextReader : Closeable {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    /** Reads [image]; boxes come back in the upright image's coordinates. */
    fun process(image: InputImage): Task<Text> = recognizer.process(image)

    suspend fun read(bitmap: Bitmap): OcrPage =
        process(InputImage.fromBitmap(bitmap, 0)).await().toOcrPage(bitmap.width.toFloat(), bitmap.height.toFloat())

    override fun close() = recognizer.close()
}

fun Text.toOcrPage(width: Float, height: Float): OcrPage = OcrPage(
    width,
    height,
    textBlocks.mapNotNull { block ->
        val lines = block.lines.mapNotNull { line ->
            val words = line.elements.mapNotNull { element ->
                element.boundingBox?.let { OcrWord(element.text, it.toBox(), element.confidence) }
            }
            val box = line.boundingBox?.toBox()
            if (words.isEmpty() || box == null) null else OcrLine(words, box, line.angle)
        }
        val box = block.boundingBox?.toBox()
        if (lines.isEmpty() || box == null) null else OcrBlock(lines, box)
    },
)

private fun Rect.toBox() = Box(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat())

suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { cont.resume(it) }
    addOnFailureListener { cont.resumeWithException(it) }
    addOnCanceledListener { cont.cancel() }
}
