package com.termux.zerocore.activity

import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableString
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.termux.R
import java.io.File

class EditTextActivity : AppCompatActivity() {
    private var mEditText: EditText? = null
    private var mSaveText: TextView? = null
    private var mCancelText: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_text)
        mEditText = findViewById(R.id.edit_text)
        mCancelText = findViewById(R.id.cancel)
        mSaveText = findViewById(R.id.ok)
        val stringExtra = intent.getStringExtra("edit_path")
        if (stringExtra == null || stringExtra.isEmpty()) {
            finish()
        }
        val file = File(stringExtra)
        if (!file.exists()) {
            finish()
        }
        val readLines = file.readLines()
        readLines.forEach {

            mEditText?.setText("${mEditText?.text}\n$it")
        }
        val mtypeFace: Typeface = Typeface.createFromAsset(assets, "font/font_termux.ttf")
        mEditText?.setTypeface(mtypeFace)
        mCancelText?.setOnClickListener {
            finish()
        }
        mSaveText?.setOnClickListener {

        }
    }
}
