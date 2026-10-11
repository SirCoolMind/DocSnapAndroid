package com.sircoolmind.docsnaplocal.scan

import com.sircoolmind.docsnaplocal.geometry.Vec2
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Puts four document corners in the order top-left, top-right, bottom-right, bottom-left
 * (clockwise on screen). Works for any convex outline, and untangles one whose corners were
 * dragged past each other.
 */
fun orderCorners(points: List<Vec2>): List<Vec2> {
    require(points.size == 4) { "Need 4 corners, got ${points.size}" }
    val cx = points.sumOf { it.x.toDouble() } / 4
    val cy = points.sumOf { it.y.toDouble() } / 4
    // With y pointing down, increasing angle runs clockwise on screen.
    val clockwise = points.sortedBy { atan2(it.y - cy, it.x - cx) }
    val topLeft = clockwise.indices.minBy { clockwise[it].x + clockwise[it].y }
    return List(4) { clockwise[(topLeft + it) % 4] }
}

/** Keeps a dragged corner inside the image. */
fun clampCorner(p: Vec2, width: Float, height: Float) = Vec2(p.x.coerceIn(0f, width), p.y.coerceIn(0f, height))

/** The image's own edges, used when no document outline is found. */
fun defaultCorners(width: Float, height: Float) =
    listOf(Vec2(0f, 0f), Vec2(width, 0f), Vec2(width, height), Vec2(0f, height))

/** Pixel size of the flattened page for ordered [corners]: the longer of each pair of opposite sides. */
fun flattenedSize(corners: List<Vec2>): Pair<Int, Int> {
    val (tl, tr, br, bl) = corners
    val width = max((tr - tl).length(), (br - bl).length())
    val height = max((bl - tl).length(), (br - tr).length())
    return width.roundToInt().coerceAtLeast(1) to height.roundToInt().coerceAtLeast(1)
}
