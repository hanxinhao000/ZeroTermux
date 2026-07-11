package com.termux.zerocore.crashhistory

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import com.example.xh_lib.utils.UUtils
import com.termux.R
import com.termux.zerocore.settings.BaseTitleActivity

class ZtCrashDetailActivity : BaseTitleActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_zt_crash_detail)
        setBaseTitle(UUtils.getString(R.string.zt_crash_history_detail_title))

        val id = intent.getStringExtra(EXTRA_CRASH_ID)
        if (id.isNullOrBlank()) {
            Toast.makeText(this, UUtils.getString(R.string.zt_crash_history_not_found), Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val record = ZtCrashHistoryStore.get(this, id)
        if (record == null) {
            Toast.makeText(this, UUtils.getString(R.string.zt_crash_history_not_found), Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val metaView = findViewById<TextView>(R.id.crash_detail_meta)
        val logView = findViewById<TextView>(R.id.crash_detail_log)

        metaView.text = getString(
            R.string.zt_crash_history_detail_meta,
            ZtCrashHistoryStore.formatTime(record.timestampMs),
            record.threadName,
            record.appVersion,
            record.exceptionClass,
            record.message
        )
        logView.text = record.stackTrace
    }

    companion object {
        const val EXTRA_CRASH_ID = "crash_id"
    }
}
