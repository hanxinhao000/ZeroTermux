package com.blockchain.ub.ui.city.adapter


import com.blockchain.ub.ui.city.view_holder.CityHolder
import com.example.xh_lib.R
import com.example.xh_lib.activity.city.CityActivity
import com.example.xh_lib.activity.city.data.CityBeanItem
import com.example.xh_lib.base.ListBaseAdapter
import com.example.xh_lib.base.ViewHolder
import com.example.xh_lib.utils.UUtils

/**
 * @author ZEL
 * @create By ZEL on 2020/4/14 11:15
 **/
class CityAdapter : ListBaseAdapter<CityBeanItem> {

    constructor(list: ArrayList<CityBeanItem>?) : super(list)

    override fun getViewHolder(): ViewHolder {


        return CityHolder(UUtils.getViewLay(R.layout.list_city))
    }

    override fun initView(position: Int, t: CityBeanItem?, viewHolder: ViewHolder?) {
        var mCityHolder : CityHolder = viewHolder as CityHolder

        if(CityActivity.LANGUAGE == 0){
            //简体
            mCityHolder.mTitle.text = t!!.ch_name
            mCityHolder.mCode.text = t!!.phone_code

        }else{
            //英文
            mCityHolder.mTitle.text = t!!.en_name
            mCityHolder.mCode.text = t!!.phone_code
        }


    }
}