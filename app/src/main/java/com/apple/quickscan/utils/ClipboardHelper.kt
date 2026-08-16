package com.apple.quickscan.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context

object ClipboardHelper {
    fun copyToClipboard(context: Context, text: String, label: String = "QuickScan"): Boolean {
        return try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val clip = ClipData.newPlainText(label, text)
            clipboard?.setPrimaryClip(clip)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
