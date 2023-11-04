package com.termux.zerocore.ftp.utils

import com.example.xh_lib.utils.SaveData
import com.google.gson.Gson
import com.termux.zerocore.bean.ZTUserBean

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

}

