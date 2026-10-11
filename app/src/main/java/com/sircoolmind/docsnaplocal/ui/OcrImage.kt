package com.sircoolmind.docsnaplocal.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.sircoolmind.docsnaplocal.geometry.Box
import com.sircoolmind.docsnaplocal.geometry.ImageToScreen
import com.sircoolmind.docsnaplocal.geometry.ScaleMode
import com.sircoolmind.docsnaplocal.geometry.Vec2
import com.sircoolmind.docsnaplocal.ocr.OcrPage
import com.sircoolmind.docsnaplocal.selection.Selection
import com.sircoolmind.docsnaplocal.selection.blockSelection
import com.sircoolmind.docsnaplocal.selection.lineSelection
import com.sircoolmind.docsnaplocal.selection.nearestWord
import com.sircoolmind.docsnaplocal.selection.wordAt

private val SelectedFill = Color(0x804FC3F7)

/**
 * An image with its OCR result drawn over it, fit-centred. Tap selects a word (or clears on
 * empty space), drag extends across words in reading order, long-press selects a line,
 * double-tap selects a block. [page] must be in the image's pixel coordinates.
 */
@Composable
fun OcrImage(
    image: ImageBitmap,
    page: OcrPage,
    selection: Selection?,
    onSelectionChange: (Selection?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var viewSize by remember { mutableStateOf(IntSize.Zero) }
    val transform = remember(viewSize, image) {
        if (viewSize == IntSize.Zero) {
            null
        } else {
            ImageToScreen(
                Box(0f, 0f, image.width.toFloat(), image.height.toFloat()),
                viewSize.width.toFloat(),
                viewSize.height.toFloat(),
                ScaleMode.Fit,
            )
        }
    }
    val onChange by rememberUpdatedState(onSelectionChange)
    val touchSlop = with(androidx.compose.ui.platform.LocalDensity.current) { 8.dp.toPx() }

    Box(modifier.onSizeChanged { viewSize = it }) {
        Image(image, contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
        if (transform == null) return@Box

        fun toImage(offset: Offset) = transform.toImage(Vec2(offset.x, offset.y))
        val slop = touchSlop / transform.scale

        Canvas(
            Modifier
                .fillMaxSize()
                .pointerInput(page, transform) {
                    detectTapGestures(
                        onTap = { p -> onChange(page.wordAt(toImage(p), slop)?.let { Selection(it, it) }) },
                        onLongPress = { p -> page.wordAt(toImage(p), slop)?.let { onChange(page.lineSelection(it)) } },
                        onDoubleTap = { p -> page.wordAt(toImage(p), slop)?.let { onChange(page.blockSelection(it)) } },
                    )
                }
                .pointerInput(page, transform) {
                    var anchor: Int? = null
                    detectDragGestures(
                        onDragStart = { p ->
                            anchor = page.nearestWord(toImage(p))
                            anchor?.let { onChange(Selection(it, it)) }
                        },
                        onDrag = { change, _ ->
                            val start = anchor
                            val end = page.nearestWord(toImage(change.position))
                            if (start != null && end != null) onChange(Selection.between(start, end))
                        },
                    )
                },
        ) {
            val radius = CornerRadius(4f, 4f)
            page.blocks.forEach { block ->
                val b = transform.toView(block.box)
                drawRoundRect(BoxStroke, Offset(b.left, b.top), Size(b.width, b.height), radius, style = Stroke(2f))
            }
            if (selection != null) {
                // One highlight per line, spanning its selected words.
                page.words.subList(selection.first, selection.last + 1)
                    .groupBy { it.line }
                    .values
                    .forEach { words ->
                        val b = transform.toView(words.map { it.word.box }.reduce(Box::union))
                        drawRoundRect(SelectedFill, Offset(b.left, b.top), Size(b.width, b.height), radius)
                    }
            }
        }
    }
}
