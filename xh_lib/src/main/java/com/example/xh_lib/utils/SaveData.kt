package com.example.xh_lib.utils

import android.content.Context
import com.google.gson.Gson

/**
 * @author ZEL
 * @create By ZEL on 2020/7/29 11:35
 **/
object SaveData {

    fun saveStringOther(key: String, values: String) {

        val sharedPreferences = UUtils.getContext().getSharedPreferences("start_flash", Context.MODE_PRIVATE)

        sharedPreferences.edit().putString(key, values).apply()

    }

    fun getStringOther(key: String): String? {

        val sharedPreferences = UUtils.getContext().getSharedPreferences("start_flash", Context.MODE_PRIVATE)

        return sharedPreferences.getString(key, "def")

        // return ""

    }



}