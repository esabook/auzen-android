package com.esabook.auzen.ui

import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.viewbinding.ViewBinding
import timber.log.Timber
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

inline fun <reified T : ViewBinding> Fragment.viewBinding(noinline bind: (View) -> T) =
    FragmentViewBindingDelegate(bind, this)

class FragmentViewBindingDelegate<T : ViewBinding>(
    private val bindMethod: (View) -> T,
    val fragment: Fragment
) : ReadOnlyProperty<Fragment, T>, LifecycleEventObserver {
    private var binding: T? = null

    override fun getValue(thisRef: Fragment, property: KProperty<*>): T {
        binding?.let { return it }

        val lifecycle = fragment.viewLifecycleOwner.lifecycle
        if (!lifecycle.currentState.isAtLeast(Lifecycle.State.INITIALIZED)) {
            error("Cannot access view bindings. View lifecycle is ${lifecycle.currentState}!")
        }

        fragment.viewLifecycleOwnerLiveData.observe(fragment) { viewLifecycleOwner ->
            viewLifecycleOwner.lifecycle.addObserver(this)
        }

        @Suppress("UNCHECKED_CAST")
        binding = bindMethod.invoke(thisRef.requireView())
        return binding!!
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        Timber.d("class: %s [[%s]], event: %s", fragment.javaClass.simpleName, fragment.hashCode(), event.name)
        if (event == Lifecycle.Event.ON_DESTROY) {
            binding = null
        }
    }

}