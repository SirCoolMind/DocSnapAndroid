package com.sircoolmind.docsnaplocal.selection

import com.sircoolmind.docsnaplocal.geometry.Vec2
import com.sircoolmind.docsnaplocal.ocr.OcrPage
import kotlin.math.max
import kotlin.math.min

/** A run of words in reading order, from word index [first] to [last] inclusive. */
data class Selection(val first: Int, val last: Int) {
    init {
        require(first in 0..last) { "Bad selection $first..$last" }
    }

    operator fun contains(index: Int) = index in first..last

    companion object {
        /** The run between two words, whichever way the finger went. */
        fun between(a: Int, b: Int) = Selection(min(a, b), max(a, b))
    }
}

/** The word under [p], counting a margin of [slop] around each word; the closest if several. */
fun OcrPage.wordAt(p: Vec2, slop: Float = 0f): Int? =
    words.filter { p in it.word.box.inflate(slop) }
        .minByOrNull { (it.word.box.center - p).length() }
        ?.index

/**
 * The word a dragging finger at [p] points to. Prefers words on the text line level with the
 * finger, so dragging past the end of a line selects up to its last word.
 */
fun OcrPage.nearestWord(p: Vec2): Int? {
    if (words.isEmpty()) return null
    val level = words.filter { p.y >= it.word.box.top && p.y <= it.word.box.bottom }
    return level.ifEmpty { words }.minBy { it.word.box.distanceTo(p) }.index
}

fun OcrPage.lineSelection(index: Int): Selection {
    val line = words[index].line
    return words.filter { it.line == line }.let { Selection(it.first().index, it.last().index) }
}

fun OcrPage.blockSelection(index: Int): Selection {
    val block = words[index].block
    return words.filter { it.block == block }.let { Selection(it.first().index, it.last().index) }
}

fun OcrPage.selectAll(): Selection? = if (words.isEmpty()) null else Selection(0, words.lastIndex)

/** Selected words joined by spaces within a line, newlines between lines, a blank line between blocks. */
fun OcrPage.textOf(selection: Selection): String = buildString {
    val selected = words.subList(selection.first, selection.last + 1)
    selected.forEachIndexed { i, ref ->
        if (i > 0) {
            val prev = selected[i - 1]
            append(
                when {
                    prev.block != ref.block -> "\n\n"
                    prev.line != ref.line -> "\n"
                    else -> " "
                },
            )
        }
        append(ref.word.text)
    }
}

fun OcrPage.fullText(): String = selectAll()?.let { textOf(it) } ?: ""

/**
 * Carries a selection over to a new reading of the same scene (the full-res re-read after a
 * Freeze). The selected words must appear as the same run in [to]; if the run appears more
 * than once, the same occurrence is kept only when both readings have the same number of
 * occurrences. Otherwise the mapping is unclear and the result is null.
 */
fun remapSelection(from: OcrPage, selection: Selection, to: OcrPage): Selection? {
    val run = from.words.subList(selection.first, selection.last + 1).map { it.word.text }
    val inFrom = from.occurrencesOf(run)
    val inTo = to.occurrencesOf(run)
    val start = when {
        inTo.size == 1 -> inTo.single()
        inTo.isNotEmpty() && inTo.size == inFrom.size -> inTo[inFrom.indexOf(selection.first)]
        else -> return null
    }
    return Selection(start, start + run.size - 1)
}

private fun OcrPage.occurrencesOf(run: List<String>): List<Int> {
    val texts = words.map { it.word.text }
    return (0..texts.size - run.size).filter { start -> run.indices.all { texts[start + it] == run[it] } }
}
