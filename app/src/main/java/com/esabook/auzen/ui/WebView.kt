package com.esabook.auzen.ui

import android.content.Context
import android.util.AttributeSet
import android.webkit.WebView

class WebView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : WebView(context, attrs) {

    private val onScrollChangedListener = ArrayList<WebViewScrollChangedListener>()

    fun addOnScrollChanged(listener: WebViewScrollChangedListener){
        onScrollChangedListener.add(listener)
    }

    fun removeOnScrollChanged(listener: WebViewScrollChangedListener){
        onScrollChangedListener.remove(listener)
    }

    override fun onScrollChanged(l: Int, t: Int, oldl: Int, oldt: Int) {
        super.onScrollChanged(l, t, oldl, oldt)
        onScrollChangedListener.forEach { it.onScrollChanged(l, t, oldl, oldt) }
    }

    fun getTotalContentHeight() = computeVerticalScrollRange()
}