package com.zp.z_file.common

import android.app.Dialog
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.zp.z_file.content.ZFileException

internal abstract class ZFileManageDialog : DialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return create(inflater, container, savedInstanceState)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?) = createDialog(savedInstanceState)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        init(savedInstanceState)
        dialog?.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_DOWN)
                onBackPressed()
            else false
        }
    }

    open fun create(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val layoutID = getContentView()
        if (layoutID <= 0) throw ZFileException("DialogFragment ContentView is not null")
        return inflater.inflate(getContentView(), container, false)
    }

    open fun getContentView(): Int = 0

    abstract fun createDialog(savedInstanceState: Bundle?): Dialog
    abstract fun init(savedInstanceState: Bundle?)

    /**
     * 返回true 拦截，否则销毁
     */
    open fun onBackPressed() = false

}