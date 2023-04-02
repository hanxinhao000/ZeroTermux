package com.zp.z_file.ui.dialog

import android.app.Dialog
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.zp.z_file.R
import com.zp.z_file.common.ZFileManageDialog
import com.zp.z_file.content.*
import com.zp.z_file.databinding.DialogZfileSelectFolderBinding
import com.zp.z_file.ui.adapter.ZFileFolderAdapter
import com.zp.z_file.util.ZFileUtil

internal class ZFileSelectFolderDialog : ZFileManageDialog() {

    companion object {
        fun newInstance(type: String) = ZFileSelectFolderDialog().apply {
            arguments = Bundle().run {
                putString("type", type)
                this
            }
        }
    }

    private var vb: DialogZfileSelectFolderBinding? = null

    private var tipStr = ""
    private var filePath: String? = ""
    private var isOnlyFolder = false
    private var isOnlyFile = false
    private var folderAdapter: ZFileFolderAdapter? = null

    var selectFolder: (String.() -> Unit)? = null

    private val backList by lazy {
        ArrayList<String>()
    }

    /** 返回当前的路径 */
    private fun getThisFilePath() = if (backList.isEmpty()) null else backList[backList.size - 1]

    override fun getContentView() = R.layout.dialog_zfile_select_folder

    override fun create(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        vb = DialogZfileSelectFolderBinding.inflate(inflater, container, false)
        return vb?.root
    }

    override fun createDialog(savedInstanceState: Bundle?) =
        Dialog(context!!, R.style.ZFile_Select_Folder_Dialog).apply {
            window?.setGravity(Gravity.BOTTOM)
        }

    override fun init(savedInstanceState: Bundle?) {
        tipStr = arguments?.getString("type") ?: ZFileConfiguration.COPY
        // 先保存之前用户配置的数据
        filePath = getZFileConfig().filePath
        isOnlyFile = getZFileConfig().isOnlyFile
        isOnlyFolder = getZFileConfig().isOnlyFolder
        vb?.zfileSelectFolderClosePic?.setOnClickListener {
            dismiss()
        }
        vb?.zfileSelectFolderDownPic?.setOnClickListener {
            selectFolder?.invoke(if (getZFileConfig().filePath.isNullOrEmpty()) SD_ROOT else getZFileConfig().filePath!!)
            dismiss()
        }
        vb?.zfileSelectFolderTitle?.text = String.format("%s到根目录", tipStr)
        initRecyclerView()
    }

    private fun initRecyclerView() {
        folderAdapter = ZFileFolderAdapter(requireContext())
        folderAdapter?.itemClick = { _, _, item ->
            if (!item.isFile) {
                getZFileConfig().filePath = item.filePath
                backList.add(item.filePath)
                getData()
            }
        }
        vb?.zfileSelectFolderRecyclerView?.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = folderAdapter
        }
        getZFileConfig().apply {
            isOnlyFile = false
            isOnlyFolder = true
            filePath = ""
        }
        getData()
    }

    private fun getData() {
        val filePath = getZFileConfig().filePath
        if (filePath.isNullOrEmpty() || filePath == SD_ROOT) {
            vb?.zfileSelectFolderTitle?.text = String.format("%s到根目录", tipStr)
        } else {
            vb?.zfileSelectFolderTitle?.text = String.format("%s到%s", tipStr, filePath.toFile().name)
        }
        ZFileUtil.getList(requireContext()) {
            if (isNullOrEmpty()) {
                folderAdapter?.clear()
            } else {
                folderAdapter?.setDatas(this)
            }
        }
    }

    override fun onDestroyView() {
        vb = null
        super.onDestroyView()
    }

    override fun onBackPressed(): Boolean {
        val path = getThisFilePath()
        if (path == SD_ROOT || path.isNullOrEmpty()) { // 根目录
            dismiss()
        } else { // 返回上一级
            backList.removeAt(backList.size - 1)
            getZFileConfig().filePath = getThisFilePath()
            getData()
        }
        return true
    }

    override fun dismiss() {
        resetData()
        super.dismiss()
    }

    private fun resetData() {
        // 恢复之前用户配置的数据
        getZFileConfig().filePath = filePath
        getZFileConfig().isOnlyFile = isOnlyFile
        getZFileConfig().isOnlyFolder = isOnlyFolder
    }

    override fun onStart() {
        val display = context!!.getZDisplay()
        dialog?.window?.setLayout(display[0], display[1])
        super.onStart()
    }


}