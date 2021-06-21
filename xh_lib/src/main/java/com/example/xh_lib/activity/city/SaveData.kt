package com.blockchain.ub.utils

import android.app.Activity
import android.content.Intent
import com.example.xh_lib.activity.city.CityActivity
import com.example.xh_lib.activity.city.CityListener


/**
 * @author ZEL
 * @create By ZEL on 2020/3/31 16:08
 **/
class SaveData {

    companion object {
        private var instance: SaveData? = null
            get() {
                if (field == null) {
                    field = SaveData()
                }
                return field
            }

        fun get(): SaveData {
            return instance!!
        }

        //简体
        public const val ZH_RCN = 0
        //英文
        public const val EN_REN = 1


    }


    public var mGlobalTheme: Boolean = true

    public var mCityListener: CityListener? = null


    public fun startCityActivity(mActivity: Activity, mCityListener: CityListener, language: Int) {

        CityActivity.LANGUAGE = language

        this.mCityListener = mCityListener

        mActivity.startActivity(Intent(mActivity, CityActivity::class.java))

    }

}