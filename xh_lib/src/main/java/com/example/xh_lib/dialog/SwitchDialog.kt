package com.example.xh_lib.dialog

import android.content.Context
import android.view.View
import android.widget.TextView
import com.blockchain.ub.util.custom.dialog.BaseDialogCentre
import com.example.xh_lib.R


/**
 * @author ZEL
 * @create By ZEL on 2020/4/8 11:40
 **/
class SwitchDialog : BaseDialogCentre {

    constructor(context: Context) : super(context)
    constructor(context: Context, themeResId: Int) : super(context, themeResId)

    private lateinit var mCancel:TextView
    private lateinit var mTitle:TextView
    private lateinit var mOk:TextView
    public lateinit var title_msg:TextView

    override fun initViewDialog(mView: View) {

        mCancel = mView.findViewById(R.id.cancel)
        mTitle = mView.findViewById(R.id.title)
        mOk = mView.findViewById(R.id.ok)
        title_msg = mView.findViewById(R.id.title_msg)

        mCancel.setOnClickListener {
            dismiss()
        }

    }


    public fun getTitleTv():TextView{

        return mTitle

    }

    public fun getOkTv():TextView{

        return mOk

    }

    override fun getContentView(): Int {

        return R.layout.dialog_switch
    }


}