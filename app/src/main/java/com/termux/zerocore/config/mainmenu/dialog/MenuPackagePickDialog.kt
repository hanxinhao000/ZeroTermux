package com.termux.zerocore.config.mainmenu.dialog

import android.content.Context
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.ub.util.custom.dialog.BaseDialogDown
import com.example.xh_lib.utils.UUtils
import com.termux.R
import com.termux.zerocore.config.mainmenu.MainMenuPackageManager
import com.termux.zerocore.config.mainmenu.view.adapter.MenuPackageZipAdapter
import java.io.File

/** Java 可调用的菜单包选择回调。 */
fun interface MenuPackagePickCallback {
    fun onPick(zipFile: File)
}

class MenuPackagePickDialog(context: Context) : BaseDialogDown(context) {

    private var pickList: RecyclerView? = null
    private var emptyView: TextView? = null
    private var onPickListener: MenuPackagePickCallback? = null

    override fun initViewDialog(mView: View) {
        pickList = mView.findViewById(R.id.menu_package_pick_list)
        emptyView = mView.findViewById(R.id.menu_package_pick_empty)
        mView.findViewById<ImageView>(R.id.close)?.setOnClickListener { dismiss() }
    }

    override fun getContentView(): Int = R.layout.dialog_menu_package_pick

    fun setOnPickListener(listener: MenuPackagePickCallback) {
        onPickListener = listener
    }

    fun refreshList() {
        val zipFiles = MainMenuPackageManager.listMenuZipFiles(context)
        if (zipFiles.isEmpty()) {
            pickList?.visibility = View.GONE
            emptyView?.visibility = View.VISIBLE
            return
        }
        emptyView?.visibility = View.GONE
        pickList?.visibility = View.VISIBLE
        val adapter = MenuPackageZipAdapter(zipFiles) { file ->
            dismiss()
            onPickListener?.onPick(file)
        }
        pickList?.layoutManager = LinearLayoutManager(context)
        pickList?.adapter = adapter
    }

    override fun show() {
        setCancelable(true)
        setCanceledOnTouchOutside(true)
        refreshList()
        super.show()
    }
}
