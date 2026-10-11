package com.sircoolmind.docsnaplocal

import android.graphics.Bitmap
import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sircoolmind.docsnaplocal.geometry.Vec2
import com.sircoolmind.docsnaplocal.ocr.OcrPage
import com.sircoolmind.docsnaplocal.ocr.TextReader
import com.sircoolmind.docsnaplocal.scan.DocumentScanner
import com.sircoolmind.docsnaplocal.scan.defaultCorners
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface Screen {
    data object Camera : Screen
    class Working(@param:StringRes val message: Int) : Screen
    class Corners(val photo: Bitmap, val corners: List<Vec2>) : Screen
    class Result(val image: Bitmap, val page: OcrPage) : Screen
}

/** Holds the Scan flow. Images live only here, in memory, and are dropped on leaving it. */
class AppViewModel : ViewModel() {
    val reader = TextReader()

    var screen by mutableStateOf<Screen>(Screen.Camera)
        private set
    var error by mutableStateOf<Int?>(null)
        private set

    private var job: Job? = null

    fun onPhotoTaken(photo: Bitmap) = run(R.string.working_finding_page) {
        val corners = withContext(Dispatchers.Default) { DocumentScanner.findCorners(photo) }
        Screen.Corners(photo, corners ?: defaultCorners(photo.width.toFloat(), photo.height.toFloat()))
    }

    fun onCornersConfirmed(photo: Bitmap, corners: List<Vec2>) = run(R.string.working_reading) {
        val page = withContext(Dispatchers.Default) { DocumentScanner.flatten(photo, corners) }
        Screen.Result(page, reader.read(page))
    }

    fun backToCamera() {
        job?.cancel()
        screen = Screen.Camera
    }

    fun clearError() {
        error = null
    }

    private fun run(@StringRes message: Int, step: suspend () -> Screen) {
        job?.cancel()
        screen = Screen.Working(message)
        job = viewModelScope.launch {
            screen = try {
                step()
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e("DocSnap", "Scan step failed", e)
                error = R.string.error_scan
                Screen.Camera
            }
        }
    }

    override fun onCleared() {
        reader.close()
    }
}
