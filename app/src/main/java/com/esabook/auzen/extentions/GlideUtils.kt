package com.esabook.auzen.extentions

import android.graphics.drawable.Drawable
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import timber.log.Timber

fun <T: ImageView> T.loadImageWithGlide(
    imgObj: Any?,
    cacheMode: DiskCacheStrategy = DiskCacheStrategy.AUTOMATIC,
    onFail: (T.() -> Unit)? = null,
    onSuccess: (T.() -> Unit)? = null,
) {
    try {
        Glide.with(this)
            .load(imgObj)
            .placeholder(this.drawable)
            .error(this.drawable)
            .diskCacheStrategy(cacheMode)
            .addListener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable>,
                    isFirstResource: Boolean
                ): Boolean {
                    onFail?.invoke(this@loadImageWithGlide)
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable,
                    model: Any,
                    target: Target<Drawable>?,
                    dataSource: DataSource,
                    isFirstResource: Boolean
                ): Boolean {
                    onSuccess?.invoke(this@loadImageWithGlide)
                    return false
                }

            })
            .into(this)
            .clearOnDetach()
            .waitForLayout()
    } catch (e: Exception) {
        Timber.w(e)
    }

}