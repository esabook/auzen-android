package com.esabook.auzen.extentions

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.ContextCompat


fun Context.copyToClipboard(text: CharSequence?){
    if (text.isNullOrBlank()) return
    val clipboard = ContextCompat.getSystemService(this, ClipboardManager::class.java)
    clipboard?.setPrimaryClip(ClipData.newPlainText("",text))
    Toast.makeText(this, "Tersalin", Toast.LENGTH_SHORT).show()
}

fun Context.shareTextToExternal(text: CharSequence?){
    if (text.isNullOrBlank()) return
    val intent = Intent(Intent.ACTION_SEND)
    intent.type = "text/plain"
    intent.putExtra(Intent.EXTRA_SUBJECT, text)
    intent.putExtra(Intent.EXTRA_TEXT, text)
    val chooser =  Intent.createChooser(intent, "Share")
    ContextCompat.startActivity(this, chooser, null)
}

fun Context.openLinkInExternalBrowser(link: String?){
    if (link.isNullOrBlank()) return
    val i = Intent(Intent.ACTION_VIEW)
    i.data = Uri.parse(link)
    startActivity(i)
}