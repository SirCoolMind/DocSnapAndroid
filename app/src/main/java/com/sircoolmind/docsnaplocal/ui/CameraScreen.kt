package com.sircoolmind.docsnaplocal.ui

import android.content.Context
import android.util.Size
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.TorchState
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.sircoolmind.docsnaplocal.R
import com.sircoolmind.docsnaplocal.ocr.LiveAnalyzer
import com.sircoolmind.docsnaplocal.ocr.LiveFrame
import com.sircoolmind.docsnaplocal.ocr.TextReader
import kotlinx.coroutines.delay
import java.util.concurrent.Executors

enum class CaptureMode { Scan, Live }

@Composable
fun CameraScreen(reader: TextReader) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val controller = remember { createController(context) }
    val previewView = remember {
        PreviewView(context).apply {
            // Show the whole frame that will be read, like the stock camera's 4:3 view.
            scaleType = PreviewView.ScaleType.FIT_CENTER
            this.controller = controller
        }
    }
    DisposableEffect(lifecycleOwner) {
        controller.bindToLifecycle(lifecycleOwner)
        onDispose { controller.unbind() }
    }

    var mode by remember { mutableStateOf(CaptureMode.Scan) }
    val torchState by controller.torchState.observeAsState(TorchState.OFF)
    var focusPoint by remember { mutableStateOf<Offset?>(null) }
    var liveFrame by remember { mutableStateOf<LiveFrame?>(null) }

    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    DisposableEffect(Unit) { onDispose { analysisExecutor.shutdown() } }
    LaunchedEffect(mode) {
        if (mode == CaptureMode.Live) {
            controller.setImageAnalysisAnalyzer(analysisExecutor, LiveAnalyzer(reader) { liveFrame = it })
        } else {
            controller.clearImageAnalysisAnalyzer()
            liveFrame = null
        }
    }

    LaunchedEffect(focusPoint) {
        if (focusPoint != null) {
            delay(700)
            focusPoint = null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
            if (mode == CaptureMode.Live) LiveOverlay(liveFrame, Modifier.fillMaxSize())
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, _, zoomChange, _ ->
                            controller.zoomBy(zoomChange)
                        }
                    }
                    .pointerInput(mode) {
                        detectTapGestures { offset ->
                            // Live mode keeps continuous autofocus: a tap there means Freeze.
                            if (mode == CaptureMode.Scan) {
                                val point = previewView.meteringPointFactory.createPoint(offset.x, offset.y)
                                controller.cameraControl?.startFocusAndMetering(
                                    FocusMeteringAction.Builder(point).build(),
                                )
                                focusPoint = offset
                            }
                        }
                    },
            )
            focusPoint?.let { FocusRing(it) }
        }

        ModeSwitch(mode = mode, onModeChange = { mode = it })

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(48.dp))
            ShutterButton(onClick = {})
            IconButton(onClick = { controller.enableTorch(torchState != TorchState.ON) }) {
                Icon(
                    painter = painterResource(R.drawable.ic_flash),
                    contentDescription = stringResource(R.string.flashlight),
                    tint = if (torchState == TorchState.ON) Color(0xFFFFD54F) else Color.White,
                )
            }
        }
    }
}

private fun createController(context: Context) = LifecycleCameraController(context).apply {
    // Gestures are handled in Compose so that a tap can mean Freeze in Live mode.
    isTapToFocusEnabled = false
    isPinchToZoomEnabled = false
    setEnabledUseCases(CameraController.IMAGE_CAPTURE or CameraController.IMAGE_ANALYSIS)
    val fourByThree = AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY
    previewResolutionSelector = ResolutionSelector.Builder()
        .setAspectRatioStrategy(fourByThree)
        .build()
    imageCaptureResolutionSelector = ResolutionSelector.Builder()
        .setAspectRatioStrategy(fourByThree)
        .build()
    imageAnalysisResolutionSelector = ResolutionSelector.Builder()
        .setAspectRatioStrategy(fourByThree)
        .setResolutionStrategy(
            ResolutionStrategy(Size(1280, 960), ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER),
        )
        .build()
    imageAnalysisBackpressureStrategy = ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
    imageCaptureMode = androidx.camera.core.ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY
}

private fun CameraController.zoomBy(factor: Float) {
    val zoom = zoomState.value ?: return
    setZoomRatio((zoom.zoomRatio * factor).coerceIn(zoom.minZoomRatio, zoom.maxZoomRatio))
}

@Composable
private fun FocusRing(center: Offset) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    val radius = 32.dp
    val radiusPx = with(density) { radius.toPx() }
    Box(
        modifier = Modifier
            .padding(
                start = with(density) { (center.x - radiusPx).coerceAtLeast(0f).toDp() },
                top = with(density) { (center.y - radiusPx).coerceAtLeast(0f).toDp() },
            )
            .size(radius * 2)
            .border(2.dp, Color.White, CircleShape),
    )
}

@Composable
private fun ModeSwitch(mode: CaptureMode, onModeChange: (CaptureMode) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        CaptureMode.entries.forEach { entry ->
            val selected = entry == mode
            TextButton(onClick = { onModeChange(entry) }) {
                Text(
                    text = stringResource(if (entry == CaptureMode.Scan) R.string.mode_scan else R.string.mode_live),
                    color = if (selected) Color(0xFFFFD54F) else Color.White,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}

@Composable
fun ShutterButton(onClick: () -> Unit, enabled: Boolean = true) {
    val label = stringResource(R.string.shutter)
    Box(
        modifier = Modifier
            .size(72.dp)
            .border(4.dp, Color.White, CircleShape)
            .padding(8.dp)
            .clip(CircleShape)
            .background(if (enabled) Color.White else Color.Gray)
            .semantics { contentDescription = label }
            .pointerInput(enabled) { if (enabled) detectTapGestures { onClick() } },
    )
}
