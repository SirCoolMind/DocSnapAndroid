package com.sircoolmind.docsnaplocal.image

import android.content.Context

/**
 * Deletes everything in the app's cache folder. The app keeps images in memory, but anything a
 * library leaves in the cache must not outlive a scan either.
 */
fun Context.clearCache() {
    cacheDir.listFiles()?.forEach { it.deleteRecursively() }
}
