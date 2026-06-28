package com.termux.zerocore.utils

import android.content.Context
import androidx.annotation.ArrayRes
import androidx.annotation.StringRes
import com.example.xh_lib.utils.UUtils
import com.termux.zerocore.config.ztcommand.navigation.ZtForegroundActivityHolder

/**
 * 按当前界面语言取字符串。UUtils.getContext() 为 Application 上下文，语言可能固定为中文；
 * 优先使用前台 Activity 的 Context，与界面语言一致。
 */
object ZtLocaleStrings {

    @JvmStatic
    fun context(): Context = ZtForegroundActivityHolder.get() ?: UUtils.getContext()

    @JvmStatic
    fun getString(@StringRes id: Int): String = context().getString(id)

    @JvmStatic
    fun getString(@StringRes id: Int, vararg formatArgs: Any): String =
        context().getString(id, *formatArgs)

    @JvmStatic
    fun getStringArray(@ArrayRes id: Int): Array<String> =
        context().resources.getStringArray(id)

    @JvmStatic
    fun format(@StringRes id: Int, vararg args: Any): String =
        String.format(getString(id), *args)
}
