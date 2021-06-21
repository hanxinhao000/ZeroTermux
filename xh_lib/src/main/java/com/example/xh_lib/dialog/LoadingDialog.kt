package com.example.xh_lib.dialog


import android.content.Context
import android.view.View
import android.widget.ImageView

import com.blockchain.ub.util.custom.dialog.BaseDialogCentre
import com.example.xh_lib.R
import com.example.xh_lib.utils.UUtils

/**
 * @author ZEL
 * @create By ZEL on 2020/4/13 17:29
 **/
class LoadingDialog : BaseDialogCentre {

    constructor(context: Context) : super(context)
    constructor(context: Context, themeResId: Int) : super(context, themeResId)

    private lateinit var mLanding: ImageView

    private lateinit var mImageList: ArrayList<Int>

    private var isStart: Boolean = false

    override fun initViewDialog(mView: View) {

        mLanding = mView.findViewById(R.id.landing)
        mImageList = ArrayList()
        mImageList.add(R.mipmap.loading1)
        mImageList.add(R.mipmap.loading2)
        mImageList.add(R.mipmap.loading3)
        mImageList.add(R.mipmap.loading4)
        mImageList.add(R.mipmap.loading5)
        mImageList.add(R.mipmap.loading6)

        setAnim()
    }

    override fun getContentView(): Int {


        return R.layout.dialog_landing
    }

    override fun dismiss() {
        super.dismiss()
        isStart = false

    }

    //动画
    private fun setAnim() {

        var index = 0
        if (isStart) {
            return
        }



        isStart = true

        Thread(Runnable {


            while (isStart) {

                if (index >= mImageList.size) {
                    index = 0
                }

                Thread.sleep(100)

                UUtils.runOnUIThread(Runnable {

                    try {
                        mLanding.setImageResource(mImageList[index])
                    }catch (e:Exception){

                    }
                    UUtils.showLog("i1:$index")
                })
                UUtils.showLog("i:$index")


                index++



            }

        }).start()


    }
}