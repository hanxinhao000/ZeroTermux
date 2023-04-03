package com.zp.z_file.ui.dialog

import android.content.Context
import android.view.View
import android.widget.TextView
import com.zp.z_file.R
import com.zp.z_file.util.ZFileUUtils


class LoadingDialog : BaseDialogCentre {
    public var msg:TextView? = null
    private var isStart: Boolean = false
    constructor(context: Context) : super(context)
    constructor(context: Context, themeResId: Int) : super(context, themeResId)
    private var isTime:Int = 300
    override fun initViewDialog(mView: View?) {
        msg = mView?.findViewById(R.id.msg)
        setAnim()

    }

    override fun getContentView(): Int {

        return R.layout.dialog_loading
    }


    override fun show() {
        super.show()
        setCancelable(false)
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


                //  UUtils.showLog("倒计时:${isTime}")
                if(isTime > -1){

                    isTime--

                    //  UUtils.showLog("倒计时:${isTime}")

                }else{
                    //  UUtils.showLog("倒计时:${isTime > 0}")
                    if(isTime <= -1) {

                        ZFileUUtils.runOnUIThread(Runnable {


                            try {
                                if (isShowing()) {
                                    isStart = false
                                    dismiss()

                                }
                            } catch (e:Exception) {

                                e.printStackTrace()
                            } finally {

                            }


                        })
                    }
                }

                Thread.sleep(100)

                index++

            }

        }).start()


    }
}
