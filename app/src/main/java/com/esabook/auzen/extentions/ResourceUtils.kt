package com.esabook.auzen.extentions

import android.content.Context
import android.content.res.Resources
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.core.content.res.ResourcesCompat

fun Resources.getDrw(@DrawableRes id: Int) = ResourcesCompat.getDrawable(this, id, null)
fun Resources.getClr(@ColorRes id: Int) = ResourcesCompat.getColor(this, id, null)
fun Context.res() = resources