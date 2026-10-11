package com.sircoolmind.docsnaplocal.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sircoolmind.docsnaplocal.R
import com.sircoolmind.docsnaplocal.selection.snippet

/** Longest copied text quoted in full in the confirmation toast. */
private const val TOAST_SNIPPET_CHARS = 80

/** Copies [text] and confirms with a toast that quotes what was copied. */
fun copyText(context: Context, text: String) {
    val clipboard = context.getSystemService(ClipboardManager::class.java)
    clipboard.setPrimaryClip(ClipData.newPlainText(context.getString(R.string.app_name), text))
    val message = context.getString(R.string.copied_text, snippet(text, TOAST_SNIPPET_CHARS))
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}

fun shareText(context: Context, text: String) {
    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
    context.startActivity(Intent.createChooser(send, null))
}

/**
 * The floating Selection bar shown while words are selected: the selected text, so the user
 * sees what Copy will take, above Copy / Select all / Share.
 */
@Composable
fun SelectionBar(selectedText: String, onSelectAll: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 6.dp,
    ) {
        Column(Modifier.padding(top = 12.dp, start = 16.dp, end = 8.dp)) {
            Text(
                text = selectedText,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(end = 8.dp),
            )
            Row(Modifier.align(Alignment.End)) {
                TextButton(onClick = { copyText(context, selectedText) }) { Text(stringResource(R.string.copy)) }
                TextButton(onClick = onSelectAll) { Text(stringResource(R.string.select_all)) }
                TextButton(onClick = { shareText(context, selectedText) }) { Text(stringResource(R.string.share)) }
            }
        }
    }
}
