package com.esabook.auzen.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding

abstract class ViewHolder<T : ViewBinding>(
    parent: ViewGroup,
    inflater: (LayoutInflater, ViewGroup, Boolean) -> T,
    val binding: T = parent.viewBindingInflate(inflater)
) : RecyclerView.ViewHolder(binding.root)