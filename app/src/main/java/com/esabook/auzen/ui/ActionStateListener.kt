package com.esabook.auzen.ui

fun interface ActionStateListener {
    enum class ActionState { START, FAIL, SUCCESS }

    fun updateState(actionState: ActionState)

}