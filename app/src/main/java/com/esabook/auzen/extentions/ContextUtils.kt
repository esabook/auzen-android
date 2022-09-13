package com.esabook.auzen.extentions

import android.content.Context
import android.content.ContextWrapper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking


tailrec fun Context.activity(): AppCompatActivity? = when (this) {
    is AppCompatActivity -> this
    else -> (this as? ContextWrapper)?.baseContext?.activity()
}

fun Context.toast(charSequence: CharSequence?) {
    if (charSequence.isNullOrBlank()) return
    runBlocking(Dispatchers.Main.immediate){
        Toast.makeText(this@toast, charSequence, Toast.LENGTH_SHORT).show()
    }
}