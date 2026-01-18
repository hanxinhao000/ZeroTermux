package com.termux.zerocore.deepseek.activity

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.SpannableString
import android.text.Spanned
import android.text.TextUtils
import android.text.TextWatcher
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.cardview.widget.CardView
import com.example.xh_lib.utils.UUtils
import com.termux.R
import com.termux.zerocore.deepseek.model.Config
import com.termux.zerocore.ftp.utils.UserSetManage
import com.termux.zerocore.url.FileUrl
import com.termux.zerocore.utils.FileHttpUtils.Companion.get
import com.topjohnwu.superuser.Shell
import com.zp.z_file.util.LogUtils
import java.io.File

class ZeroTermuxDeepSeekSettingsActivity : AppCompatActivity() {
    companion object {
        public val TAG = ZeroTermuxDeepSeekSettingsActivity::class.java.simpleName
    }

    private val mKeyClick by lazy { findViewById<EditText>(R.id.key_click) }
    private val mDeepSeekEdit by lazy { findViewById<EditText>(R.id.deepseek_edit) }
    private val mKeyClickSummary by lazy { findViewById<TextView>(R.id.key_click_summary) }
    private val mDeepSeekKeySummary by lazy { findViewById<TextView>(R.id.deepseek_key_summary) }

    private val mAiVisibleSwitch by lazy { findViewById<SwitchCompat>(R.id.ai_visible_switch) }
    private val mAiVisibleLayout by lazy { findViewById<LinearLayout>(R.id.ai_visible_layout) }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_zero_termux_deep_seek_settings)
        initView()
        initStatus()
    }

    private fun initView() {
         setSwitchStatus(mAiVisibleSwitch, mAiVisibleLayout)

        // 设置DeepSeek蓝色点击识别
        val commandLink = UserSetManage.get().getZTUserBean().commandLink
        if (commandLink.isNullOrEmpty()) {
            mKeyClick.setText(Config.COMMANDS)
        } else {
            mKeyClick.setText(commandLink)
        }

        mKeyClick.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(
                p0: CharSequence?,
                p1: Int,
                p2: Int,
                p3: Int
            ) {

            }

            override fun onTextChanged(
                p0: CharSequence?,
                p1: Int,
                p2: Int,
                p3: Int
            ) {
                val ztUserBean = UserSetManage.get().getZTUserBean()
                var command = p0?.toString()
                if (!TextUtils.isEmpty(command) && command!!.contains("，")) {
                    command = p0?.toString()?.replace("，", ",")
                    mKeyClick.setText(command)
                    mKeyClick.setSelection(command!!.length);
                }
                ztUserBean.commandLink = command
                UserSetManage.get().setZTUserBean(ztUserBean)
            }

            override fun afterTextChanged(p0: Editable?) {

            }

        })

        // 设置 DeepSeep Key
        val deepSeekApiKey = UserSetManage.get().getZTUserBean().deepSeekApiKey
        if (!TextUtils.isEmpty(deepSeekApiKey)) {
            mDeepSeekEdit.setText(deepSeekApiKey)
        }
       mDeepSeekEdit.addTextChangedListener(object : TextWatcher {
           override fun beforeTextChanged(
               p0: CharSequence?,
               p1: Int,
               p2: Int,
               p3: Int
           ) {
           }
           override fun onTextChanged(
               p0: CharSequence?,
               p1: Int,
               p2: Int,
               p3: Int
           ) {
               val ztUserBean = UserSetManage.get().getZTUserBean()
               var deepSeekApiKey = p0?.toString()
               ztUserBean.deepSeekApiKey = deepSeekApiKey
               UserSetManage.get().setZTUserBean(ztUserBean)
           }
           override fun afterTextChanged(p0: Editable?) {
           }
       })

    }

    private fun initStatus() {
        val ztUserBean = UserSetManage.get().getZTUserBean()
        mAiVisibleSwitch.isChecked = ztUserBean.isIsDeepSeekVisibleTerminal

        mKeyClickSummary.text = getKeyClickText(UUtils.getString(R.string.deepseek_settings_recognition_edit_keyword),
            UUtils.getString(R.string.deepseek_settings_recognition_edit_info), object : ClickableSpan() {
            override fun onClick(widget: View) {
                mKeyClick.setText(Config.COMMANDS)
            }
        })

        mDeepSeekKeySummary.text = getKeyClickText(UUtils.getString(R.string.deepseek_settings_key_edit_info_keyword),
            UUtils.getString(R.string.deepseek_settings_key_edit_info), object : ClickableSpan() {
                override fun onClick(widget: View) {
                    startActivity(Intent(this@ZeroTermuxDeepSeekSettingsActivity, ZeroTermuxDeepSeekKeyActivity::class.java))
                }
            })
        mDeepSeekKeySummary.movementMethod = LinkMovementMethod.getInstance()
        mKeyClickSummary.movementMethod = LinkMovementMethod.getInstance()
    }

    private fun getKeyClickText(keyword: String, text: String, clickableSpan: ClickableSpan) :SpannableString {
        val spannableString = SpannableString(text)
        val startIndex = text.indexOf(keyword)
        val endIndex = startIndex + keyword.length

        if (startIndex != -1) {
            spannableString.setSpan(
                clickableSpan,
                startIndex,
                endIndex,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )

             spannableString.setSpan(ForegroundColorSpan(UUtils.getColor(R.color.color_48baf3)), startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            return spannableString;
        }
        return spannableString;
    }

    private fun setSwitchStatus(switchCompat: SwitchCompat, linearLayout: LinearLayout) {
        linearLayout.setOnClickListener {
            switchCompat.isChecked = !(switchCompat.isChecked)
        }
        switchCompat.setOnCheckedChangeListener { buttonView, isChecked ->
            val ztUserBean = UserSetManage.get().getZTUserBean()
            when (switchCompat) {
                mAiVisibleSwitch -> {
                     ztUserBean.isIsDeepSeekVisibleTerminal = switchCompat.isChecked
                 }

            }
            UserSetManage.get().setZTUserBean(ztUserBean)
        }
    }

}
