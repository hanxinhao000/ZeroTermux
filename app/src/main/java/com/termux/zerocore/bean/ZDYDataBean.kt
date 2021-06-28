package com.termux.zerocore.bean

import com.arialyy.aria.core.common.AbsEntity
import com.arialyy.aria.core.download.DownloadEntity

data class ZDYDataBean(
    val code: Int,
    val `data`: ArrayList<Data>,
    val home: String,
    val ip: String,
    val versionName: String,
    val serviceName: String
)

data class Data(
    val download: String,
    val name: String,
    val size: String,
    val fileName: String,
    val note: String,
    var progress: Int,
    var isRun: Boolean = false,
    var isFail: Boolean = false,
    var speed: String,
    var timeLeft: String,
    var convertCurrentProgress: String,
    var id: Long = 0,
    var mDownloadEntity: AbsEntity
)
