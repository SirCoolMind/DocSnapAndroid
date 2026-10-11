package com.sircoolmind.docsnaplocal.ocr

import kotlin.math.abs

/** Lines tilted further than this are not counted: they were read across, not along, the text. */
private const val MAX_LEVEL_ANGLE = 30f

/**
 * How well a page reads: letters and digits in level lines, weighted by ML Kit's confidence.
 * Upright text gives more, and more confident, words than the same text read sideways.
 */
fun uprightScore(page: OcrPage): Float =
    page.blocks.flatMap { it.lines }
        .filter { abs(it.angle) <= MAX_LEVEL_ANGLE }
        .flatMap { it.words }
        .sumOf { word -> (word.text.count(Char::isLetterOrDigit) * (0.25f + word.confidence)).toDouble() }
        .toFloat()

/** The clockwise turn with the best score; on a tie the smaller turn wins, so 0 is kept when unsure. */
fun bestRotation(scores: Map<Int, Float>): Int =
    scores.entries.sortedBy { it.key }.maxBy { it.value }.key
