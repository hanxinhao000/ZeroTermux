package com.termux.zerocore.dialog

import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.TextView
import com.example.xh_lib.utils.LogUtils
import com.example.xh_lib.utils.SaveData
import com.example.xh_lib.utils.UUtils
import com.termux.R
import com.termux.app.TermuxService
import com.termux.shared.termux.TermuxConstants
import com.termux.zerocore.utils.ViewBackUtils
import com.termux.zerocore.view.CustomScrollView
import java.lang.RuntimeException

class ProtocolDialog : BaseDialogCentre, View.OnFocusChangeListener {
    public val TAG = "ProtocolDialog"
    public var edit_text:TextView? = null
    public var ok:TextView? = null
    public var cancel:TextView? = null
    public var custom_scroll: CustomScrollView? = null

    public var isYD = false
    constructor(context: Context) : super(context)
    constructor(context: Context, themeResId: Int) : super(context, themeResId)

    override fun initViewDialog(mView: View?) {
        edit_text = mView?.findViewById(R.id.edit_text)
        custom_scroll = mView?.findViewById(R.id.custom_scroll)
        ok = mView?.findViewById(R.id.ok)
        cancel = mView?.findViewById(R.id.cancel)
        edit_text?.text = UUtils.getString(R.string.许可证)
        cancel?.setOnClickListener {
            val intent = Intent(mContext, TermuxService::class.java)
            intent.action = TermuxConstants.TERMUX_APP.TERMUX_SERVICE.ACTION_STOP_SERVICE
            mContext.startService(intent)
            System.exit(1)
            throw RuntimeException("用户不同意协议,自动退出...")
        }
        ok?.setOnClickListener {
            if (isYD){
                SaveData.saveStringOther("xieyi","true")
                dismiss()
            }else{
                UUtils.showMsg(UUtils.getString(R.string.请认真阅读许可协议))
            }
        }
        custom_scroll?.setOnScrollChangeListener(object : CustomScrollView.OnScrollChangeListener {
            override fun onScrollToStart() {

            }

            override fun onScrollToEnd() {
                isYD = true
            }

        })
        focusManagement()
    }
    private fun focusManagement() {
        custom_scroll?.onFocusChangeListener = this
        ok?.onFocusChangeListener = this
        cancel?.onFocusChangeListener = this
    }

    override fun getContentView(): Int {

        return R.layout.dialog_protocol
    }

    override fun onFocusChange(p0: View?, p1: Boolean) {
        if (p0 == null) {
            LogUtils.d(TAG, "focus changed return view is empty!")
            return
        }
        ViewBackUtils.setBackLine161823_2e(custom_scroll!!)
        ok?.setTextColor(UUtils.getColor(R.color.color_ffffff))
        cancel?.setTextColor(UUtils.getColor(R.color.color_ffffff))
        when (p0.id) {
            R.id.custom_scroll -> {
                ViewBackUtils.setBackLine161823_fd(custom_scroll!!)
            }
            R.id.ok -> {
                ok?.setTextColor(UUtils.getColor(R.color.color_FD9F3E))
            }
            R.id.cancel -> {
                cancel?.setTextColor(UUtils.getColor(R.color.color_FD9F3E))
            }
        }
    }
}
