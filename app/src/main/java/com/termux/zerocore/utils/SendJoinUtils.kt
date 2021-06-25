package com.termux.zerocore.utils

import android.app.Activity
import android.content.Intent
import com.google.gson.Gson
import com.termux.zerocore.bean.JoinBean
import java.util.*

object  SendJoinUtils {



    public fun sendJoin(mActivity:Activity){

        val arrayList = ArrayList<JoinBean.Data>()

        val data = JoinBean.Data()
        data.id = "9f77fc393ec5d4f7"
        data.name = "服务器1"
        data.ip = "127.0.0.1"

        arrayList.add(data)
        arrayList.add(data)
        arrayList.add(data)

        val joinBean = JoinBean()
        joinBean.arrayList = arrayList

        val toJson = Gson().toJson(joinBean)

        try {
            val intent = Intent()
            intent.action = "com.zero_join.action.ENTER"
            intent.putExtra("join_noke",toJson)
            mActivity.startActivity(intent)
        }catch (e:Exception){
            e.printStackTrace()
        }

    }

}
