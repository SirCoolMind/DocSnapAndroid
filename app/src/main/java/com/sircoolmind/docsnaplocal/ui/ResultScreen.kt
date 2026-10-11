package com.sircoolmind.docsnaplocal.ui

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sircoolmind.docsnaplocal.R
import com.sircoolmind.docsnaplocal.ocr.OcrPage
import com.sircoolmind.docsnaplocal.selection.Selection
import com.sircoolmind.docsnaplocal.selection.selectAll
import com.sircoolmind.docsnaplocal.selection.textOf

/** The flattened page with its OCR result. Leaving it drops the images. */
@Composable
fun ResultScreen(image: Bitmap, page: OcrPage, onBack: () -> Unit) {
    val bitmap = remember(image) { image.asImageBitmap() }
    var selection by remember(page) { mutableStateOf<Selection?>(null) }

    BackHandler { onBack() }

    Column(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        IconButton(onClick = onBack) {
            Icon(painterResource(R.drawable.ic_back), stringResource(R.string.back), tint = Color.White)
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            OcrImage(bitmap, page, selection, { selection = it }, Modifier.fillMaxSize().padding(8.dp))
            selection?.let { sel ->
                SelectionBar(
                    selectedText = page.textOf(sel),
                    onSelectAll = { selection = page.selectAll() },
                    modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                )
            }
        }
    }
}
