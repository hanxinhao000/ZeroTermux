package com.termux.zerocore.adb.dialog

import android.app.Activity
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Message
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.WindowManager
import android.widget.EditText
import android.widget.TextView
import com.example.xh_lib.utils.UUtils
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.termux.R
import com.termux.zerocore.adb.ADB
import kotlinx.coroutines.*


class AdbWindowsDialog  {

    private val mHandle: Handler = object: Handler(){
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            UUtils.showMsg(msg.obj as String?)
        }
    }
    fun initWindowView(mActivity: Activity) {

        val arrayList = ArrayList<String>()
        arrayList.add(Permission.SYSTEM_ALERT_WINDOW)
        val granted = XXPermissions.isGranted(mActivity.applicationContext, arrayList)
        if (!granted) {
            XXPermissions.with(mActivity)
                .permission(Permission.SYSTEM_ALERT_WINDOW)
                .request(object: OnPermissionCallback {
                    override fun onGranted(permissions: MutableList<String>?, all: Boolean) {
                        showWindowsView(mActivity)
                    }

                    override fun onDenied(permissions: MutableList<String>?, never: Boolean) {
                        super.onDenied(permissions, never)
                    }
            })
        } else {
            showWindowsView(mActivity)
        }
    }

    fun showWindowsView(mActivity: Activity) {
        val systemService = mActivity.getSystemService(Context.WINDOW_SERVICE)

        var layoutParam = WindowManager.LayoutParams().apply {
            //设置大小 自适应
            width = MATCH_PARENT
            height = WRAP_CONTENT
            type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_PHONE
            }
            gravity = Gravity.LEFT or Gravity.CENTER
            format = PixelFormat.RGBA_8888

            flags =
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL /*or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE*/
        }
        var floatRootView = LayoutInflater.from(mActivity.applicationContext).inflate(R.layout.pupu_window_adb_start, null)
        floatRootView?.setOnTouchListener(ItemViewTouchListener(layoutParam, mActivity.windowManager))
        (systemService as WindowManager).addView(floatRootView, layoutParam)

        val cancel = floatRootView.findViewById<TextView>(R.id.cancel)
        val pairingCode = floatRootView.findViewById<EditText>(R.id.pairing_code)
        val portNumber = floatRootView.findViewById<EditText>(R.id.port_number)
        val commit = floatRootView.findViewById<TextView>(R.id.commit)
        commit.setOnClickListener {
            val pairingCodeStr = pairingCode.text
            val portNumberStr = portNumber.text
            if (pairingCodeStr.isNullOrEmpty() || portNumberStr.isNullOrEmpty()) {
                UUtils.showMsg(UUtils.getString(R.string.input_content_cannot_be_empty))
                return@setOnClickListener
            }

            Thread(Runnable {
                startConnectAdb(portNumberStr.toString(), pairingCodeStr.toString(), mActivity.applicationContext)
            }).start()
        }

        cancel.setOnClickListener {
            (systemService as WindowManager).removeView(floatRootView)
            floatRootView?.setOnTouchListener(null)
            floatRootView = null
        }

    }

    private fun startConnectAdb(portNumberStr: String, pairingCodeStr: String, mContext: Context) {

        val instance = ADB.getInstance(mContext)
        if (instance.pair(portNumberStr, pairingCodeStr) && instance.initServer()) {
            instance.sendScript("ls")
            Log.d("TAG", "startConnectAdb: ok")
        } else {
            val message = Message()
            message.obj = "服务初始化失败!"
            mHandle.sendMessage(message)
            Log.d("TAG", "startConnectAdb: false")
        }
    }

    class ItemViewTouchListener(val wl: WindowManager.LayoutParams, val windowManager: WindowManager) :
        View.OnTouchListener {
        private var x = 0
        private var y = 0
        override fun onTouch(view: View, motionEvent: MotionEvent): Boolean {
            when (motionEvent.action) {
                MotionEvent.ACTION_DOWN -> {
                    x = motionEvent.rawX.toInt()
                    y = motionEvent.rawY.toInt()

                }
                MotionEvent.ACTION_MOVE -> {
                    val nowX = motionEvent.rawX.toInt()
                    val nowY = motionEvent.rawY.toInt()
                    val movedX = nowX - x
                    val movedY = nowY - y
                    x = nowX
                    y = nowY
                    wl.apply {
                        x += movedX
                        y += movedY
                    }
                    //更新悬浮球控件位置
                    windowManager?.updateViewLayout(view, wl)
                }
                else -> {

                }
            }
            return false
        }
    }
}


