package com.termux.zerocore.settings

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.cardview.widget.CardView
import com.example.xh_lib.utils.UUtils
import com.termux.R

class ZTAboutActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ztabout)
        findViewById<CardView>(R.id.zt_termux_github_card_view).setOnClickListener {
            UUtils.startUrl("https://github.com/hanxinhao000/ZeroTermux")
        }

        findViewById<CardView>(R.id.zt_core_manage_termux_github_card_view).setOnClickListener {
            UUtils.startUrl("https://github.com/hanxinhao000/ZeroCoreManage")
        }
    }
}
