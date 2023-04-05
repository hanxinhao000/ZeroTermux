package com.zp.z_file.listener


import com.zp.z_file.bean.DataBean
import java.io.File

interface RestoreFileDataListener {
    fun file(mDataBean: DataBean)
}
