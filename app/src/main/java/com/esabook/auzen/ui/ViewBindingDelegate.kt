package com.esabook.auzen.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

inline fun <reified T : ViewBinding> ViewGroup.viewBinding() = ViewBindingDelegate(T::class.java, this)

class ViewBindingDelegate<T : ViewBinding>(
    private val bindingClass: Class<T>,
    val fragment: ViewGroup
) : ReadOnlyProperty<ViewGroup, T> {
    private var binding: T? = null

    override fun getValue(thisRef: ViewGroup, property: KProperty<*>): T {
        binding?.let { return it }

        val inflateMethod = bindingClass.getMethod("inflate", LayoutInflater::class.java, ViewGroup::class.java)
        @Suppress("UNCHECKED_CAST")
        binding = inflateMethod.invoke(null, LayoutInflater.from(thisRef.context), thisRef) as T
        return binding!!
    }
}