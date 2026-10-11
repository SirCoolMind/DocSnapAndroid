package com.sircoolmind.docsnaplocal

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sircoolmind.docsnaplocal.ui.CameraPermissionGate
import com.sircoolmind.docsnaplocal.ui.CameraScreen
import com.sircoolmind.docsnaplocal.ui.CornersScreen
import com.sircoolmind.docsnaplocal.ui.ResultScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    DocSnapApp(viewModel())
                }
            }
        }
    }
}

@Composable
private fun DocSnapApp(model: AppViewModel) {
    val context = LocalContext.current
    LaunchedEffect(model.error) {
        model.error?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            model.clearError()
        }
    }

    when (val screen = model.screen) {
        Screen.Camera -> CameraPermissionGate {
            CameraScreen(reader = model.reader, onPhotoTaken = model::onPhotoTaken)
        }
        is Screen.Working -> WorkingScreen(screen.message, onCancel = model::backToCamera)
        is Screen.Corners -> CornersScreen(
            photo = screen.photo,
            initialCorners = screen.corners,
            onConfirm = { model.onCornersConfirmed(screen.photo, it) },
            onRetake = model::backToCamera,
        )
        is Screen.Result -> ResultScreen(screen.image, screen.page, onBack = model::backToCamera)
    }
}

@Composable
private fun WorkingScreen(message: Int, onCancel: () -> Unit) {
    BackHandler { onCancel() }
    Column(
        Modifier.fillMaxSize().background(Color.Black),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator(color = Color.White)
        Text(stringResource(message), color = Color.White)
    }
}
