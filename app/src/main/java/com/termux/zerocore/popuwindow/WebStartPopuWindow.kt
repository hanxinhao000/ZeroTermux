package com.termux.zerocore.popuwindow


import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.util.AttributeSet
import android.view.View
import android.widget.EditText
import android.widget.TextView
import com.example.xh_lib.utils.UUtils
import com.github.iielse.switchbutton.SwitchView
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.termux.R
import com.termux.zerocore.dialog.LoadingDialog
import com.termux.zerocore.dialog.SwitchDialog
import com.termux.zerocore.url.FileUrl.zeroTermuxApk
import java.io.File
import java.io.InputStream

class WebStartPopuWindow : BasePuPuWindow {

    companion object{

        var isRun = false


    }

    private var mSwitchButton: SwitchView? = null
    private var port_ed: TextView? = null
    private var root_address: TextView? = null
    private var http_address: TextView? = null
    private var code_address: TextView? = null
    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    override fun initView(mView: View) {
        mSwitchButton = mView.findViewById(R.id.switch_btn)
        port_ed = mView.findViewById(R.id.port_ed)
        root_address = mView.findViewById(R.id.root_address)
        http_address = mView.findViewById(R.id.http_address)
        code_address = mView.findViewById(R.id.code_address)

        mSwitchButton!!.isOpened = isRun




        mSwitchButton!!.setOnStateChangedListener(object : SwitchView.OnStateChangedListener {
            override fun toggleToOn(view: SwitchView?) {

                isRun = true
                mSwitchButton!!.isOpened = isRun
                startFun(true)

            }

            override fun toggleToOff(view: SwitchView?) {

                isRun = false
                mSwitchButton!!.isOpened = isRun
                startFun(false)
            }

        })

        code_address!!.setOnClickListener {


            // SendJoinUtils.INSTANCE.sendJoin(this);
            val intent = Intent()
            intent.data = Uri.parse("https://github.com/joinAero/AndroidWebServ") //Url 就是你要打开的网址
            intent.action = Intent.ACTION_VIEW
            val activity = mContext as Activity
            activity.startActivity(intent) //启动浏览器



        }
    }

    override fun getViewId(): Int {

        return R.layout.pupu_window_web_start
    }


    private fun startFun(isTrue:Boolean){

        if(isTrue){

            try {


                http_address!!.text = "${UUtils.getHostIP()}:${7766}"
                try {

                    val activity = mContext as Activity
                    val intent = Intent()
                    intent.action = "com.start_server.action.ENTER"
                    intent.putExtra("address", "")
                    intent.putExtra("port", 7766)
                    activity.startActivity(intent)
                    UUtils.showMsg(UUtils.getString(R.string.开启成功))

                }catch (e:Exception){
                    e.printStackTrace()
                    isRun = false
                    mSwitchButton!!.isOpened = isRun
                    installApk(mContext.getAssets().open("apk/WebStart.ip"), "WebStart")
                }





            }catch (e:Exception){
                e.printStackTrace()
            }

        }else{


            try {

                val activity = mContext as Activity
                val intent = Intent()
                intent.action = "com.stop_server.action.ENTER"
                activity.startActivity(intent)
                http_address!!.text = "--"
            }catch (e:Exception){
                e.printStackTrace()
                isRun = false
                mSwitchButton!!.isOpened = isRun
                installApk(mContext.getAssets().open("apk/WebStart.ip"), "WebStart")
            }


        }




    }


    private fun installApk(inputStream: InputStream, fileName: String) {
        val switchDialog1: SwitchDialog = switchDialogShow(UUtils.getString(R.string.警告), UUtils.getString(R.string.您未安装该插件))
        switchDialog1.cancel!!.setOnClickListener { switchDialog1.dismiss() }
        switchDialog1.ok!!.setOnClickListener {
            switchDialog1.dismiss()
            XXPermissions.with(mContext)
                .permission(Permission.WRITE_EXTERNAL_STORAGE)
                .permission(Permission.READ_EXTERNAL_STORAGE)
                .permission(Permission.REQUEST_INSTALL_PACKAGES)
                .request(object : OnPermissionCallback {
                    override fun onGranted(permissions: List<String>, all: Boolean) {
                        if (all) {
                            if (!zeroTermuxApk.exists()) {
                                zeroTermuxApk.mkdirs()
                            }
                            val file1 = File(Environment.getExternalStorageDirectory(), "/xinhao/apk/$fileName.apk")
                            val loadingDialog = LoadingDialog(mContext)
                            loadingDialog.show()
                            UUtils.writerFileRawInput(file1, inputStream)
                            Thread {
                                try {
                                    Thread.sleep(2000)
                                } catch (e: InterruptedException) {
                                    e.printStackTrace()
                                }
                                UUtils.runOnThread(Runnable {
                                    loadingDialog.dismiss()
                                    UUtils.installApk(UUtils.getContext(), file1.absolutePath)
                                })
                            }.start()
                        } else {
                            UUtils.showMsg("无权限")
                        }
                    }

                    override fun onDenied(permissions: List<String>, never: Boolean) {
                        if (never) {
                            UUtils.showMsg("无权限")
                            // 如果是被永久拒绝就跳转到应用权限系统设置页面
                            XXPermissions.startPermissionActivity(mContext, permissions)
                        } else {
                            UUtils.showMsg("无权限")
                        }
                    }
                })
        }
    }


    private fun switchDialogShow(title: String, msg: String): SwitchDialog {
        val switchDialog = SwitchDialog(mContext)
        switchDialog.title!!.text = title
        switchDialog.msg!!.text = msg
        switchDialog.other!!.visibility = View.GONE
        switchDialog.ok!!.text = UUtils.getString(R.string.确定)
        switchDialog.cancel!!.text = UUtils.getString(R.string.取消)
        switchDialog.show()
        return switchDialog
    }

}
