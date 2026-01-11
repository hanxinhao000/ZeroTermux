package com.termux.zerocore.config.ztcommand.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.xh_lib.activity.ScanActivity
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.termux.zerocore.config.ztcommand.config.QRCodeEncoderConfig

class SocketBaseActivity : AppCompatActivity() {
    companion object {
        val TAG: String? = SocketBaseActivity::class.simpleName
        const val OPEN_TYPE: String = "open_type"
        const val OPEN_UNKNOWN: Int = -1
        const val OPEN_CAMERA_QR_TYPE: Int = 10000
    }

    private val mType: Int
        get() {
            if (intent == null) {
                return OPEN_UNKNOWN
            }
            val intExtra = intent.getIntExtra(OPEN_TYPE, 0)
            return intExtra
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (mType == OPEN_UNKNOWN) {
            finish()
        }

        when (mType) {
            OPEN_CAMERA_QR_TYPE -> {
                requestCamera()
            }
        }

    }

    private fun sendMessageAndFinish(code: Int, message: String?) {
        when (mType) {
            OPEN_CAMERA_QR_TYPE -> {
                QRCodeEncoderConfig.sendMessage(code, message)
            }
        }
        finish()
    }

    // 请求权限
    private fun requestCamera() {
        XXPermissions.with(this)
            .permission(Permission.CAMERA)
            .request(object : OnPermissionCallback {
                override fun onGranted(permissions: MutableList<String?>?, all: Boolean) {
                    if (!all) {
                        sendMessageAndFinish(1, "not permission camera")
                        return
                    }
                    val intent =
                        Intent(this@SocketBaseActivity.applicationContext, ScanActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    this@SocketBaseActivity.startActivityForResult(intent, OPEN_CAMERA_QR_TYPE)
                }

                override fun onDenied(permissions: MutableList<String?>?, never: Boolean) {
                    sendMessageAndFinish(1, "not permission camera")
                }
            })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.i(TAG, "onActivityResult requestCode: $requestCode ,resultCode: $resultCode ,data: $data")
    }
}
