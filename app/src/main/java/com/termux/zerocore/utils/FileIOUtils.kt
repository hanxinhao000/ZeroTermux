package com.termux.zerocore.utils

import com.example.xh_lib.utils.LogUtils
import com.example.xh_lib.utils.UUtils
import com.google.gson.Gson
import com.termux.R
import com.termux.zerocore.bean.ClipboardBean
import com.termux.zerocore.bean.MinLBean
import com.termux.zerocore.bean.MinLBean.DataNum
import java.util.Collections

object FileIOUtils {

    public const val COMMEND_KEY = "commi22"
    public const val COMMEND_DEF = "def"

    public const val CLIP_BOARD_KEY = "ClipBoardKey"
    public const val TAG = "FileIOUtils"

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
            arrayList.add(0, dataNum)
            data.list = arrayList
            val s = Gson().toJson(minLBean)
            SaveData.saveData(COMMEND_KEY, s)
        } else {
            val minLBean = Gson().fromJson(commi22, MinLBean::class.java)
            val list = minLBean.data.list
            val dataNum = DataNum()
            dataNum.id = System.currentTimeMillis()
            dataNum.name = nameString
            dataNum.value = commitString
            dataNum.isChecked = isChecked
            list.add(0, dataNum)
            val s = Gson().toJson(minLBean)
            SaveData.saveData(COMMEND_KEY, s)
            UUtils.showMsg(UUtils.getString(R.string.添加成功))
        }

    }

    public fun getClipBoardData() : List<ClipboardBean.Clipboard>? {
        val data = SaveData.getData(CLIP_BOARD_KEY)
        if (!isEmpty(data)) {
            val mClipboardBean = Gson().fromJson(data, ClipboardBean::class.java)
            return mClipboardBean.data.list
        }
        return null
    }

    public fun clearClipBoardString(){
        SaveData.saveData(CLIP_BOARD_KEY, "def")
    }
    public fun deleteClipBoardString(mClipboard: ClipboardBean.Clipboard) :Boolean {
        val data = SaveData.getData(CLIP_BOARD_KEY)
        try {
            if (!isEmpty(data)) {
                val mClipboardBeanTemp = Gson().fromJson(data, ClipboardBean::class.java)
                val list = mClipboardBeanTemp.data.list
                for (i in 0 until list.size) {
                    if (list[i].index == mClipboard.index) {
                        LogUtils.d(TAG, "deleteClipBoardString remove data :${list[i]}")
                        list.removeAt(i)
                        break
                    }
                }
                val s = Gson().toJson(mClipboardBeanTemp)
                SaveData.saveData(CLIP_BOARD_KEY, s)
                return true
            }
        }catch (e:Exception) {
            LogUtils.d(TAG, "deleteClipBoardString remove error :$e")
        }
        return false
    }

    public fun setClipBoardString(value:String) {
        val data = SaveData.getData(CLIP_BOARD_KEY)
        if (isEmpty(data)) {
            val clipboardBean = ClipboardBean()
            val data = ClipboardBean.Data()
            var list:ArrayList<ClipboardBean.Clipboard> = ArrayList()
            val dataNum = ClipboardBean.Clipboard()
            dataNum.name = value
            dataNum.index = 0
            list.add(dataNum)
            data.list = list
            clipboardBean.data = data
            val s = Gson().toJson(clipboardBean)
            SaveData.saveData(CLIP_BOARD_KEY, s)
            LogUtils.d(TAG, "setClipBoardString ClipBoard is Empty,save data :$s")
        } else {
            try {
                val mClipboardBean = Gson().fromJson(data, ClipboardBean::class.java)
                val list = mClipboardBean.data.list
                val dataNum = ClipboardBean.Clipboard()
                dataNum.name = value
                dataNum.index = list.size
                list.add(0, dataNum)
                val s = Gson().toJson(mClipboardBean)
                SaveData.saveData(CLIP_BOARD_KEY, s)
                LogUtils.d(TAG, "setClipBoardString ClipBoard save data :$s")
            } catch (e:Exception) {
                LogUtils.d(TAG, "data error:$e")
            }
        }
    }

    private fun isEmpty(text:String?) :Boolean{
        return text == null || text.isEmpty() || text == COMMEND_DEF
    }

}
