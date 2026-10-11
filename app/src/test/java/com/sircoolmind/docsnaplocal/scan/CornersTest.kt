package com.sircoolmind.docsnaplocal.scan

import com.sircoolmind.docsnaplocal.geometry.Vec2
import org.junit.Assert.assertEquals
import org.junit.Test

class CornersTest {
    private val tl = Vec2(100f, 120f)
    private val tr = Vec2(900f, 80f)
    private val br = Vec2(950f, 1200f)
    private val bl = Vec2(60f, 1150f)

    @Test
    fun ordersCornersFromAnyStartingOrder() {
        val expected = listOf(tl, tr, br, bl)
        assertEquals(expected, orderCorners(listOf(br, tl, bl, tr)))
        assertEquals(expected, orderCorners(listOf(bl, br, tr, tl)))
        assertEquals(expected, orderCorners(expected))
    }

    @Test
    fun untanglesCornersDraggedPastEachOther() {
        // The user dragged the top-left handle to the right of the top-right one.
        val draggedTl = Vec2(950f, 60f)
        val draggedTr = Vec2(150f, 100f)
        assertEquals(
            listOf(draggedTr, draggedTl, br, bl),
            orderCorners(listOf(draggedTl, draggedTr, br, bl)),
        )
    }

    @Test
    fun ordersATiltedPage() {
        // A page turned about 30 degrees clockwise.
        val top = Vec2(400f, 50f)
        val right = Vec2(900f, 340f)
        val bottom = Vec2(600f, 860f)
        val left = Vec2(100f, 570f)
        assertEquals(listOf(top, right, bottom, left), orderCorners(listOf(bottom, left, top, right)))
    }

    @Test
    fun clampKeepsCornersInsideTheImage() {
        assertEquals(Vec2(0f, 0f), clampCorner(Vec2(-20f, -5f), 1000f, 800f))
        assertEquals(Vec2(1000f, 800f), clampCorner(Vec2(1200f, 900f), 1000f, 800f))
        assertEquals(Vec2(500f, 400f), clampCorner(Vec2(500f, 400f), 1000f, 800f))
    }

    @Test
    fun defaultCornersAreTheImageEdgesInOrder() {
        val corners = defaultCorners(1000f, 800f)
        assertEquals(listOf(Vec2(0f, 0f), Vec2(1000f, 0f), Vec2(1000f, 800f), Vec2(0f, 800f)), corners)
        assertEquals(corners, orderCorners(corners))
    }

    @Test
    fun flattenedSizeUsesLongerOppositeSides() {
        assertEquals(1000 to 800, flattenedSize(defaultCorners(1000f, 800f)))
        val trapezoid = listOf(Vec2(200f, 0f), Vec2(800f, 0f), Vec2(1000f, 800f), Vec2(0f, 800f))
        assertEquals(1000 to 825, flattenedSize(trapezoid))
    }
}
