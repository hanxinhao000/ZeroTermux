package com.termux.zerocore.settings

import android.os.Bundle
import android.text.TextUtils
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.xh_lib.utils.UUtils
import com.termux.R
import com.termux.zerocore.config.mainmenu.MenuUpdateSource
import com.termux.zerocore.config.mainmenu.MenuUpdateSourceManager
import com.termux.zerocore.config.mainmenu.view.adapter.MenuUpdateSourceAdapter

class MenuUpdateSourceActivity : BaseTitleActivity(), MenuUpdateSourceAdapter.Listener {

    private var sourceList: RecyclerView? = null
    private var adapter: MenuUpdateSourceAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu_update_source)
        setBaseTitle(UUtils.getString(R.string.menu_update_source_settings_title))
        sourceList = findViewById(R.id.menu_update_source_list)
        sourceList?.layoutManager = LinearLayoutManager(this)
        refreshList()
    }

    private fun refreshList() {
        val items = MenuUpdateSourceManager.buildListItems(this)
        val selectedId = MenuUpdateSourceManager.getSelectedSourceId(this)
        if (adapter == null) {
            adapter = MenuUpdateSourceAdapter(items, selectedId, this)
            sourceList?.adapter = adapter
        } else {
            adapter?.updateData(items, selectedId)
        }
    }

    override fun onSourceSelected(source: MenuUpdateSource) {
        MenuUpdateSourceManager.setSelectedSourceId(this, source.id)
        refreshList()
    }

    override fun onAddSourceClick() {
        showUrlInputDialog(null, null)
    }

    override fun onEditSourceClick(source: MenuUpdateSource) {
        showUrlInputDialog(source.id, source.url)
    }

    override fun onDeleteSourceClick(source: MenuUpdateSource) {
        if (MenuUpdateSourceManager.deleteCustomSource(this, source.id)) {
            Toast.makeText(this, UUtils.getString(R.string.删除成功), Toast.LENGTH_SHORT).show()
            refreshList()
        }
    }

    private fun showUrlInputDialog(editSourceId: String?, initialUrl: String?) {
        val urlInput = EditText(this)
        urlInput.hint = UUtils.getString(R.string.menu_update_source_input_hint)
        if (!TextUtils.isEmpty(initialUrl)) {
            urlInput.setText(initialUrl)
            urlInput.setSelection(initialUrl!!.length)
        }
        val isEdit = !TextUtils.isEmpty(editSourceId)
        AlertDialog.Builder(this)
            .setTitle(UUtils.getString(R.string.menu_update_source_input_title))
            .setIcon(R.mipmap.linux_new_ico)
            .setView(urlInput)
            .setPositiveButton(UUtils.getString(R.string.确定)) { _, _ ->
                val url = urlInput.text.toString().trim()
                if (TextUtils.isEmpty(url)) {
                    Toast.makeText(
                        this,
                        UUtils.getString(R.string.menu_update_source_input_empty),
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setPositiveButton
                }
                val success = if (isEdit) {
                    MenuUpdateSourceManager.updateCustomSource(this, editSourceId!!, url)
                } else {
                    MenuUpdateSourceManager.addCustomSource(this, url)
                }
                if (!success) {
                    Toast.makeText(
                        this,
                        UUtils.getString(R.string.menu_update_source_input_duplicate),
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setPositiveButton
                }
                if (isEdit) {
                    Toast.makeText(this, UUtils.getString(R.string.修改成功), Toast.LENGTH_SHORT).show()
                }
                refreshList()
            }
            .setNegativeButton(UUtils.getString(R.string.取消), null)
            .show()
    }
}
