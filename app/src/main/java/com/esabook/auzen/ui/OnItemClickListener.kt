package com.esabook.auzen.ui

import androidx.recyclerview.widget.RecyclerView

fun interface OnItemClickListener{
    fun onClick(holder: RecyclerView.ViewHolder, position: Int, payload: Any?)
}