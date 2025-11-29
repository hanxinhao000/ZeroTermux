package com.termux.zerocore.ftp.utils

import com.example.xh_lib.utils.SaveData
import com.google.gson.Gson
import com.termux.zerocore.bean.ZTUserBean
import com.termux.zerocore.http.HTTPIP

class UserSetManage private constructor() {
    companion object {
        private var instance: UserSetManage? = null
            get() {
                if (field == null) {
                    field = UserSetManage()
                }
                return field
            }
        fun get(): UserSetManage{
            return instance!!
        }
    }

    public fun getZTUserBean(): ZTUserBean {
        val zTUserBeanJson = SaveData.getStringOther(ZTConstant.ZT_USER_BEAN_KEY)
        if (zTUserBeanJson.isNullOrEmpty() || zTUserBeanJson == "def") {
            return ZTUserBean()
        }
        return try {
            Gson().fromJson(zTUserBeanJson, ZTUserBean::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            ZTUserBean()
        }
    }

    public fun setZTUserBean(mZTUserBean: ZTUserBean) {
        val toJson = Gson().toJson(mZTUserBean)
        SaveData.saveStringOther(ZTConstant.ZT_USER_BEAN_KEY, toJson)
    }

    public fun defServerIPName(): DataServers {
        val arrayList = ArrayList<DataServer>()
        val dataServer = DataServer("def", HTTPIP.IP)
        arrayList.add(dataServer)

        val dataServers = DataServers(arrayList)

        val ztUserBean = getZTUserBean()
        ztUserBean.serverJsonString = Gson().toJson(dataServers)
        setZTUserBean(ztUserBean)
        return dataServers
    }

    data class DataServer(val serverName: String, val serverUrl: String) {
        override fun toString(): String {
            return "DataServer(serverName='$serverName', serverUrl='$serverUrl')"
        }
    }
    data class DataServers(val dataServers: ArrayList<DataServer>) {
        override fun toString(): String {
            return "DataServers(dataServers=$dataServers)"
        }
    }

}

