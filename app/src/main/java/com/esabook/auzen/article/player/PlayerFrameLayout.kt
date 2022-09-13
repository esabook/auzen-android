package com.esabook.auzen.article.player

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout

class PlayerFrameLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    var playerView : PlayerView? = null

    override fun onDetachedFromWindow() {
        removeView(playerView?.getView())
        playerView = null
        super.onDetachedFromWindow()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (playerView == null)
            playerView = PlayerView(this)

        addView(playerView?.getView())
    }
}