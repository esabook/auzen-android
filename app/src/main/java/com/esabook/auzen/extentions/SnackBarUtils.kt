package com.esabook.auzen.extentions

import android.view.View
import com.google.android.material.snackbar.Snackbar

fun View.snackbar(msg: CharSequence) = Snackbar.make(this, msg, Snackbar.LENGTH_SHORT)
fun View.showSnackbar(msg: CharSequence) = snackbar(msg).show()
