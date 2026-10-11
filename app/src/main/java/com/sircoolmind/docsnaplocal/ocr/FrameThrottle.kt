package com.sircoolmind.docsnaplocal.ocr

/** Lets a frame through at most once every [minIntervalMs]; the rest are skipped to save battery. */
class FrameThrottle(private val minIntervalMs: Long) {
    private var last: Long? = null

    fun tryAcquire(nowMs: Long): Boolean {
        val previous = last
        if (previous != null && nowMs - previous < minIntervalMs) return false
        last = nowMs
        return true
    }
}
