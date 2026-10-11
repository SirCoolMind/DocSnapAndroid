package com.sircoolmind.docsnaplocal.ui

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.sircoolmind.docsnaplocal.R
import com.sircoolmind.docsnaplocal.geometry.Box
import com.sircoolmind.docsnaplocal.geometry.ImageToScreen
import com.sircoolmind.docsnaplocal.geometry.ScaleMode
import com.sircoolmind.docsnaplocal.geometry.Vec2
import com.sircoolmind.docsnaplocal.geometry.clipDraw
import com.sircoolmind.docsnaplocal.scan.clampCorner
import com.sircoolmind.docsnaplocal.scan.defaultCorners
import kotlin.math.roundToInt

/** How much more the magnifier enlarges than the photo on screen. */
private const val LOUPE_ZOOM = 3f

/**
 * Shows the Corners on the photo; any corner can be dragged before Flatten. [found] is false
 * when no page outline was detected and the corners start at the photo's edges.
 */
@Composable
fun CornersScreen(
    photo: Bitmap,
    initialCorners: List<Vec2>,
    found: Boolean,
    onConfirm: (List<Vec2>) -> Unit,
    onRetake: () -> Unit,
) {
    val image = remember(photo) { photo.asImageBitmap() }
    val width = photo.width.toFloat()
    val height = photo.height.toFloat()
    var corners by remember(initialCorners) { mutableStateOf(initialCorners) }
    var dragging by remember { mutableStateOf<Int?>(null) }
    var viewSize by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current
    val grabRadius = with(density) { 48.dp.toPx() }
    val handleRadius = with(density) { 12.dp.toPx() }
    val loupeRadius = with(density) { 64.dp.toPx() }

    BackHandler { onRetake() }

    Column(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Text(
            stringResource(if (found) R.string.corners_hint else R.string.corners_not_found),
            color = Color.White,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(12.dp),
        )
        // Padding keeps handles on the image's edges reachable.
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(24.dp)
                .onSizeChanged { viewSize = it },
        ) {
            Image(image, contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
            if (viewSize != IntSize.Zero) {
                val transform = remember(viewSize, photo) {
                    ImageToScreen(Box(0f, 0f, width, height), viewSize.width.toFloat(), viewSize.height.toFloat(), ScaleMode.Fit)
                }
                Canvas(
                    Modifier
                        .fillMaxSize()
                        .pointerInput(transform) {
                            detectDragGestures(
                                onDragStart = { p ->
                                    dragging = corners.indices
                                        .map { it to (transform.toView(corners[it]) - Vec2(p.x, p.y)).length() }
                                        .filter { it.second <= grabRadius }
                                        .minByOrNull { it.second }
                                        ?.first
                                },
                                onDragEnd = { dragging = null },
                                onDragCancel = { dragging = null },
                                onDrag = { change, amount ->
                                    val i = dragging ?: return@detectDragGestures
                                    change.consume()
                                    val moved = corners[i] + Vec2(amount.x / transform.scale, amount.y / transform.scale)
                                    corners = corners.toMutableList().also { it[i] = clampCorner(moved, width, height) }
                                },
                            )
                        },
                ) {
                    val points = corners.map { transform.toView(it) }.map { Offset(it.x, it.y) }
                    val outline = Path().apply {
                        moveTo(points[0].x, points[0].y)
                        points.drop(1).forEach { lineTo(it.x, it.y) }
                        close()
                    }
                    drawPath(outline, BoxFill)
                    drawPath(outline, BoxStroke, style = Stroke(width = 4f))
                    points.forEachIndexed { i, p ->
                        if (i == dragging) return@forEachIndexed
                        drawCircle(Color.White, handleRadius, p)
                        drawCircle(BoxStroke, handleRadius, p, style = Stroke(width = 4f))
                    }
                    dragging?.let { drawLoupe(image, corners[it], points[it], transform.scale * LOUPE_ZOOM, loupeRadius) }
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onRetake) { Text(stringResource(R.string.retake)) }
            TextButton(onClick = { corners = defaultCorners(width, height) }) { Text(stringResource(R.string.reset_corners)) }
            Button(onClick = { onConfirm(corners) }) { Text(stringResource(R.string.read_text)) }
        }
    }
}

/**
 * A round magnifier showing the photo around the dragged [corner], placed in the top corner
 * away from the finger at [cornerOnScreen], with a crosshair on the corner itself.
 */
private fun DrawScope.drawLoupe(image: ImageBitmap, corner: Vec2, cornerOnScreen: Offset, scale: Float, radius: Float) {
    val margin = radius / 4
    val centerX = if (cornerOnScreen.x < size.width / 2) size.width - radius - margin else radius + margin
    val center = Offset(centerX, radius + margin)
    val half = radius / scale
    val src = Box(corner.x - half, corner.y - half, corner.x + half, corner.y + half)
    val dst = Box(center.x - radius, center.y - radius, center.x + radius, center.y + radius)
    val circle = Path().apply { addOval(androidx.compose.ui.geometry.Rect(center, radius)) }

    clipPath(circle) {
        drawRect(Color.Black, Offset(dst.left, dst.top), androidx.compose.ui.geometry.Size(dst.width, dst.height))
        clipDraw(src, dst, image.width.toFloat(), image.height.toFloat())?.let { (s, d) ->
            drawImage(
                image,
                srcOffset = IntOffset(s.left.roundToInt(), s.top.roundToInt()),
                srcSize = IntSize(s.width.roundToInt().coerceAtLeast(1), s.height.roundToInt().coerceAtLeast(1)),
                dstOffset = IntOffset(d.left.roundToInt(), d.top.roundToInt()),
                dstSize = IntSize(d.width.roundToInt().coerceAtLeast(1), d.height.roundToInt().coerceAtLeast(1)),
            )
        }
    }
    val arm = radius / 4
    drawLine(BoxStroke, center - Offset(arm, 0f), center + Offset(arm, 0f), strokeWidth = 3f)
    drawLine(BoxStroke, center - Offset(0f, arm), center + Offset(0f, arm), strokeWidth = 3f)
    drawCircle(Color.White, radius, center, style = Stroke(width = 4f))
}
