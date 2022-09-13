package com.esabook.auzen.ui

fun interface WebViewScrollChangedListener {
    fun onScrollChanged(x: Int, y: Int, oldx: Int, oldy: Int)
}