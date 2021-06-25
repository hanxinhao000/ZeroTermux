package com.termux.zerocore.bean

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
    val note: String
)
