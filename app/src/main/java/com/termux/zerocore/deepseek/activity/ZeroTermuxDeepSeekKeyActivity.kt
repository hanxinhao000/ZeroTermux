package com.termux.zerocore.deepseek.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.xh_lib.utils.UUtils
import com.termux.R

class ZeroTermuxDeepSeekKeyActivity : AppCompatActivity() {
    private val mTitle1 by lazy { findViewById<TextView>(R.id.title_1) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_zero_termux_deep_seek_key)
        mTitle1.text = getKeyClickText(UUtils.getString(R.string.deepseek_settings_ai_get_deepseek_key_1_keyword),
            UUtils.getString(R.string.deepseek_settings_ai_get_deepseek_key_1), object : ClickableSpan() {
                override fun onClick(p0: View) {
                    val intent = Intent()
                    intent.data = Uri.parse("https://platform.deepseek.com/") //Url 就是你要打开的网址
                    intent.action = Intent.ACTION_VIEW
                    startActivity(intent) //启动浏览器
                }
            });
        mTitle1.movementMethod = LinkMovementMethod()
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
}
