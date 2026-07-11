package com.termux.zerocore.crashhistory

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.xh_lib.utils.UUtils
import com.termux.R
import com.termux.zerocore.settings.BaseTitleActivity

class ZtCrashHistoryActivity : BaseTitleActivity(), ZtCrashHistoryAdapter.Listener {

    private lateinit var listView: RecyclerView
    private lateinit var emptyView: TextView
    private lateinit var clearAllView: TextView
    private var adapter: ZtCrashHistoryAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_zt_crash_history)
        setBaseTitle(UUtils.getString(R.string.zt_crash_history_title))

        listView = findViewById(R.id.crash_history_list)
        emptyView = findViewById(R.id.crash_history_empty)
        clearAllView = findViewById(R.id.crash_history_clear_all)

        listView.layoutManager = LinearLayoutManager(this)
        clearAllView.setOnClickListener { confirmClearAll() }
        refreshList()
    }

    override fun onResume() {
        super.onResume()
        refreshList()
    }

    private fun refreshList() {
        val items = ZtCrashHistoryStore.listSummaries(this)
        if (adapter == null) {
            adapter = ZtCrashHistoryAdapter(items, this)
            listView.adapter = adapter
        } else {
            adapter?.updateData(items)
        }
        val isEmpty = items.isEmpty()
        emptyView.visibility = if (isEmpty) View.VISIBLE else View.GONE
        listView.visibility = if (isEmpty) View.GONE else View.VISIBLE
        clearAllView.isEnabled = !isEmpty
        clearAllView.alpha = if (isEmpty) 0.4f else 1f
    }

    private fun confirmClearAll() {
        AlertDialog.Builder(this)
            .setTitle(UUtils.getString(R.string.zt_crash_history_clear_all))
            .setMessage(UUtils.getString(R.string.zt_crash_history_clear_all_confirm))
            .setIcon(R.mipmap.linux_new_ico)
            .setPositiveButton(UUtils.getString(R.string.确定)) { _, _ ->
                ZtCrashHistoryStore.clearAll(this)
                Toast.makeText(this, UUtils.getString(R.string.已完成清空), Toast.LENGTH_SHORT).show()
                refreshList()
            }
            .setNegativeButton(UUtils.getString(R.string.取消), null)
            .show()
    }

    override fun onItemClick(item: ZtCrashSummary) {
        val intent = Intent(this, ZtCrashDetailActivity::class.java)
        intent.putExtra(ZtCrashDetailActivity.EXTRA_CRASH_ID, item.id)
        startActivity(intent)
    }

    override fun onDeleteClick(item: ZtCrashSummary) {
        AlertDialog.Builder(this)
            .setTitle(UUtils.getString(R.string.删除))
            .setMessage(UUtils.getString(R.string.zt_crash_history_delete_confirm))
            .setIcon(R.mipmap.linux_new_ico)
            .setPositiveButton(UUtils.getString(R.string.确定)) { _, _ ->
                ZtCrashHistoryStore.delete(this, item.id)
                Toast.makeText(this, UUtils.getString(R.string.删除成功), Toast.LENGTH_SHORT).show()
                refreshList()
            }
            .setNegativeButton(UUtils.getString(R.string.取消), null)
            .show()
    }
}
