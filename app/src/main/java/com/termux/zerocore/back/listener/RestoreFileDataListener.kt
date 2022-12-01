package com.termux.zerocore.back.listener

import com.termux.zerocore.back.bean.DataBean
import java.io.File

interface RestoreFileDataListener {
    fun file(mDataBean: DataBean)
}
