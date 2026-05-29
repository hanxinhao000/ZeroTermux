package com.termux.zerocore.utils

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.widget.ImageView
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.S)
object BackgroundBlurUtilsV31 {

    fun applyBlur(imageView: ImageView, radius: Int) {
        val effect = RenderEffect.createBlurEffect(
            radius.toFloat(), radius.toFloat(), Shader.TileMode.CLAMP
        )
        imageView.setRenderEffect(effect)
    }

    fun removeBlur(imageView: ImageView) {
        imageView.setRenderEffect(null)
    }
}
