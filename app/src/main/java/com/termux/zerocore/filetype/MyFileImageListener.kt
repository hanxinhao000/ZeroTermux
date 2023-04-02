package com.termux.zerocore.filetype

import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.termux.R
import com.zp.z_file.listener.ZFileImageListener

import java.io.File

class MyFileImageListener : ZFileImageListener() {

    /**
     * 图片类型加载
     */
    override fun loadImage(imageView: ImageView, file: File) {
        Glide.with(imageView.context)
            .load(file)
            .apply(RequestOptions().apply {
                placeholder(R.drawable.ic_zfile_other)
                error(R.drawable.ic_zfile_other)
            })
            .into(imageView)
    }
}
