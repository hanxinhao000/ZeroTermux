package com.termux.zerocore.developer

import android.os.Bundle
import android.system.Os
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.xh_lib.utils.UUtils
import com.termux.R
import com.termux.zerocore.url.FileUrl
import org.alfresco.jlan.server.config.ServerConfiguration
import org.alfresco.jlan.smb.server.SMBServer
import java.io.File


class DeveloperActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_developer)
        findViewById<Button>(R.id.create_files).setOnClickListener {
            val file = File(FileUrl.mainAppUrl, "files1")
            val file1 = File(FileUrl.zeroTermuxHome, "files")
            if (!file1.exists()) {
                file1.mkdirs()
            }
            try {
                Os.symlink(file1.absolutePath, file.absolutePath)
            }catch (e: Exception) {
                e.printStackTrace()
            }

        }

        findViewById<Button>(R.id.samba_create).setOnClickListener {
            try {
                val config = ServerConfiguration("Android Server")
                val server = SMBServer(config)
                server.startServer()
            } catch (e: Exception) {
                UUtils.showMsg(e.toString())
            }
        }
        findViewById<Button>(R.id.test1).setOnClickListener {
            try {
                val file =
                    File("/data/data/com.termux/files/home/ubuntu-in-termux/ubuntu-fs/etc/init.d/procps")
                Toast.makeText(this, "${file.exists()}", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                UUtils.showMsg(e.toString())
            }
        }
    }
}
