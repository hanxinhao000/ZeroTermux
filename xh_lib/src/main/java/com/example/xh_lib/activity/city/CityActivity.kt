package com.example.xh_lib.activity.city

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.TypedValue
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import com.blockchain.ub.ui.city.adapter.CityAdapter
import com.blockchain.ub.utils.SaveData
import com.example.xh_lib.R
import com.example.xh_lib.activity.BaseThemeActivity
import com.example.xh_lib.activity.city.data.CityBeanItem
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.lang.reflect.Type

class CityActivity : BaseThemeActivity() {


    companion object {

        public var LANGUAGE = 0

    }

    //缓存的

    private val mListView by lazy { findViewById<ListView>(R.id.list_view) }
    private val mSearch by lazy { findViewById<EditText>(R.id.search) }
    private val mCancel by lazy { findViewById<TextView>(R.id.cancel) }

    //主要
    private lateinit var mArrayList: ArrayList<CityBeanItem>
    private val mCacheArrayList: ArrayList<CityBeanItem> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_city)
        setGoneTitle()
        jxJson()

        mCancel.setOnClickListener {
            finish()
        }


        mSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {

            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

                val toString = p0.toString()

                mCacheArrayList.clear()

                if(LANGUAGE == 0){
                    if(toString.isEmpty()){
                        mListView.adapter = CityAdapter(mArrayList)
                    }else{
                        for (i in 0 until mArrayList.size) {

                            if (mArrayList[i].ch_name.contains(toString)) {

                                mCacheArrayList.add(mArrayList[i])



                            }

                        }
                        mListView.adapter = CityAdapter(mCacheArrayList)
                    }

                }else{
                    if(toString.isEmpty()){
                        mListView.adapter = CityAdapter(mArrayList)
                    }else{
                        for (i in 0 until mArrayList.size) {

                            if (mArrayList[i].en_name.toLowerCase().contains(toString.toLowerCase())) {

                                mCacheArrayList.add(mArrayList[i])



                            }

                        }
                        mListView.adapter = CityAdapter(mCacheArrayList)
                    }

                }



            }


        })
    }

    //先解析json

    private fun jxJson() {

        val bufferedInputStream = BufferedInputStream(assets.open("country.json"))

        val byteArrayOutputStream = ByteArrayOutputStream()

        val byteArray = ByteArray(1024) { 0 }

        var len: Int = 0

        while (((bufferedInputStream.read(byteArray)).also { len = it }) != -1) {
            byteArrayOutputStream.write(byteArray, 0, len)
            //获取当前下载量
        }

        val toString = byteArrayOutputStream.toString("UTF-8")

        val gson = Gson()

        val type: Type = object : TypeToken<List<CityBeanItem?>?>() {}.type
        val beanOnes: List<CityBeanItem> = gson.fromJson(toString, type)

        mArrayList = ArrayList<CityBeanItem>()

        mArrayList.addAll(beanOnes)

        mListView.adapter = CityAdapter(mArrayList)

        mListView.setOnItemClickListener { adapterView, view, i, l ->

            if (SaveData.get().mCityListener != null) {
                var language = ""
                language = if (LANGUAGE == 0) {
                    mArrayList[i].ch_name
                } else {
                    mArrayList[i].en_name
                }

                SaveData.get().mCityListener!!.cityName(language, Integer.parseInt(mArrayList[i].phone_code))
            }
            finish()


        }


        bufferedInputStream.close()
        byteArrayOutputStream.close()


    }

    override fun refreshUI(mBackground: TypedValue, mTextColor: TypedValue) {

    }
}
