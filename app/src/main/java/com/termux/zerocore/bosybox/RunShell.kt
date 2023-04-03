package com.termux.zerocore.bosybox

import com.example.xh_lib.utils.LogUtils
import com.example.xh_lib.utils.UUtils
import java.io.File
import java.nio.charset.Charset


object RunShell {
    var TAG = "RunShell"

    /**
     * Send a raw shell command
     */
    public fun shell(redirect: Boolean, command: List<String>) {
        val processBuilder = ProcessBuilder(command)
            .directory(UUtils.getContext().filesDir)
            .apply {
                if (redirect) {
                    redirectErrorStream(true)
                }

                environment().apply {
                    put("HOME", UUtils.getContext().filesDir.path)
                    put("TMPDIR", UUtils.getContext().cacheDir.path)
                }
            }

        val start = processBuilder.start()!!
        val data = ByteArray(1024)
        var len = 0
        while (-1 != start.getInputStream().read(data).also { len = it }) {
            val str = String(data, 0, len, Charset.forName("UTF-8"))
          LogUtils.d(TAG, "shell $str")
        }
    }

}
