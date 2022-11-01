package com.termux.zerocore.utils

import com.example.xh_lib.utils.UUtils
import com.google.gson.Gson
import com.termux.R
import com.termux.zerocore.bean.MinLBean
import com.termux.zerocore.bean.MinLBean.DataNum

object FileIOUtils {

    public const val COMMEND_KEY = "commi22"
    public const val COMMEND_DEF = "def"

    public fun commendSave(nameString:String, commitString:String, isChecked:Boolean) {
        val commi22 = SaveData.getData(COMMEND_KEY)
        if (commi22 == null || commi22.isEmpty() || commi22 == COMMEND_DEF) {
            val minLBean = MinLBean()
            val data = MinLBean.Data()
            minLBean.data = data
            val arrayList = ArrayList<DataNum>()
            val dataNum = DataNum()
            dataNum.id = System.currentTimeMillis()
            dataNum.name = nameString
            dataNum.value = commitString
            dataNum.isChecked = isChecked
            arrayList.add(dataNum)
            data.list = arrayList
            val s = Gson().toJson(minLBean)
            SaveData.saveData(COMMEND_KEY, s)
            UUtils.showMsg(UUtils.getString(R.string.添加成功))
        } else {
            val minLBean = Gson().fromJson(commi22, MinLBean::class.java)
            val list = minLBean.data.list
            val dataNum = DataNum()
            dataNum.id = System.currentTimeMillis()
            dataNum.name = nameString
            dataNum.value = commitString
            dataNum.isChecked = isChecked
            list.add(dataNum)
            val s = Gson().toJson(minLBean)
            SaveData.saveData(COMMEND_KEY, s)
            UUtils.showMsg(UUtils.getString(R.string.添加成功))
        }

    }


}
