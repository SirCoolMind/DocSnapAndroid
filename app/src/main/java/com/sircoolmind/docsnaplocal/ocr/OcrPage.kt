package com.sircoolmind.docsnaplocal.ocr

import com.sircoolmind.docsnaplocal.geometry.Box

data class OcrWord(val text: String, val box: Box, val confidence: Float)

/** [angle] is the line's tilt in degrees as ML Kit reports it; 0 is level text. */
data class OcrLine(val words: List<OcrWord>, val box: Box, val angle: Float = 0f)

data class OcrBlock(val lines: List<OcrLine>, val box: Box)

/** A word with its place in reading order (block, then line, then word). */
data class WordRef(val index: Int, val block: Int, val line: Int, val word: OcrWord)

/** OCR result for one image, in that image's pixel coordinates. */
class OcrPage(val width: Float, val height: Float, val blocks: List<OcrBlock>) {

    /** All words in reading order. [WordRef.line] counts lines across the whole page. */
    val words: List<WordRef> = buildList {
        var lineIndex = 0
        blocks.forEachIndexed { b, block ->
            block.lines.forEach { line ->
                line.words.forEach { add(WordRef(size, b, lineIndex, it)) }
                lineIndex++
            }
        }
    }

    val isEmpty get() = words.isEmpty()

    /** The same page with every box passed through [map], for a new image of [width] x [height]. */
    fun mapBoxes(width: Float, height: Float, map: (Box) -> Box) = OcrPage(
        width,
        height,
        blocks.map { block ->
            OcrBlock(
                lines = block.lines.map { line ->
                    OcrLine(line.words.map { it.copy(box = map(it.box)) }, map(line.box), line.angle)
                },
                box = map(block.box),
            )
        },
    )

    companion object {
        val Empty = OcrPage(0f, 0f, emptyList())
    }
}
