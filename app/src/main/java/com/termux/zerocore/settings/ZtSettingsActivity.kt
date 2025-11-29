package com.termux.zerocore.settings

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.cardview.widget.CardView
import com.termux.R
import com.termux.app.activities.SettingsActivity

class ZtSettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_zt_settings)
        findViewById<CardView>(R.id.termux_settings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        findViewById<CardView>(R.id.zt_termux_settings).setOnClickListener {
            startActivity(Intent(this, ZeroTermuxSettingsActivity::class.java))
        }
        findViewById<CardView>(R.id.online_sh_server).setOnClickListener {
            startActivity(Intent(this, ZTOnlineServerActivity::class.java))
        }
        findViewById<CardView>(R.id.zt_about_card_view).setOnClickListener {
            startActivity(Intent(this, ZTAboutActivity::class.java))
        }
        findViewById<CardView>(R.id.install_card_view).setOnClickListener {
            startActivity(Intent(this, ZTInstallActivity::class.java))
        }

    }
}
