package com.sircoolmind.docsnaplocal.image

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import kotlin.math.max

/** Longest side kept when importing; bigger photos are decoded at a power-of-two reduction. */
const val IMPORT_MAX_SIDE = 4000

/** The largest power-of-two reduction that keeps the longest side at or above [maxSide]. */
fun sampleSizeFor(width: Int, height: Int, maxSide: Int): Int {
    var sample = 1
    while (max(width, height) / (sample * 2) >= maxSide) sample *= 2
    return sample
}

/** Decodes a Photo Picker image into memory, turned the way its EXIF orientation says. */
fun decodeImport(resolver: ContentResolver, uri: Uri, maxSide: Int = IMPORT_MAX_SIDE): Bitmap {
    fun open() = resolver.openInputStream(uri) ?: error("Cannot open $uri")

    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    open().use { BitmapFactory.decodeStream(it, null, bounds) }
    val options = BitmapFactory.Options().apply {
        inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight, maxSide)
        inPreferredConfig = Bitmap.Config.ARGB_8888
    }
    val decoded = open().use { BitmapFactory.decodeStream(it, null, options) } ?: error("Cannot decode $uri")
    val orientation = open().use {
        ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
    }
    val matrix = exifMatrix(orientation) ?: return decoded
    return Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
}

private fun exifMatrix(orientation: Int): Matrix? {
    val matrix = Matrix()
    when (orientation) {
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.setScale(-1f, 1f)
        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.setRotate(180f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.apply { setRotate(180f); postScale(-1f, 1f) }
        ExifInterface.ORIENTATION_TRANSPOSE -> matrix.apply { setRotate(90f); postScale(-1f, 1f) }
        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.setRotate(90f)
        ExifInterface.ORIENTATION_TRANSVERSE -> matrix.apply { setRotate(-90f); postScale(-1f, 1f) }
        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.setRotate(-90f)
        else -> return null
    }
    return matrix
}

/** A copy of [bitmap] whose longest side is at most [maxSide]; [bitmap] itself if already small. */
fun scaleDown(bitmap: Bitmap, maxSide: Int): Bitmap {
    val longest = max(bitmap.width, bitmap.height)
    if (longest <= maxSide) return bitmap
    val scale = maxSide.toFloat() / longest
    return Bitmap.createScaledBitmap(
        bitmap,
        (bitmap.width * scale).toInt().coerceAtLeast(1),
        (bitmap.height * scale).toInt().coerceAtLeast(1),
        true,
    )
}
