package com.sircoolmind.docsnaplocal.geometry

import kotlin.math.max
import kotlin.math.min

/** A point. Plain Kotlin so the logic can be unit-tested on the JVM. */
data class Vec2(val x: Float, val y: Float) {
    operator fun minus(other: Vec2) = Vec2(x - other.x, y - other.y)
    operator fun plus(other: Vec2) = Vec2(x + other.x, y + other.y)
    fun length() = kotlin.math.hypot(x, y)
}

/** An axis-aligned rectangle. */
data class Box(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val width get() = right - left
    val height get() = bottom - top
    val center get() = Vec2((left + right) / 2, (top + bottom) / 2)

    operator fun contains(p: Vec2) = p.x in left..right && p.y in top..bottom

    fun inflate(by: Float) = Box(left - by, top - by, right + by, bottom + by)

    /** Distance from [p] to the nearest point of the box; 0 inside. */
    fun distanceTo(p: Vec2): Float {
        val dx = max(max(left - p.x, 0f), p.x - right)
        val dy = max(max(top - p.y, 0f), p.y - bottom)
        return kotlin.math.hypot(dx, dy)
    }

    fun union(other: Box) = Box(
        min(left, other.left), min(top, other.top), max(right, other.right), max(bottom, other.bottom),
    )

    companion object {
        fun ofPoints(points: List<Vec2>) = Box(
            points.minOf { it.x }, points.minOf { it.y }, points.maxOf { it.x }, points.maxOf { it.y },
        )
    }
}

/** Size of an image after turning it clockwise by [degrees] (a multiple of 90). */
fun rotatedSize(width: Float, height: Float, degrees: Int): Vec2 =
    if (normalize(degrees) % 180 == 0) Vec2(width, height) else Vec2(height, width)

/**
 * Maps a point in an image of [width] x [height] to the same point in that image turned
 * clockwise by [degrees], which is how CameraX's `rotationDegrees` and ML Kit relate.
 */
fun rotatePoint(p: Vec2, width: Float, height: Float, degrees: Int): Vec2 = when (normalize(degrees)) {
    0 -> p
    90 -> Vec2(height - p.y, p.x)
    180 -> Vec2(width - p.x, height - p.y)
    270 -> Vec2(p.y, width - p.x)
    else -> error("Rotation must be a multiple of 90: $degrees")
}

fun rotateBox(box: Box, width: Float, height: Float, degrees: Int): Box = Box.ofPoints(
    listOf(Vec2(box.left, box.top), Vec2(box.right, box.bottom)).map { rotatePoint(it, width, height, degrees) },
)

private fun normalize(degrees: Int) = ((degrees % 360) + 360) % 360

enum class ScaleMode { Fit, Fill }

/**
 * Maps between image coordinates and view coordinates when the [visible] part of an image is
 * centred in a view of [viewWidth] x [viewHeight], scaled to fit inside it or to fill it.
 */
class ImageToScreen(
    val visible: Box,
    val viewWidth: Float,
    val viewHeight: Float,
    mode: ScaleMode,
) {
    val scale: Float = when (mode) {
        ScaleMode.Fit -> min(viewWidth / visible.width, viewHeight / visible.height)
        ScaleMode.Fill -> max(viewWidth / visible.width, viewHeight / visible.height)
    }
    private val offsetX = (viewWidth - visible.width * scale) / 2 - visible.left * scale
    private val offsetY = (viewHeight - visible.height * scale) / 2 - visible.top * scale

    fun toView(p: Vec2) = Vec2(p.x * scale + offsetX, p.y * scale + offsetY)

    fun toView(b: Box) = Box(
        b.left * scale + offsetX, b.top * scale + offsetY, b.right * scale + offsetX, b.bottom * scale + offsetY,
    )

    fun toImage(p: Vec2) = Vec2((p.x - offsetX) / scale, (p.y - offsetY) / scale)
}

/**
 * Clips a [src] rectangle to an image of [width] x [height] and shrinks [dst] in proportion, so
 * drawing part of an image never reads outside it. Null when nothing of [src] is inside.
 */
fun clipDraw(src: Box, dst: Box, width: Float, height: Float): Pair<Box, Box>? {
    val clipped = Box(max(src.left, 0f), max(src.top, 0f), min(src.right, width), min(src.bottom, height))
    if (clipped.width <= 0f || clipped.height <= 0f) return null
    val sx = dst.width / src.width
    val sy = dst.height / src.height
    return clipped to Box(
        dst.left + (clipped.left - src.left) * sx,
        dst.top + (clipped.top - src.top) * sy,
        dst.right - (src.right - clipped.right) * sx,
        dst.bottom - (src.bottom - clipped.bottom) * sy,
    )
}
