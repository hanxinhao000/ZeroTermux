package com.termux.zerocore.bean.online_sh

data class OnLineShBean(
    val code: Int,
    val `data`: List<Data>,
    val home: String,
    val ip: String,
    val msg: String,
    val serviceName: String,
    val versionName: String
)

data class Data(
    val download: String,
    val isDownload: String,
    val name: String,
    val note: String
)
