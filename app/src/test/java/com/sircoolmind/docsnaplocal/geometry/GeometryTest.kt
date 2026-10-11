package com.sircoolmind.docsnaplocal.geometry

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GeometryTest {

    private fun assertVec(expected: Vec2, actual: Vec2) {
        assertEquals("x", expected.x, actual.x, 0.01f)
        assertEquals("y", expected.y, actual.y, 0.01f)
    }

    private fun assertBox(expected: Box, actual: Box) {
        assertEquals("left", expected.left, actual.left, 0.01f)
        assertEquals("top", expected.top, actual.top, 0.01f)
        assertEquals("right", expected.right, actual.right, 0.01f)
        assertEquals("bottom", expected.bottom, actual.bottom, 0.01f)
    }

    @Test
    fun rotatedSizeSwapsSidesForQuarterTurns() {
        assertVec(Vec2(640f, 480f), rotatedSize(640f, 480f, 0))
        assertVec(Vec2(480f, 640f), rotatedSize(640f, 480f, 90))
        assertVec(Vec2(640f, 480f), rotatedSize(640f, 480f, 180))
        assertVec(Vec2(480f, 640f), rotatedSize(640f, 480f, 270))
        assertVec(Vec2(480f, 640f), rotatedSize(640f, 480f, -90))
    }

    @Test
    fun rotatePointTurnsClockwise() {
        // A landscape sensor frame 640 x 480, phone held upright: rotation 90.
        // The sensor's top-left corner ends up at the top-right of the upright image.
        assertVec(Vec2(480f, 0f), rotatePoint(Vec2(0f, 0f), 640f, 480f, 90))
        assertVec(Vec2(0f, 640f), rotatePoint(Vec2(640f, 480f), 640f, 480f, 90))
        assertVec(Vec2(630f, 470f), rotatePoint(Vec2(10f, 10f), 640f, 480f, 180))
        assertVec(Vec2(10f, 630f), rotatePoint(Vec2(10f, 10f), 640f, 480f, 270))
        assertVec(Vec2(10f, 20f), rotatePoint(Vec2(10f, 20f), 640f, 480f, 360))
    }

    @Test
    fun rotateBoxKeepsBoxNormalised() {
        val box = Box(100f, 50f, 200f, 80f)
        assertBox(Box(400f, 100f, 430f, 200f), rotateBox(box, 640f, 480f, 90))
        assertBox(Box(440f, 400f, 540f, 430f), rotateBox(box, 640f, 480f, 180))
        assertBox(Box(50f, 440f, 80f, 540f), rotateBox(box, 640f, 480f, 270))
    }

    @Test
    fun fitCentresImageWithBarsTopAndBottom() {
        // 3:4 image in a tall 1080 x 2000 view: scale 1080/960, bars above and below.
        val t = ImageToScreen(Box(0f, 0f, 960f, 1280f), 1080f, 2000f, ScaleMode.Fit)
        assertEquals(1.125f, t.scale, 0.0001f)
        assertVec(Vec2(0f, 280f), t.toView(Vec2(0f, 0f)))
        assertVec(Vec2(1080f, 1720f), t.toView(Vec2(960f, 1280f)))
        assertVec(Vec2(540f, 1000f), t.toView(Vec2(480f, 640f)))
    }

    @Test
    fun fillCropsTheLongSide() {
        // 4:3 image filling a square view: scaled to the height, sides cropped.
        val t = ImageToScreen(Box(0f, 0f, 400f, 300f), 600f, 600f, ScaleMode.Fill)
        assertEquals(2f, t.scale, 0.0001f)
        assertVec(Vec2(-100f, 0f), t.toView(Vec2(0f, 0f)))
        assertVec(Vec2(300f, 300f), t.toView(Vec2(200f, 150f)))
    }

    @Test
    fun visibleRegionOffsetIsRespected() {
        // Only x 100..300 of the image is shown (a crop rect); it fills the 400 px wide view.
        val t = ImageToScreen(Box(100f, 0f, 300f, 200f), 400f, 400f, ScaleMode.Fit)
        assertVec(Vec2(0f, 0f), t.toView(Vec2(100f, 0f)))
        assertVec(Vec2(400f, 400f), t.toView(Vec2(300f, 200f)))
        assertBox(Box(200f, 0f, 400f, 100f), t.toView(Box(200f, 0f, 300f, 50f)))
    }

    @Test
    fun toImageInvertsToView() {
        val t = ImageToScreen(Box(20f, 40f, 980f, 1320f), 1080f, 2000f, ScaleMode.Fit)
        val p = Vec2(123f, 456f)
        assertVec(p, t.toImage(t.toView(p)))
    }

    @Test
    fun sensorFrameBoxLandsWhereThePreviewShowsIt() {
        // Whole pipeline: a box ML Kit reports in the upright frame of a rotated sensor image,
        // drawn over a fit-centred viewfinder.
        val sensorW = 1280f
        val sensorH = 960f
        val visible = rotateBox(Box(0f, 0f, sensorW, sensorH), sensorW, sensorH, 90)
        assertBox(Box(0f, 0f, 960f, 1280f), visible)
        val t = ImageToScreen(visible, 1080f, 2000f, ScaleMode.Fit)
        assertBox(Box(108f, 392.5f, 216f, 448.75f), t.toView(Box(96f, 100f, 192f, 150f)))
    }

    @Test
    fun boxDistanceIsZeroInsideAndEuclideanOutside() {
        val b = Box(0f, 0f, 10f, 10f)
        assertEquals(0f, b.distanceTo(Vec2(5f, 5f)), 0f)
        assertEquals(5f, b.distanceTo(Vec2(15f, 5f)), 0.001f)
        assertEquals(5f, b.distanceTo(Vec2(13f, 14f)), 0.001f)
    }

    @Test
    fun clipDrawShrinksDestinationWithSource() {
        // A magnifier on the image's top-left corner: half the source is outside the image.
        val (src, dst) = clipDraw(Box(-10f, -10f, 10f, 10f), Box(0f, 0f, 100f, 100f), 50f, 50f)!!
        assertBox(Box(0f, 0f, 10f, 10f), src)
        assertBox(Box(50f, 50f, 100f, 100f), dst)
    }

    @Test
    fun clipDrawKeepsInsideSourceAsIs() {
        val (src, dst) = clipDraw(Box(10f, 10f, 20f, 20f), Box(0f, 0f, 40f, 40f), 50f, 50f)!!
        assertBox(Box(10f, 10f, 20f, 20f), src)
        assertBox(Box(0f, 0f, 40f, 40f), dst)
    }

    @Test
    fun clipDrawOutsideImageIsNull() {
        assertNull(clipDraw(Box(60f, 0f, 80f, 20f), Box(0f, 0f, 40f, 40f), 50f, 50f))
    }
}
