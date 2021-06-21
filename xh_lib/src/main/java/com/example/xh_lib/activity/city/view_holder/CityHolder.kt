package com.blockchain.ub.ui.city.view_holder

import android.view.View
import android.widget.TextView

import com.example.xh_lib.R
import com.example.xh_lib.base.ViewHolder

/**
 * @author ZEL
 * @create By ZEL on 2020/4/14 11:15
 **/
class CityHolder : ViewHolder {

    public lateinit var mTitle:TextView
    public lateinit var mCode:TextView
    constructor(mView: View?) : super(mView){

        mTitle = findViewById(R.id.title)
        mCode = findViewById(R.id.code)

    }
}