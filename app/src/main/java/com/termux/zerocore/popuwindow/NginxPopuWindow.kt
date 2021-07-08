package com.termux.zerocore.popuwindow

import android.content.Context
import android.util.AttributeSet
import android.view.View
import com.github.iielse.switchbutton.SwitchView


import com.termux.R

class NginxPopuWindow : BasePuPuWindow {

    companion object{

        var isRun = false


    }

    private var mSwitchButton: SwitchView? = null
    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    override fun initView(mView: View) {
        mSwitchButton = mView.findViewById(R.id.switch_btn)

        mSwitchButton!!.isOpened = isRun


        mSwitchButton!!.setOnStateChangedListener(object : SwitchView.OnStateChangedListener {
            override fun toggleToOn(view: SwitchView?) {

                isRun = true
                mSwitchButton!!.isOpened = isRun

            }

            override fun toggleToOff(view: SwitchView?) {

                isRun = false
                mSwitchButton!!.isOpened = isRun

            }

        })
    }

    override fun getViewId(): Int {

        return R.layout.pupu_window_nginx
    }
}
