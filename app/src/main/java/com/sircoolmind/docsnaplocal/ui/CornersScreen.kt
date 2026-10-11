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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.sircoolmind.docsnaplocal.R
import com.sircoolmind.docsnaplocal.geometry.Box
import com.sircoolmind.docsnaplocal.geometry.ImageToScreen
import com.sircoolmind.docsnaplocal.geometry.ScaleMode
import com.sircoolmind.docsnaplocal.geometry.Vec2
import com.sircoolmind.docsnaplocal.scan.clampCorner
import com.sircoolmind.docsnaplocal.scan.defaultCorners

/** Shows the found Corners on the photo; any corner can be dragged before Flatten. */
@Composable
fun CornersScreen(
    photo: Bitmap,
    initialCorners: List<Vec2>,
    onConfirm: (List<Vec2>) -> Unit,
    onRetake: () -> Unit,
) {
    val image = remember(photo) { photo.asImageBitmap() }
    val width = photo.width.toFloat()
    val height = photo.height.toFloat()
    var corners by remember(initialCorners) { mutableStateOf(initialCorners) }
    var viewSize by remember { mutableStateOf(IntSize.Zero) }
    val grabRadius = with(LocalDensity.current) { 48.dp.toPx() }
    val handleRadius = with(LocalDensity.current) { 12.dp.toPx() }

    BackHandler { onRetake() }

    Column(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Text(
            stringResource(R.string.corners_hint),
            color = Color.White,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(12.dp),
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
                            var dragging: Int? = null
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
                    points.forEach {
                        drawCircle(Color.White, handleRadius, it)
                        drawCircle(BoxStroke, handleRadius, it, style = Stroke(width = 4f))
                    }
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
