package com.blockchain.ub.util.custom.dialog


import android.app.Dialog
import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import com.example.xh_lib.R
import com.example.xh_lib.utils.UUtils


/**
 * @author ZEL
 * @create By ZEL on 2020/4/2 15:04
 **/
abstract class BaseDialogDown : Dialog {

    public lateinit var mContext: Context

    private val mDimAmount = 0.5f
    private val mOutCancel = false
    private val mMargin = 0

    var mWidth: Int = 0
    private val mHeight = 0


    constructor(context: Context) : super(context, R.style.BaseDialog) {
        initView(context)
    }

    constructor(context: Context, themeResId: Int) : super(context, themeResId) {
        initView(context)
    }

    private fun initView(mContext: Context) {

        this.mContext = mContext

        val contentView = getContentView()

        val viewLay = UUtils.getViewLay(contentView)


        initViewDialog(viewLay)
        setContentView(viewLay)
    }

    abstract fun initViewDialog(mView: View)

    override fun show() {
        super.show()

        val params = window!!.attributes
        params.gravity = Gravity.BOTTOM
        params.dimAmount = mDimAmount
        //设置dialog宽度

        //设置dialog宽度
        if (mWidth == 0) {
            params.width = getScreenWidth(context) - 2 * dp2px(context, mMargin.toFloat())
        } else {
            params.width = mWidth
        }

        //设置dialog高度

        //设置dialog高度
        if (mHeight == 0) {
            params.height = WindowManager.LayoutParams.WRAP_CONTENT
        } else {
            params.height = mHeight
        }




        window!!.attributes = params

        setCancelable(mOutCancel)

    }




    open fun getScreenWidth(context: Context): Int {
        val displayMetrics = context.resources.displayMetrics
        return displayMetrics.widthPixels
    }

    open fun dp2px(context: Context, dipValue: Float): Int {
        val scale = context.resources.displayMetrics.density
        return (dipValue * scale + 0.5f).toInt()
    }


    abstract fun getContentView(): Int
}