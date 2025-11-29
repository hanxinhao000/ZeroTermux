package com.termux.zerocore.settings

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.termux.R
import com.termux.zerocore.ftp.utils.UserSetManage

/**
 *  此类未完成，后期需要重新继续此需求
 *  需求：更新在线脚本url，可随意变更数据源
 */
class ZTOnlineServerActivity : AppCompatActivity() {
    private val TAG = "ZTInstallActivity"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ztonline)
        initUrlData()
    }

    // 拉取本地数据
    private fun initUrlData() {
        val ztUserBean = UserSetManage.Companion.get().getZTUserBean()
        var serverJsonString = ztUserBean.serverJsonString
        var dataServers: UserSetManage.DataServers? = null
        if (serverJsonString.isNullOrEmpty()) {
            dataServers = UserSetManage.Companion.get().defServerIPName()
        } else {
            dataServers = Gson().fromJson<UserSetManage.DataServers>(serverJsonString,
                UserSetManage.DataServers::class.java)
        }
        Log.i(TAG, "initUrlDataxxxxxx $dataServers")
    }

}
