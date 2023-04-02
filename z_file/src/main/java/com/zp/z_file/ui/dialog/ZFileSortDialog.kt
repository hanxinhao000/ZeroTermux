package com.zp.z_file.ui.dialog

import android.app.Dialog
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioGroup
import com.zp.z_file.R
import com.zp.z_file.common.ZFileManageDialog
import com.zp.z_file.content.setNeedWH
import com.zp.z_file.databinding.DialogZfileSortBinding

internal class ZFileSortDialog : ZFileManageDialog(), RadioGroup.OnCheckedChangeListener {

    companion object {
        fun newInstance(sortSelectId: Int, sequenceSelectId: Int) = ZFileSortDialog().apply {
            arguments = Bundle().run {
                putInt("sortSelectId", sortSelectId)
                putInt("sequenceSelectId", sequenceSelectId)
                this
            }
        }
    }

    private var vb: DialogZfileSortBinding? = null

    private var sortSelectId = 0
    private var sequenceSelectId = 0

    var checkedChangedListener: ((Int, Int) -> Unit)? = null

    override fun create(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        vb = DialogZfileSortBinding.inflate(inflater, container, false)
        return vb?.root
    }

    override fun getContentView() = R.layout.dialog_zfile_sort

    override fun createDialog(savedInstanceState: Bundle?) = Dialog(context!!, R.style.ZFile_Common_Dialog).apply {
        window?.setGravity(Gravity.CENTER)
    }

    override fun init(savedInstanceState: Bundle?) {
        sortSelectId = arguments?.getInt("sortSelectId", 0) ?: 0
        sequenceSelectId = arguments?.getInt("sequenceSelectId", 0) ?: 0
        check()
        when (sortSelectId) {
            R.id.zfile_sort_by_default -> vb?.zfileSortByDefault?.isChecked = true
            R.id.zfile_sort_by_name -> vb?.zfileSortByName?.isChecked = true
            R.id.zfile_sort_by_date -> vb?.zfileSortByDate?.isChecked = true
            R.id.zfile_sort_by_size -> vb?.zfileSortBySize?.isChecked = true
            else -> vb?.zfileSortByDefault?.isChecked = true
        }
        when (sequenceSelectId) {
            R.id.zfile_sequence_asc -> vb?.zfileSequenceAsc?.isChecked = true
            R.id.zfile_sequence_desc -> vb?.zfileSequenceDesc?.isChecked = true
            else -> vb?.zfileSequenceAsc?.isChecked = true
        }
        vb?.zfileSortGroup?.setOnCheckedChangeListener(this)
        vb?.zfileSequenceGroup?.setOnCheckedChangeListener(this)
        vb?.zfileDialogSortCancel?.setOnClickListener { dismiss() }
        vb?.zfileDialogSortDown?.setOnClickListener {
            checkedChangedListener?.invoke(sortSelectId, sequenceSelectId)
            dismiss()
        }
    }

    override fun onCheckedChanged(group: RadioGroup?, checkedId: Int) {
        if (group?.id == R.id.zfile_sortGroup) { // 方式
            sortSelectId = checkedId
            check()
        } else { // 顺序
            sequenceSelectId = checkedId
        }
    }

    private fun check() {
        vb?.zfileSequenceLayout?.visibility = if (sortSelectId == R.id.zfile_sort_by_default) View.GONE else View.VISIBLE
    }

    override fun onStart() {
        super.onStart()
        setNeedWH()
    }

    override fun onDestroyView() {
        vb = null
        super.onDestroyView()
    }
}