package com.zp.z_file.util

import android.util.Log
import com.zp.z_file.content.LOG_TAG
import com.zp.z_file.content.getZFileConfig

internal object ZFileLog {

    private const val I = 0
    private const val E = 1

    fun i(msg: String?) {
        i(LOG_TAG, msg)
    }

    fun e(msg: String?) {
        e(LOG_TAG, msg)
    }

    fun i(tag: String, message: String?) {
        log(I, tag, message)
    }

    fun e(tag: String, message: String?) {
        log(E, tag, message)
    }

    private fun log(type: Int, TAG: String, msg: String?) {
        if (getZFileConfig().showLog) {
            when (type) {
                E -> Log.e(TAG, msg ?: "msg is Null")
                I -> Log.i(TAG, msg ?: "msg is Null")
            }
        }
    }

}