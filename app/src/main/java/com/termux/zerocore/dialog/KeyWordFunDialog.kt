package com.termux.zerocore.dialog

import android.content.Context
import android.view.View
import android.widget.TextView
import androidx.cardview.widget.CardView
import com.example.xh_lib.utils.UUtils
import com.termux.R
import com.termux.zerocore.ftp.utils.UserSetManage

class KeyWordFunDialog : BaseDialogCentre {
    companion object {
        public const val DOUBLE_CLICK_CUSTOM_COMMAND = 0
        public const val DOUBLE_CLICK_KEYWORD = 1
        public const val DOUBLE_CLICK_NOTHING = 2
        public const val DOUBLE_CLICK_SSH = 3


        public fun getDoubleClickString(code: Int): String {
            return when (code) {
                DOUBLE_CLICK_KEYWORD -> {
                    UUtils.getString(R.string.settings_keyword_item1)
                }
                DOUBLE_CLICK_NOTHING -> {
                    UUtils.getString(R.string.settings_keyword_item2)
                }
                DOUBLE_CLICK_CUSTOM_COMMAND -> {
                    UUtils.getString(R.string.settings_keyword_item3)
                }
                DOUBLE_CLICK_SSH -> {
                    UUtils.getString(R.string.content_ssh_name)
                }
                else -> {
                    UUtils.getString(R.string.settings_keyword_item3)
                }
            }
        }
    }
    public var mItemKeyword: CardView? = null
    public var mNother: CardView? = null
    public var mSsh: CardView? = null
    public var mItemCustomCommand: CardView? = null
    constructor(context: Context) : super(context)
    constructor(context: Context, themeResId: Int) : super(context, themeResId)
    override fun initViewDialog(mView: View?) {
        mItemKeyword = mView?.findViewById(R.id.item_keyword)
        mNother = mView?.findViewById(R.id.item_nother)
        mSsh = mView?.findViewById(R.id.item_ssh)
        mItemCustomCommand = mView?.findViewById(R.id.item_custom_command)
        setOnClick()
    }

    private fun setOnClick() {
        mItemKeyword?.setOnClickListener {
            val ztUserBean = UserSetManage.get().getZTUserBean()
            ztUserBean.doubleClickFun = DOUBLE_CLICK_KEYWORD
            UserSetManage.get().setZTUserBean(ztUserBean)
            dismiss()
        }
        mNother?.setOnClickListener {
            val ztUserBean = UserSetManage.get().getZTUserBean()
            ztUserBean.doubleClickFun = DOUBLE_CLICK_NOTHING
            UserSetManage.get().setZTUserBean(ztUserBean)
            dismiss()
        }
        mSsh?.setOnClickListener {
            val ztUserBean = UserSetManage.get().getZTUserBean()
            ztUserBean.doubleClickFun = DOUBLE_CLICK_SSH
            UserSetManage.get().setZTUserBean(ztUserBean)
            dismiss()
        }
        mItemCustomCommand?.setOnClickListener {
            val ztUserBean = UserSetManage.get().getZTUserBean()
            ztUserBean.doubleClickFun = DOUBLE_CLICK_CUSTOM_COMMAND
            UserSetManage.get().setZTUserBean(ztUserBean)
            dismiss()
        }
    }

    override fun getContentView(): Int {
        return R.layout.dialog_key_word_fun
    }

    override fun show() {
        super.show()
    }
}
