package com.esabook.auzen.article.player

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.Toolbar
import com.esabook.auzen.R

class PlayerToolbar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : Toolbar(context, attrs) {

    val playerView = PlayerView(this)


    init {
        addView(playerView.getView())
    }

    private fun playerStateObserver(state: PlayerView.PlayerState) {
        updatePlayerPaddingRight()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        updatePlayerPaddingRight()
        playerView.playerStateLiveData.observeForever(this::playerStateObserver)
    }

    override fun onDetachedFromWindow() {
        playerView.playerStateLiveData.removeObserver(this::playerStateObserver)
        super.onDetachedFromWindow()
    }

    private fun updatePlayerPaddingRight() {
        if (menu.hasVisibleItems()) {
            playerView.setPaddingRight(0)
        } else {
            playerView.setPaddingRight(resources.getDimensionPixelSize(R.dimen.dp_24))
        }
    }
}