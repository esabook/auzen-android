package com.esabook.auzen.ui

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.Window
import com.esabook.auzen.databinding.ProgressDialogBinding
import timber.log.Timber


open class ProgressDialog(context: Context) : Dialog(context) {

    private var binding: ProgressDialogBinding =
        ProgressDialogBinding.inflate(LayoutInflater.from(context))

    private val dismissHandler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(binding.root)
        window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    override fun show() {
        if (isOnRefreshing) return

        isOnRefreshing = true
        try {
            super.show()
        } catch (e: Exception) {
        }
        Timber.v("showed")
    }

    var isOnRefreshing = false
    fun setRefreshing(show: Boolean) {
        dismissHandler.removeCallbacks(this::dismiss)
        if (show)
            show()
        else
            dismissHandler.postDelayed(this::dismiss, 500)
    }

    override fun dismiss() {
        if (isOnRefreshing.not()) return

        isOnRefreshing = false
        try {
            super.dismiss()
        } catch (e: Exception) {
        }
        Timber.v("dismissed")
    }

    inline fun onShow(crossinline runnable: ProgressDialog.() -> Unit): ProgressDialog {
        setOnShowListener {
            runnable.invoke(this)
            setOnShowListener(null)
        }
        return this
    }

    inline fun onDismiss(crossinline runnable: ProgressDialog.() -> Unit): ProgressDialog {
        setOnDismissListener {
            runnable.invoke(this)
            setOnDismissListener(null)
        }
        return this
    }

    inline fun onCancel(crossinline runnable: ProgressDialog.() -> Unit): ProgressDialog {
        setOnCancelListener {
            runnable.invoke(this)
            setOnCancelListener(null)
        }
        return this
    }

}