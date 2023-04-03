package com.termux.zerocore.activity

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.xh_lib.utils.UUtils
import com.termux.R
import kotlinx.coroutines.*
import me.testica.codeeditor.Editor
import me.testica.codeeditor.SyntaxHighlightRule
import java.io.File
import java.util.regex.Matcher
import java.util.regex.Pattern


class EditTextActivity : AppCompatActivity() {
    private var mEditText: EditText? = null
    private var mSaveText: TextView? = null
    private var mCancelText: TextView? = null
    private var editor: Editor? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_text)
        mEditText = findViewById(R.id.edit_text)
        mCancelText = findViewById(R.id.cancel)
        editor = findViewById(R.id.editor)
        mSaveText = findViewById(R.id.ok)
        val stringExtra = intent.getStringExtra("edit_path")
        if (stringExtra == null || stringExtra.isEmpty()) {
            finish()
        }
        val file = File(stringExtra)
        if (file.length() > (20 * 1024)) {
            UUtils.showMsg("文件太大!\n[file size too big]")
            finish()
            return
        }
        if (!file.exists()) {
            finish()
        }
        val readLines = file.readLines()
        val stringBuilder = StringBuilder()
        readLines.forEach {
            stringBuilder.append(it + "\n")
        }
      /*  val arrayList = ArrayList<String>()
        arrayList.add("echo")
        arrayList.add("cd")
        arrayList.add("fi")
        arrayList.add("if")
        arrayList.add("#")
        mEditText?.setText(matcherSearchTitle(UUtils.getColor(R.color.color_B87733), stringBuilder.toString(),arrayList))
        val mtypeFace: Typeface = Typeface.createFromAsset(assets, "font/font_termux.ttf")
        mEditText?.setTypeface(mtypeFace)*/

        editor!!.getNumLinesView().visibility = View.GONE
        editor!!.getEditText().setTextColor(UUtils.getColor(R.color.color_A9B7C6))
        editor!!.setSyntaxHighlightRules(
            SyntaxHighlightRule("[0-9]*", "#4A85BA"),
            SyntaxHighlightRule("'.*'", "#6A8759"),
            SyntaxHighlightRule("#.*", "#629755"),
            SyntaxHighlightRule("\\b(echo|if|fi|cd|ls|cp|mv|rm|rename|touch|ps|grep|export|then|tar|sleep|unzip|gzip|zip|chroot|chmod)\\b", "#B87733"),
            SyntaxHighlightRule("\".*\"", "#6A8759"),
        )
        MainScope().launch(Dispatchers.IO) {
            delay(300)
            withContext(Dispatchers.Main) {
                val mtypeFace: Typeface = Typeface.createFromAsset(assets, "font/font_termux.ttf")
                editor!!.setTypeface(mtypeFace)
                editor!!.setText(stringBuilder.toString())
            }
        }

        mCancelText?.setOnClickListener {
            finish()
        }
        mSaveText?.setOnClickListener {
            val toString = editor!!.getEditText().text?.toString()
            if (toString == null || toString.isEmpty()) {
               UUtils.showMsg( UUtils.getString(R.string.命令不能为空))
                return@setOnClickListener
            }
            if (UUtils.setFileString(file, toString)) {
                UUtils.showMsg( UUtils.getString(R.string.save_ok_))
                finish()
            } else {
                UUtils.showMsg( UUtils.getString(R.string.save_error_))
            }
        }
    }

    fun matcherSearchTitle(color: Int, text: String, keyword: ArrayList<String>): SpannableString {
        val s = SpannableString(text)
        for (i in keyword.indices) {
            val p: Pattern = Pattern.compile(keyword[i])
            val matcher: Matcher = p.matcher(s)
            while (matcher.find()) {
                val start: Int = matcher.start()
                val end: Int = matcher.end()
                s.setSpan(
                    ForegroundColorSpan(color), start, end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }
        return s
    }
}
