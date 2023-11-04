package com.termux.zerocore.utils

import com.example.xh_lib.utils.UUtils
import com.termux.zerocore.http_service.HttpServer
import com.termux.zerocore.url.FileUrl
import com.zp.z_file.util.LogUtils
import java.io.File
import java.io.IOException

class FileHttpUtils private constructor() {
    private var mHttpServer: HttpServer? = null
    private val port: Int = 19956
    private var isServicesRun = false
    private val TAG = "FileHttpUtils"
    private val TAGFileHttpUtils = "file_http_utils"
    companion object {
        private var instance: FileHttpUtils? = null
            get() {
                if (field == null) {
                    field = FileHttpUtils()
                }
                return field
            }

        @Synchronized
        fun get(): FileHttpUtils {
            return instance!!
        }
    }

    public fun startServer() {
        Thread {
            Thread.sleep(500)
            w()
            if (mHttpServer == null) {
                mHttpServer = HttpServer(port)
            }

            mHttpServer?.let {
                if (it.isAlive) {
                    LogUtils.d(TAG, "startServer server is run return")
                    return@let
                }
                try {
                    mHttpServer?.start()
                    isServicesRun = true
                } catch (e: IOException) {
                    isServicesRun = false
                    e.printStackTrace()
                }
            }
        }.start()
    }

    public fun stopServer() {
        if (mHttpServer == null) {
            LogUtils.d(TAG, "startServer server is not run return")
            return
        }

        mHttpServer?.let {
            if (!it.isAlive) {
                LogUtils.d(TAG, "startServer server is not run return")
                return@let
            }
            try {
                mHttpServer?.stop()
                isServicesRun = false
            } catch (e: IOException) {
                isServicesRun = true
                e.printStackTrace()
            }
        }
    }
    public fun bootHttp() {
        val data = SaveData.getData(TAGFileHttpUtils)
        if (data == null || data.isEmpty() || data.equals("def")) {
            startServer()
        }
    }

    public fun isServicesRun(): Boolean {
        return isServicesRun
    }

    public fun setHttpBoot() {
        SaveData.saveData(TAGFileHttpUtils, "def")
    }
    public fun cancelHttpBoot() {
        SaveData.saveData(TAGFileHttpUtils, "boot")
    }

    public fun w() {
        Thread {
            val file = File(FileUrl.mainFilesUrl, "css1.css")
            UUtils.writerFile("css/css1.css", file)
        }.start()
    }
}
