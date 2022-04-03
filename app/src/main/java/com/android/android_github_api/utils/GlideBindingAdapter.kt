package com.android.android_github_api.utils

import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.databinding.BindingAdapter
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.RequestManager

object GlideBindingAdapter {
    @JvmStatic
    @BindingAdapter(
        "glideInstance",
        "glideUrl",
        "glideCenterCrop",
        "glideCircularCrop",
        requireAll = false
    )
    fun ImageView.bindGlideUrl(
        glideInstance: RequestManager,
        url: String?,
        centerCrop: Boolean = false,
        circularCrop: Boolean = false
    ) {
        if (url == null) return

        createGlideRequestWithUrl(
            glideInstance,
            url,
            centerCrop,
            circularCrop
        ).into(this)
    }

    private fun createGlideRequestWithUrl(
        glideInstance: RequestManager,
        url: String,
        centerCrop: Boolean,
        circularCrop: Boolean
    ): RequestBuilder<Drawable> {
        val req = glideInstance.load(url)
        if (centerCrop) req.centerCrop()
        if (circularCrop) req.circleCrop()
        return req
    }
}