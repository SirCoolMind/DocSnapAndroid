package com.sircoolmind.docsnaplocal.ui

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sircoolmind.docsnaplocal.R
import com.sircoolmind.docsnaplocal.ocr.OcrPage
import com.sircoolmind.docsnaplocal.selection.Selection
import com.sircoolmind.docsnaplocal.selection.fullText
import com.sircoolmind.docsnaplocal.selection.selectAll
import com.sircoolmind.docsnaplocal.selection.textOf

private enum class ResultView { Image, Text }

/**
 * The flattened page, shown as an image with overlay or as editable text. Copy all and Share
 * use the text as edited. Leaving the screen drops the images.
 */
@Composable
fun ResultScreen(image: Bitmap, page: OcrPage, onBack: () -> Unit) {
    val context = LocalContext.current
    val bitmap = remember(image) { image.asImageBitmap() }
    var view by rememberSaveable { mutableStateOf(ResultView.Image) }
    var text by rememberSaveable(page) { mutableStateOf(page.fullText()) }
    var selection by remember(page) { mutableStateOf<Selection?>(null) }

    BackHandler { onBack() }

    Column(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 16.dp)) {
            IconButton(onClick = onBack) {
                Icon(painterResource(R.drawable.ic_back), stringResource(R.string.back), tint = Color.White)
            }
            SingleChoiceSegmentedButtonRow(Modifier.weight(1f)) {
                ResultView.entries.forEachIndexed { i, entry ->
                    SegmentedButton(
                        selected = view == entry,
                        onClick = { view = entry },
                        shape = SegmentedButtonDefaults.itemShape(i, ResultView.entries.size),
                    ) {
                        Text(stringResource(if (entry == ResultView.Image) R.string.view_image else R.string.view_text))
                    }
                }
            }
        }

        Box(Modifier.weight(1f).fillMaxWidth()) {
            when (view) {
                ResultView.Image -> {
                    OcrImage(bitmap, page, selection, { selection = it }, Modifier.fillMaxSize().padding(8.dp))
                    selection?.let { sel ->
                        SelectionBar(
                            selectedText = page.textOf(sel),
                            onSelectAll = { selection = page.selectAll() },
                            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                        )
                    }
                }
                ResultView.Text -> OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.fillMaxSize().padding(12.dp),
                    textStyle = MaterialTheme.typography.bodyLarge,
                )
            }
            if (page.isEmpty) {
                Text(
                    stringResource(R.string.no_text_found),
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .background(Color(0xCC000000), MaterialTheme.shapes.medium)
                        .padding(16.dp),
                )
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(onClick = { shareText(context, text) }, enabled = text.isNotBlank(), modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.share_text))
            }
            Button(onClick = { copyText(context, text) }, enabled = text.isNotBlank(), modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.copy_all))
            }
        }
    }
}
