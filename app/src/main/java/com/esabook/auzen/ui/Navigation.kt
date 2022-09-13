package com.esabook.auzen.ui

import android.view.ViewGroup
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.FragmentManager
import com.esabook.auzen.R
import timber.log.Timber

class Navigation(val fragmentManager: FragmentManager, @IdRes val containerId: Int) {

    fun submit(transaction: Navigation.() -> Unit) {
        fragmentManager.beginTransaction().let {
            transaction.invoke(this)
        }
    }

    fun getLastVisibleFragment(): Fragment {
        val fg = fragmentManager.fragments
        fg.forEachIndexed { i, it ->
            val info = mapOf(
                "no" to i,
                "visible" to it.isVisible,
                "hidden" to it.isHidden,
                "name" to it::class.java.simpleName,
                "hash" to it.hashCode()
            )
            Timber.d(info.toString())
        }
        val last = fg.lastOrNull { it.isVisible } ?: fg.last()
        Timber.d("name: ${last::class.java.simpleName} hash: ${last.hashCode()}")
        return last
    }


    companion object {
        fun Fragment.findNavigation(@IdRes id: Int = R.id.nav_host_fragment): Navigation {
            return (requireActivity() as AppCompatActivity).findNavigation(id)
        }

        fun AppCompatActivity.findNavigation(@IdRes id: Int = R.id.nav_host_fragment): Navigation {
            val container = findViewById<ViewGroup>(id)
            if (container !is FragmentContainerView) {
                throw IllegalStateException("Cannot handle: id: $id is not FragmentContainerView}")
            }

            val fragmentManager = supportFragmentManager
            return Navigation(fragmentManager, id)
        }
    }
}