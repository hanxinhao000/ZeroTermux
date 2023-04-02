package com.zp.z_file.ui.dialog

import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.zp.z_file.R
import com.zp.z_file.content.dip2px
import com.zp.z_file.content.getColorById
import com.zp.z_file.content.getStringById

internal class ZFileLoadingDialog(
    context: Context,
    private var title: String? = context getStringById R.string.zfile_loading
) : AlertDialog(context) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val wh = context dip2px 136f
        setContentView(getContentView(wh))
    }

    private fun getContentView(wh: Int) = LinearLayout(context).apply {
        window?.setLayout(wh, wh)
        layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        )
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER

        val barWh = context dip2px 45f
        val bar = ProgressBar(context).run {
            layoutParams = LinearLayout.LayoutParams(barWh, barWh)
            this
        }
        addView(bar)
        val padding = context dip2px 14f
        val titleTxt = TextView(context).run {
            layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
            gravity = Gravity.CENTER
            setPadding(0, padding, 0, 0)
            textSize = 13f
            maxLines = 1
            setTextColor(context getColorById R.color.zfile_black)
            text = if (title.isNullOrEmpty()) context getStringById R.string.zfile_loading else title
            this
        }
        addView(titleTxt)
    }

    override fun dismiss() {
        System.gc()
        super.dismiss()
    }
}