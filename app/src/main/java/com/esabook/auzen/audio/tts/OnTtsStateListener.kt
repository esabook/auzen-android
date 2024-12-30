package com.esabook.auzen.audio.tts

interface OnTtsStateListener {
    fun onTtsReady()

    fun onTtsStart(text: String?, playId: String)

    fun onTtsStop(playId: String)
}
