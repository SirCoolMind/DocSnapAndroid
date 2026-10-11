package com.sircoolmind.docsnaplocal.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.sircoolmind.docsnaplocal.R

private const val REPO_URL = "https://github.com/SirCoolMind/DocSnapAndroid"

/**
 * Name, version, tech stack and source link. The link is handed to the browser: DocSnap has no
 * internet permission and never loads it itself.
 */
@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val version = remember { context.versionName() }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } },
        title = { Text(stringResource(R.string.about_name)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.about_version, version), style = MaterialTheme.typography.labelLarge)
                Text(stringResource(R.string.about_stack))
                Text(stringResource(R.string.about_privacy))
                Column {
                    Text(
                        text = REPO_URL,
                        color = MaterialTheme.colorScheme.primary,
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier.clickable(role = Role.Button) { openInBrowser(context, REPO_URL) },
                    )
                    Text(stringResource(R.string.about_link_note), style = MaterialTheme.typography.bodySmall)
                }
            }
        },
    )
}

private fun Context.versionName(): String =
    packageManager.getPackageInfo(packageName, 0).versionName.orEmpty()

private fun openInBrowser(context: Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, R.string.no_browser, Toast.LENGTH_SHORT).show()
    }
}
