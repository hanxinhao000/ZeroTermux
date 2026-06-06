package com.termux.zerocore.settings

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.cardview.widget.CardView
import com.example.xh_lib.utils.UUtils
import com.termux.R
import com.termux.app.activities.SettingsActivity
import com.termux.zerocore.ai.activity.MainAiSettings
import com.termux.zerocore.ai.deepseek.activity.ZeroTermuxDeepSeekSettingsActivity
import com.termux.zerocore.guide.TermuxGuideActivity
import com.termux.zerocore.guide.TermuxGuideActivity.Companion.GUIDE_CREATE_FOLDER
import com.termux.zerocore.guide.TermuxGuideActivity.Companion.GUIDE_EXTRA
import com.termux.zerocore.guide.TermuxGuideActivity.Companion.GUIDE_EXTRA_JUMP_OTHER

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
            startActivity(Intent(this, LeftMenuSettingsActivity::class.java))
        }
        findViewById<CardView>(R.id.ai).setOnClickListener {
            startActivity(Intent(this, MainAiSettings::class.java))
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
        findViewById<CardView>(R.id.save_path).setOnClickListener {
            val intent1 = Intent(this, TermuxGuideActivity::class.java)
            intent1.putExtra(GUIDE_EXTRA, GUIDE_CREATE_FOLDER)
            intent1.putExtra(GUIDE_EXTRA_JUMP_OTHER, true)
            startActivity(intent1)
        }
        setBaseTitle(UUtils.getString(R.string.zt_settings))

    }
}
