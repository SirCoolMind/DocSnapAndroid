package com.sircoolmind.docsnaplocal.ocr

import org.junit.Assert.assertEquals
import org.junit.Test

class FrameThrottleTest {
    @Test
    fun firstFramePassesThenOnePerInterval() {
        val throttle = FrameThrottle(200)
        val passed = listOf(1000L, 1033, 1066, 1199, 1200, 1233, 1400, 1401).filter { throttle.tryAcquire(it) }
        assertEquals(listOf(1000L, 1200, 1400), passed)
    }

    @Test
    fun aLongGapPassesAtOnce() {
        val throttle = FrameThrottle(200)
        throttle.tryAcquire(0)
        assertEquals(true, throttle.tryAcquire(60_000))
    }
}
