package com.termux.zerocore.settings

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import androidx.appcompat.widget.SwitchCompat
import androidx.cardview.widget.CardView
import com.example.xh_lib.utils.UUtils
import com.termux.R
import com.termux.zerocore.ftp.utils.UserSetManage

class MenuSettingsActivity : BaseTitleActivity() {

    private val foldMenuCloseSwitch by lazy { findViewById<SwitchCompat>(R.id.fold_menu_close_switch) }
    private val foldMenuCloseLl by lazy { findViewById<LinearLayout>(R.id.fold_menu_close_ll) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu_settings)
        setBaseTitle(UUtils.getString(R.string.menu_settings_title))
        initView()
        initStatus()
    }

    private fun initView() {
        foldMenuCloseLl.setOnClickListener {
            foldMenuCloseSwitch.isChecked = !foldMenuCloseSwitch.isChecked
        }
        foldMenuCloseSwitch.setOnCheckedChangeListener { _, isChecked ->
            val ztUserBean = UserSetManage.get().getZTUserBean()
            ztUserBean.isCloseFoldMenu = isChecked
            UserSetManage.get().setZTUserBean(ztUserBean)
        }
        findViewById<CardView>(R.id.left_menu_settings_entry).setOnClickListener {
            startActivity(Intent(this, MenuUpdateSourceActivity::class.java))
        }
    }

    private fun initStatus() {
        foldMenuCloseSwitch.isChecked = UserSetManage.get().getZTUserBean().isCloseFoldMenu
    }
}
