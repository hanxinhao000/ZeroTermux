package com.zp.z_file.common

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

internal abstract class ZFileActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        create()
        init(savedInstanceState)
    }

    open fun create() = setContentView(getContentView())

    open fun getContentView(): Int = 0

    abstract fun init(savedInstanceState: Bundle?)


}