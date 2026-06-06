package com.termux.zerocore.settings

import android.content.Intent
import android.os.Bundle
import androidx.cardview.widget.CardView
import com.example.xh_lib.utils.UUtils
import com.termux.R

class LeftMenuSettingsActivity : BaseTitleActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_left_menu_settings)
        setBaseTitle(UUtils.getString(R.string.left_menu_settings_title))
        findViewById<CardView>(R.id.menu_update_source_entry).setOnClickListener {
            startActivity(Intent(this, MenuUpdateSourceActivity::class.java))
        }
    }
}
