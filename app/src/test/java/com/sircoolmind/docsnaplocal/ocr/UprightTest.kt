package com.sircoolmind.docsnaplocal.ocr

import com.sircoolmind.docsnaplocal.geometry.Box
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UprightTest {
    private val box = Box(0f, 0f, 10f, 10f)

    private fun page(vararg lines: Pair<List<Pair<String, Float>>, Float>) = OcrPage(
        100f,
        100f,
        listOf(OcrBlock(lines.map { (words, angle) -> OcrLine(words.map { OcrWord(it.first, box, it.second) }, box, angle) }, box)),
    )

    @Test
    fun clearLevelTextBeatsSideways() {
        val upright = page(listOf("Invoice" to 0.95f, "number" to 0.9f) to 0f)
        val sideways = page(listOf("lo" to 0.3f, "ν" to 0.2f) to 2f)
        assertTrue(uprightScore(upright) > uprightScore(sideways))
    }

    @Test
    fun linesReadAcrossTheTextAreIgnored() {
        assertEquals(0f, uprightScore(page(listOf("Invoice" to 0.95f) to 88f)), 0f)
        assertEquals(0f, uprightScore(page(listOf("Invoice" to 0.95f) to -91f)), 0f)
    }

    @Test
    fun punctuationDoesNotScore() {
        assertEquals(0f, uprightScore(page(listOf("--" to 0.9f, "|" to 0.9f) to 0f)), 0f)
    }

    @Test
    fun emptyPageScoresZero() {
        assertEquals(0f, uprightScore(OcrPage.Empty), 0f)
    }

    @Test
    fun bestRotationPicksHighestScore() {
        assertEquals(90, bestRotation(mapOf(0 to 3f, 90 to 40f, 180 to 2f, 270 to 5f)))
        assertEquals(180, bestRotation(mapOf(180 to 50f, 0 to 10f, 270 to 1f, 90 to 1f)))
    }

    @Test
    fun tiesKeepTheSmallestTurn() {
        assertEquals(0, bestRotation(mapOf(270 to 0f, 90 to 0f, 0 to 0f, 180 to 0f)))
        assertEquals(90, bestRotation(mapOf(270 to 7f, 90 to 7f, 0 to 1f)))
    }
}
