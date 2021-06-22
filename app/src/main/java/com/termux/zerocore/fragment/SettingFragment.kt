package com.termux.zerocore.fragment

import android.view.View
import com.example.xh_lib.utils.UUtils
import com.termux.R

class SettingFragment : BaseFragment {
    constructor() : super()

    override fun getFragmentView(): View {

        return UUtils.getViewLay(R.layout.dialog_switch)
    }

    override fun initFragmentView(mView: View?) {

    }
}
