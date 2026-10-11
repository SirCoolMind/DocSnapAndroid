package com.sircoolmind.docsnaplocal.selection

import com.sircoolmind.docsnaplocal.geometry.Box
import com.sircoolmind.docsnaplocal.geometry.Vec2
import com.sircoolmind.docsnaplocal.ocr.OcrBlock
import com.sircoolmind.docsnaplocal.ocr.OcrLine
import com.sircoolmind.docsnaplocal.ocr.OcrPage
import com.sircoolmind.docsnaplocal.ocr.OcrWord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Builds a page from text: blocks split by blank lines, words laid out 100 px wide, lines 40 px apart. */
private fun page(text: String): OcrPage {
    var y = 0f
    val blocks = text.trimIndent().trim().split("\n\n").map { blockText ->
        val lines = blockText.lines().map { lineText ->
            val words = lineText.split(" ").mapIndexed { i, w ->
                OcrWord(w, Box(i * 100f, y, i * 100f + 80f, y + 30f), 0.9f)
            }
            val line = OcrLine(words, Box(0f, y, words.last().box.right, y + 30f))
            y += 40f
            line
        }
        y += 40f
        OcrBlock(lines, lines.map { it.box }.reduce(Box::union))
    }
    return OcrPage(1000f, y, blocks)
}

class SelectionTest {
    private val doc = page(
        """
        Invoice number 42
        Due 1 March

        Total RM 120
        """,
    )

    @Test
    fun wordsAreInReadingOrder() {
        assertEquals(
            listOf("Invoice", "number", "42", "Due", "1", "March", "Total", "RM", "120"),
            doc.words.map { it.word.text },
        )
        assertEquals(listOf(0, 0, 0, 1, 1, 1, 2, 2, 2), doc.words.map { it.line })
        assertEquals(listOf(0, 0, 0, 0, 0, 0, 1, 1, 1), doc.words.map { it.block })
    }

    @Test
    fun tapHitsTheWordUnderTheFinger() {
        assertEquals(1, doc.wordAt(Vec2(150f, 15f)))
        assertEquals(4, doc.wordAt(Vec2(110f, 50f)))
    }

    @Test
    fun tapBetweenWordsHitsOnlyWithSlop() {
        assertNull(doc.wordAt(Vec2(90f, 15f)))
        assertEquals(0, doc.wordAt(Vec2(85f, 15f), slop = 6f))
    }

    @Test
    fun tapOnEmptySpaceHitsNothing() {
        assertNull(doc.wordAt(Vec2(900f, 15f)))
    }

    @Test
    fun dragRangeIsOrderedEitherWay() {
        assertEquals(Selection(1, 4), Selection.between(1, 4))
        assertEquals(Selection(1, 4), Selection.between(4, 1))
    }

    @Test
    fun dragAcrossLinesSelectsEverythingBetween() {
        val start = doc.wordAt(Vec2(150f, 15f))!!
        val end = doc.nearestWord(Vec2(150f, 50f))!!
        val selection = Selection.between(start, end)
        assertEquals("number 42\nDue 1", doc.textOf(selection))
    }

    @Test
    fun dragPastLineEndPicksLastWordOfThatLine() {
        assertEquals(2, doc.nearestWord(Vec2(700f, 15f)))
    }

    @Test
    fun dragIntoGapBetweenLinesPicksNearestWord() {
        assertEquals(3, doc.nearestWord(Vec2(10f, 36f)))
    }

    @Test
    fun longPressSelectsTheLine() {
        assertEquals(Selection(3, 5), doc.lineSelection(4))
        assertEquals("Due 1 March", doc.textOf(doc.lineSelection(4)))
    }

    @Test
    fun doubleTapSelectsTheBlock() {
        assertEquals(Selection(0, 5), doc.blockSelection(4))
        assertEquals(Selection(6, 8), doc.blockSelection(7))
    }

    @Test
    fun selectAllAndFullText() {
        assertEquals(Selection(0, 8), doc.selectAll())
        assertEquals("Invoice number 42\nDue 1 March\n\nTotal RM 120", doc.fullText())
        assertNull(OcrPage.Empty.selectAll())
        assertEquals("", OcrPage.Empty.fullText())
    }

    @Test
    fun remapKeepsAUniqueRun() {
        val live = page("Invoice nunber 42\nDue 1 March")
        val fullRes = page("Invoice number 42\nDue 1 March\n\nTotal RM 120")
        // "Due 1" sits at indexes 3..4 in both, but the remap goes by words, not indexes.
        assertEquals(Selection(3, 4), remapSelection(live, Selection(3, 4), fullRes))
        val shifted = page("Header\n\nInvoice number 42\nDue 1 March")
        assertEquals(Selection(4, 5), remapSelection(live, Selection(3, 4), shifted))
    }

    @Test
    fun remapClearsWhenWordsChanged() {
        val live = page("Invoice nunber 42")
        val fullRes = page("Invoice number 42")
        assertNull(remapSelection(live, Selection(1, 1), fullRes))
    }

    @Test
    fun remapKeepsSameOccurrenceWhenCountsMatch() {
        val live = page("RM 10\nRM 20\nRM 30")
        val fullRes = page("RM 10\nRM 20\nRM 30")
        assertEquals(Selection(2, 2), remapSelection(live, Selection(2, 2), fullRes))
    }

    @Test
    fun remapClearsWhenOccurrenceIsAmbiguous() {
        val live = page("RM 10\nRM 20")
        val fullRes = page("RM 5\nRM 10\nRM 20")
        assertNull(remapSelection(live, Selection(2, 2), fullRes))
    }

    @Test
    fun snippetFlattensAndShortens() {
        assertEquals("Total RM 120", snippet("Total RM 120", 40))
        assertEquals("Invoice number 42 Due 1 March", snippet("Invoice number 42\nDue 1 March\n\n", 40))
        assertEquals("Invoice number…", snippet("Invoice number 42", 16))
        assertEquals("", snippet("  \n ", 10))
    }
}
