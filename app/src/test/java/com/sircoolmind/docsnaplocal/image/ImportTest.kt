package com.sircoolmind.docsnaplocal.image

import org.junit.Assert.assertEquals
import org.junit.Test

class ImportTest {
    @Test
    fun smallPhotosAreDecodedAtFullSize() {
        assertEquals(1, sampleSizeFor(4000, 3000, 4000))
        assertEquals(1, sampleSizeFor(1200, 1600, 4000))
        assertEquals(1, sampleSizeFor(7999, 6000, 4000))
    }

    @Test
    fun bigPhotosAreReducedButKeepAtLeastTheLimit() {
        assertEquals(2, sampleSizeFor(8000, 6000, 4000))
        assertEquals(2, sampleSizeFor(6000, 12000, 4000))
        assertEquals(4, sampleSizeFor(16320, 12240, 4000))
    }
}
