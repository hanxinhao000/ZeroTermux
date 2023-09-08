package com.termux.zerocore.developer

import android.os.Bundle
import android.system.Os
import android.util.Log

import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.xh_lib.utils.UUtils
import com.termux.R
import com.termux.zerocore.adb.dialog.AdbWindowsDialog
import com.termux.zerocore.url.FileUrl
import com.termux.zerocore.zip.ZipUtils
import com.termux.zerocore.zip.ZipUtils.ZipNameListener

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
        findViewById<Button>(R.id.test_zip).setOnClickListener {
        ZipUtils.toZip("/data/data/com.termux/files/", "/data/data/com.termux/files.zip", object : ZipNameListener{
            override fun zip(FileName: String?, size: Int, position: Int) {
                Log.d("TAG", "zipxxxxxxxxxxxxxxx FileName: $FileName")
            }

            override fun complete() {

            }

            override fun progress(size: Long, position: Long) {
                Log.d("TAG", "zipxxxxxxxxxxxxxxx  size: $size, position: $position")
            }

        });
        }
        findViewById<Button>(R.id.test_unzip).setOnClickListener {
            Log.d("TAG", "onCreate:  unzip")
            Toast.makeText(this,"....", Toast.LENGTH_LONG).show()
           ZipUtils.unZip(File("/data/data/com.termux/files.zip"), "/data/data/com.termux/files1/", object : ZipNameListener{
               override fun zip(FileName: String?, size: Int, position: Int) {

               }

               override fun complete() {

               }

               override fun progress(size: Long, position: Long) {

               }

           })
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
        findViewById<Button>(R.id.test_rlj).setOnClickListener {

            Os.symlink("/storage/emulated/0", "/data/data/com.termux/files1/files/sdcard")
        }

        findViewById<Button>(R.id.adb_connect).setOnClickListener {

            AdbWindowsDialog().initWindowView(this)
        }
    }
}
