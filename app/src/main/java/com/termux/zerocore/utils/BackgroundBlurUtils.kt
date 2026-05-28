package com.termux.zerocore.utils

import android.os.Build
import android.widget.ImageView

object BackgroundBlurUtils {

    @JvmStatic
    fun applyBlur(imageView: ImageView, blurRadius: Int) {
        if (blurRadius <= 0) {
            removeBlur(imageView)
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            BackgroundBlurUtilsV31.applyBlur(imageView, blurRadius)
        }
    }

    @JvmStatic
    fun removeBlur(imageView: ImageView) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            BackgroundBlurUtilsV31.removeBlur(imageView)
        }
    }
}
