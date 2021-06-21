package com.termux.zerocore.popuwindow


import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.AttributeSet
import android.view.View
import android.widget.PopupWindow
import com.example.xh_lib.utils.UUtils

/**
 * @author ZEL
 * @create By ZEL on 2020/4/9 14:28
 **/
abstract class BasePuPuWindow : PopupWindow {

    public lateinit var mContext: Context
    public lateinit var mActivity: Activity

    constructor(context: Context?) : super(context) {
        initView(context!!)
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        initView(context!!)
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        initView(context!!)
    }


    private fun initView(mContext: Context) {

        this.mContext = mContext
        this.mActivity = mContext as Activity

        contentView = UUtils.getViewLay(getViewId())
        isOutsideTouchable = true

        setBackgroundDrawable( ColorDrawable(Color.TRANSPARENT))
        initView(contentView)
    }


    abstract fun initView(mView: View)

    abstract fun getViewId(): Int


}
