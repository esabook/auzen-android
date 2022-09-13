package com.esabook.auzen.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

inline fun <reified T : ViewBinding> ViewGroup.viewBinding(noinline inflater: (LayoutInflater, ViewGroup, Boolean) -> T) =
    ViewBindingDelegate(inflater, this)

inline fun <T : ViewBinding> ViewGroup.viewBindingInflate(inflater: (LayoutInflater, ViewGroup, Boolean) -> T) =
    inflater.invoke(LayoutInflater.from(context), this, false)

class ViewBindingDelegate<T : ViewBinding>(
    private val inflater: (LayoutInflater, ViewGroup, Boolean) -> T,
    val viewGroup: ViewGroup
) : ReadOnlyProperty<ViewGroup, T> {
    private var binding: T? = null

    override fun getValue(thisRef: ViewGroup, property: KProperty<*>): T {
        binding?.let { return it }

        binding = thisRef.viewBindingInflate(inflater)
        return binding!!
    }
}