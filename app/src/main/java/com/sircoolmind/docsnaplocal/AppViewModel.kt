package com.sircoolmind.docsnaplocal

import androidx.lifecycle.ViewModel
import com.sircoolmind.docsnaplocal.ocr.TextReader

class AppViewModel : ViewModel() {
    val reader = TextReader()

    override fun onCleared() {
        reader.close()
    }
}
