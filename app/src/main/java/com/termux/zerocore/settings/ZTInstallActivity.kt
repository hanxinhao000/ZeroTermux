package com.termux.zerocore.settings

import android.os.Bundle
import android.os.Environment
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.example.xh_lib.utils.LogUtils
import com.example.xh_lib.utils.UUtils
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.termux.R
import com.termux.zerocore.dialog.LoadingDialog
import com.termux.zerocore.dialog.SwitchDialog
import com.termux.zerocore.url.FileUrl.zeroTermuxApk
import java.io.File
import java.io.IOException
import java.io.InputStream

class ZTInstallActivity : AppCompatActivity(), View.OnClickListener {
    private val TAG = "ZTInstallActivity"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ztinstall)
        findViewById<CardView>(R.id.api).setOnClickListener(this)
        findViewById<CardView>(R.id.boot).setOnClickListener(this)
        findViewById<CardView>(R.id.styling).setOnClickListener(this)
        findViewById<CardView>(R.id.tasker).setOnClickListener(this)
        findViewById<CardView>(R.id.x11).setOnClickListener(this)
        findViewById<CardView>(R.id.file).setOnClickListener(this)
        findViewById<CardView>(R.id.web).setOnClickListener(this)
        findViewById<CardView>(R.id.float_card_view).setOnClickListener(this)
        findViewById<CardView>(R.id.widget_card_view).setOnClickListener(this)

    }

    override fun onClick(v: View?) {
       when (v?.id) {
           R.id.api -> {
               try {
                   installApk(getAssets().open("apk/termux_api.ip"), "termux_api")
               } catch (e: IOException) {
                   e.printStackTrace()
                   LogUtils.e(TAG, "installApk termux_api error! $e")
               }
           }
           R.id.boot -> {
               try {
                   installApk(getAssets().open("apk/termux_boot.ip"), "termux_boot")
               } catch (e: IOException) {
                   e.printStackTrace()
                   LogUtils.e(TAG, "installApk termux_boot error! $e")
               }
           }
           R.id.styling -> {
               try {
                   installApk(getAssets().open("apk/termux_styling.ip"), "termux_styling")
               } catch (e: IOException) {
                   e.printStackTrace()
                   LogUtils.e(TAG, "installApk termux_styling error! $e")
               }
           }
           R.id.tasker -> {
               try {
                   installApk(getAssets().open("apk/termux_tasker.ip"), "termux_tasker")
               } catch (e: IOException) {
                   e.printStackTrace()
                   LogUtils.e(TAG, "installApk termux_tasker error! $e")
               }
           }
           R.id.x11 -> {
               try {
                   installApk(getAssets().open("apk/termux_x11.ip"), "termux_x11")
               } catch (e: IOException) {
                   e.printStackTrace()
                   LogUtils.e(TAG, "installApk termux_x11 error! $e")
               }
           }
           R.id.file -> {
               try{
                   installApk(getAssets().open("apk/utermux_file_plug.ip"), "files")
               } catch (e: IOException) {
                   e.printStackTrace();
                   LogUtils.e(TAG, "installApk utermux_file_plug error! $e")
               }
           }
           R.id.web -> {
               try{
                   installApk(getAssets().open("apk/WebStart.ip"), "WebStart")
               } catch (e: IOException) {
                   e.printStackTrace();
                   LogUtils.e(TAG, "installApk WebStart error! $e")
               }
           }
           R.id.widget_card_view -> {
               try{
                   installApk(getAssets().open("apk/termux-widge.ip"), "termux-widge")
               } catch (e: IOException) {
                   e.printStackTrace();
                   LogUtils.e(TAG, "installApk WebStart error! $e")
               }
           }
           R.id.float_card_view -> {
               try {
                   installApk(getAssets().open("apk/zero_float.ip"), "zeroFloat");
               } catch (e: IOException) {
                   e.printStackTrace()
                   LogUtils.e(TAG, "installApk zero_float error! $e")
               }
           }
       }
    }

    private fun installApk(inputStream: InputStream, fileName: String) {
        val switchDialog1: SwitchDialog = switchDialogShow(
            UUtils.getString(R.string.警告),
            UUtils.getString(R.string.您未安装该插件)
        )
        switchDialog1.cancel!!.setOnClickListener { switchDialog1.dismiss() }
        switchDialog1.ok!!.setOnClickListener {
            switchDialog1.dismiss()
            XXPermissions.with(this@ZTInstallActivity)
                .permission(Permission.WRITE_EXTERNAL_STORAGE)
                .permission(Permission.READ_EXTERNAL_STORAGE)
                .permission(Permission.REQUEST_INSTALL_PACKAGES)
                .request(object : OnPermissionCallback {
                    override fun onGranted(
                        permissions: List<String>,
                        all: Boolean
                    ) {
                        if (all) {
                            if (!zeroTermuxApk.exists()) {
                                zeroTermuxApk.mkdirs()
                            }
                            val file1 = File(
                                Environment.getExternalStorageDirectory(),
                                "/xinhao/apk/$fileName.apk"
                            )
                            val loadingDialog =
                                LoadingDialog(this@ZTInstallActivity)
                            loadingDialog.show()
                            UUtils.writerFileRawInput(file1, inputStream)
                            Thread {
                                try {
                                    Thread.sleep(2000)
                                } catch (e: InterruptedException) {
                                    e.printStackTrace()
                                }
                                runOnUiThread {
                                    loadingDialog.dismiss()
                                    UUtils.installApk(UUtils.getContext(), file1.absolutePath)
                                }
                            }.start()
                        } else {
                            UUtils.showMsg("无权限")
                        }
                    }

                    override fun onDenied(
                        permissions: List<String>,
                        never: Boolean
                    ) {
                        if (never) {
                            UUtils.showMsg("无权限")
                            // 如果是被永久拒绝就跳转到应用权限系统设置页面
                            XXPermissions.startPermissionActivity(this@ZTInstallActivity, permissions)
                        } else {
                            UUtils.showMsg("无权限")
                        }
                    }
                })
        }
    }

    private fun switchDialogShow(title: String, msg: String): SwitchDialog {
        val switchDialog = SwitchDialog(this)
        switchDialog.title!!.text = title
        switchDialog.msg!!.text = msg
        switchDialog.other!!.visibility = View.GONE
        switchDialog.ok!!.text = UUtils.getString(R.string.确定)
        switchDialog.cancel!!.text = UUtils.getString(R.string.取消)
        switchDialog.show()
        return switchDialog
    }


}
