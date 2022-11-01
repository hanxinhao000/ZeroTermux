package com.termux.zerocore.utils


import android.content.Context
import android.util.TypedValue
import com.example.xh_lib.utils.LogUtils
import com.example.xh_lib.utils.UUtils
import com.termux.zerocore.view.DiaLogData

object WindowsUtils {

    private const val GRID_WIDTH = 90
    private const val TAG = "WindowsUtils"

    public fun getGridNumber() :Int {
        val resources = UUtils.getContext().resources
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val number = screenWidth / dp2px(UUtils.getContext(), GRID_WIDTH)
        LogUtils.d(TAG, "getGridNumber:$number")
        return number
    }

    public fun dp2px(context: Context, dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(),
            context.resources.displayMetrics
        ).toInt()
    }
}
