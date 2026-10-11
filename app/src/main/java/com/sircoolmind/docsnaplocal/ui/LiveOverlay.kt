package com.sircoolmind.docsnaplocal.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import com.sircoolmind.docsnaplocal.geometry.ImageToScreen
import com.sircoolmind.docsnaplocal.geometry.ScaleMode
import com.sircoolmind.docsnaplocal.ocr.LiveFrame

val BoxStroke = Color(0xFF4FC3F7)
val BoxFill = Color(0x224FC3F7)

/** Boxes each text block of [frame] over a viewfinder that shows the frame fit-centred. */
@Composable
fun LiveOverlay(frame: LiveFrame?, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        if (frame == null) return@Canvas
        val transform = ImageToScreen(frame.visible, size.width, size.height, ScaleMode.Fit)
        frame.page.blocks.forEach { block ->
            val b = transform.toView(block.box)
            val topLeft = Offset(b.left, b.top)
            val boxSize = Size(b.width, b.height)
            val radius = CornerRadius(6f, 6f)
            drawRoundRect(BoxFill, topLeft, boxSize, radius)
            drawRoundRect(BoxStroke, topLeft, boxSize, radius, style = Stroke(width = 3f))
        }
    }
}
