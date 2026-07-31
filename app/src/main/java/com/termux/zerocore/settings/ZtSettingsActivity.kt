package com.termux.zerocore.settings

import android.content.Intent
import android.os.Bundle
import androidx.cardview.widget.CardView
import com.example.xh_lib.utils.UUtils
import com.termux.R
import com.termux.app.activities.SettingsActivity
import com.termux.zerocore.ai.activity.MainAiSettings
import com.termux.zerocore.ai.agent.ZtAgentAiSettingsActivity
class ZtSettingsActivity : BaseTitleActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_zt_settings)
        findViewById<CardView>(R.id.termux_settings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        findViewById<CardView>(R.id.zt_termux_settings).setOnClickListener {
            startActivity(Intent(this, ZeroTermuxSettingsActivity::class.java))
        }
        findViewById<CardView>(R.id.left_menu_settings_card).setOnClickListener {
            startActivity(Intent(this, MenuSettingsActivity::class.java))
        }
        findViewById<CardView>(R.id.ai).setOnClickListener {
            startActivity(Intent(this, MainAiSettings::class.java))
        }
        findViewById<CardView>(R.id.agent_ai_entry).setOnClickListener {
            startActivity(Intent(this, ZtAgentAiSettingsActivity::class.java))
        }
        findViewById<CardView>(R.id.developer_options_card).setOnClickListener {
            startActivity(Intent(this, ZtDeveloperOptionsActivity::class.java))
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
        setBaseTitle(UUtils.getString(R.string.zt_settings))
    }
}
