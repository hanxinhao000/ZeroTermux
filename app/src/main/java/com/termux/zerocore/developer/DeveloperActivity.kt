package com.termux.zerocore.developer

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.system.Os
import android.widget.Button
import com.termux.R
import com.termux.zerocore.url.FileUrl
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
    }
}
