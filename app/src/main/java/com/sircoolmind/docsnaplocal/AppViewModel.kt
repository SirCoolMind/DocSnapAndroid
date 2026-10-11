package com.sircoolmind.docsnaplocal

import android.content.ContentResolver
import android.graphics.Bitmap
import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sircoolmind.docsnaplocal.geometry.Vec2
import com.sircoolmind.docsnaplocal.image.decodeImport
import com.sircoolmind.docsnaplocal.image.rotate
import com.sircoolmind.docsnaplocal.image.scaleDown
import com.sircoolmind.docsnaplocal.ocr.OcrPage
import com.sircoolmind.docsnaplocal.ocr.TextReader
import com.sircoolmind.docsnaplocal.ocr.bestRotation
import com.sircoolmind.docsnaplocal.ocr.uprightScore
import com.sircoolmind.docsnaplocal.scan.DocumentScanner
import com.sircoolmind.docsnaplocal.scan.defaultCorners
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface Screen {
    data object Camera : Screen
    class Working(@param:StringRes val message: Int) : Screen
    class Corners(val photo: Bitmap, val corners: List<Vec2>, val found: Boolean) : Screen
    class Result(val image: Bitmap, val page: OcrPage) : Screen
}

private const val UPRIGHT_PROBE_SIDE = 1280

/** Holds the Scan flow. Images live only here, in memory, and are dropped on leaving it. */
class AppViewModel : ViewModel() {
    val reader = TextReader()

    var screen by mutableStateOf<Screen>(Screen.Camera)
        private set
    var error by mutableStateOf<Int?>(null)
        private set

    private var job: Job? = null

    fun onPhotoTaken(photo: Bitmap) = run(R.string.working_finding_page) { cornersFor(photo) }

    fun onImport(resolver: ContentResolver, uri: Uri) = run(R.string.working_finding_page) {
        cornersFor(withContext(Dispatchers.IO) { decodeImport(resolver, uri) })
    }

    fun onCornersConfirmed(photo: Bitmap, corners: List<Vec2>) = run(R.string.working_reading) {
        val flat = withContext(Dispatchers.Default) { DocumentScanner.flatten(photo, corners) }
        val upright = upright(flat)
        Screen.Result(upright, reader.read(upright))
    }

    private suspend fun cornersFor(photo: Bitmap): Screen {
        val corners = withContext(Dispatchers.Default) { DocumentScanner.findCorners(photo) }
        return Screen.Corners(
            photo,
            corners ?: defaultCorners(photo.width.toFloat(), photo.height.toFloat()),
            found = corners != null,
        )
    }

    /** Turns a sideways or upside-down page Upright: reads a small copy four ways, keeps the best. */
    private suspend fun upright(page: Bitmap): Bitmap {
        val probe = withContext(Dispatchers.Default) { scaleDown(page, UPRIGHT_PROBE_SIDE) }
        val scores = listOf(0, 90, 180, 270).associateWith { degrees ->
            val turned = withContext(Dispatchers.Default) { rotate(probe, degrees) }
            uprightScore(reader.read(turned))
        }
        val best = bestRotation(scores)
        return if (best == 0) page else withContext(Dispatchers.Default) { rotate(page, best) }
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
