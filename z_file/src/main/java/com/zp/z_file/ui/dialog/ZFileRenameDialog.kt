package com.zp.z_file.ui.dialog

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import com.zp.z_file.R
import com.zp.z_file.common.ZFileManageDialog
import com.zp.z_file.content.isNull
import com.zp.z_file.content.setNeedWH
import com.zp.z_file.content.toast
import com.zp.z_file.databinding.DialogZfileRenameBinding
import com.zp.z_file.util.ZFileLog

internal class ZFileRenameDialog : ZFileManageDialog(), Runnable {

    private var vb: DialogZfileRenameBinding? = null

    var reanameDown: (String.() -> Unit)? = null
    private var handler: Handler? = null
    private var oldName = "请输入文件名称"

    companion object {

        fun newInstance(oldName: String) = ZFileRenameDialog().apply {
            arguments = Bundle().run {
                putString("oldName", oldName)
                this
            }
        }

    }

    override fun create(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        vb = DialogZfileRenameBinding.inflate(inflater, container, false)
        return vb?.root
    }

    override fun getContentView() = R.layout.dialog_zfile_rename

    override fun createDialog(savedInstanceState: Bundle?) =
        Dialog(context!!, R.style.ZFile_Common_Dialog).apply {
            window?.setGravity(Gravity.CENTER)
        }

    override fun init(savedInstanceState: Bundle?) {
        oldName = arguments?.getString("oldName") ?: "请输入文件名称"
        handler = Handler()
        vb?.zfileDialogRenameEdit?.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_UNSPECIFIED) {
                rename()
            }
            true
        }
        vb?.zfileDialogRenameEdit?.hint = oldName
        vb?.zfileDialogRenameDown?.setOnClickListener {
            rename()
        }
        vb?.zfileDialogRenameCancel?.setOnClickListener {
            dismiss()
        }
    }

    private fun rename() {
        val newName = vb?.zfileDialogRenameEdit?.text.toString()
        if (newName.isNull()) {
            context?.toast("请输入文件名")
        } else {
            if (oldName == newName) {
                ZFileLog.e("相同名字，不执行重命名操作")
                dismiss()
            } else {
                reanameDown?.invoke(newName)
                dismiss()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        setNeedWH()
        handler?.postDelayed(this, 150)
    }

    override fun run() {
        vb?.zfileDialogRenameEdit?.requestFocus()
        val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.showSoftInput(vb?.zfileDialogRenameEdit, InputMethodManager.SHOW_IMPLICIT)
    }

    override fun onDestroyView() {
        vb = null
        closeKeyboard()
        handler?.removeCallbacks(this)
        handler?.removeCallbacksAndMessages(null)
        handler = null
        super.onDestroyView()
    }

    private fun closeKeyboard() {
        val m = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val isOpen = m.isActive
        if (isOpen) {
            m.hideSoftInputFromWindow((context as Activity).currentFocus?.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
        }
    }
}