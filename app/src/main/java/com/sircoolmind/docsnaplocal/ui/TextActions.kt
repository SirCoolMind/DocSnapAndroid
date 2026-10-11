package com.sircoolmind.docsnaplocal.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sircoolmind.docsnaplocal.R

fun copyText(context: Context, text: String) {
    val clipboard = context.getSystemService(ClipboardManager::class.java)
    clipboard.setPrimaryClip(ClipData.newPlainText(context.getString(R.string.app_name), text))
    Toast.makeText(context, R.string.copied, Toast.LENGTH_SHORT).show()
}

fun shareText(context: Context, text: String) {
    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
    context.startActivity(Intent.createChooser(send, null))
}

/** The floating Copy / Select all / Share bar shown while words are selected. */
@Composable
fun SelectionBar(selectedText: String, onSelectAll: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 6.dp,
    ) {
        Row(Modifier.padding(horizontal = 8.dp)) {
            TextButton(onClick = { copyText(context, selectedText) }) { Text(stringResource(R.string.copy)) }
            TextButton(onClick = onSelectAll) { Text(stringResource(R.string.select_all)) }
            TextButton(onClick = { shareText(context, selectedText) }) { Text(stringResource(R.string.share)) }
        }
    }
}
