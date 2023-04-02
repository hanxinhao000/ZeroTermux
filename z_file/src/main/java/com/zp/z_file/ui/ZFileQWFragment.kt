package com.zp.z_file.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.zp.z_file.R
import com.zp.z_file.async.ZFileQWAsync
import com.zp.z_file.content.*
import com.zp.z_file.databinding.FragmentZfileQwBinding
import com.zp.z_file.ui.adapter.ZFileListAdapter
import com.zp.z_file.util.ZFileQWUtil
import com.zp.z_file.util.ZFileUtil

internal class ZFileQWFragment : Fragment() {

    private var vb: FragmentZfileQwBinding? = null

    private var isFirstLoad = true

    private var qwFileType = ZFileConfiguration.QQ
    // 文件类型
    private var type = ZFILE_QW_PIC
    private var qwManage = false

    private var qwAdapter: ZFileListAdapter? = null

    companion object {
        fun newInstance(qwFileType: String, type: Int, isManager: Boolean) = ZFileQWFragment().apply {
            arguments = Bundle().run {
                putString(QW_FILE_TYPE_KEY, qwFileType)
                putInt("type", type)
                putBoolean("isManager", isManager)
                this
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        vb = FragmentZfileQwBinding.inflate(inflater, container, false)
        return vb?.root
    }

    private fun initAll() {
        qwFileType = arguments?.getString(QW_FILE_TYPE_KEY) ?: ZFileConfiguration.QQ
        type = arguments?.getInt("type") ?: ZFILE_QW_PIC
        initViewStub()
        initRecyclerView()
    }

    private fun initRecyclerView() {
        initAdapter()
        vb?.zfileQwRecyclerView?.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = qwAdapter
        }
        vb?.zfileQwBar?.visibility = View.VISIBLE

        val qwFileLoadListener = getZFileHelp().getQWFileLoadListener()
        val filterArray = qwFileLoadListener?.getFilterArray(type) ?: ZFileQWUtil.getQWFilterMap()[type]!!
        ZFileQWAsync(qwFileType, type, context!!) {
            vb?.zfileQwBar?.visibility = View.GONE
            if (isNullOrEmpty()) {
                qwAdapter?.clear()
                setEmptyState(View.VISIBLE)
            } else {
                qwAdapter?.setDatas(this)
                setEmptyState(View.GONE)
            }
        }.start(filterArray)
    }

    private fun initAdapter() {
        if (qwAdapter == null) {
            qwAdapter = ZFileListAdapter(context!!, true).run {
                itemClick = { v, position, item ->
                    if (getZFileConfig().clickAndAutoSelected) {
                        boxLayoutClick(position, item)
                    } else {
                        ZFileUtil.openFile(item.filePath, v)
                    }
                    getZFileHelp().getFileClickListener().itemFileClick(item, v)
                }
                qwChangeListener = { isManage, item, isSelect ->
                    if (isManage) {
                        (context as? ZFileQWActivity)?.observer(item toQWBean isSelect)
                    }
                }
                isManage = qwManage
                this
            }
        }
    }

    fun setManager(isManage: Boolean) {
        if (this.qwManage != isManage) {
            this.qwManage = isManage
            qwAdapter?.isManage = isManage
        }
    }

    fun removeLastSelectData(bean: ZFileBean?) {
        qwAdapter?.setQWLastState(bean)
    }

    fun resetAll() {
        qwManage = false
        qwAdapter?.isManage = false
    }

    override fun onResume() {
        super.onResume()
        if (isFirstLoad) {
            initAll()
            isFirstLoad = false
        }
    }

    override fun onDestroyView() {
        vb = null
        super.onDestroyView()
    }

    private var emptyView: View? = null

    private fun initViewStub() {
        vb?.zfileQwEmptyStub?.layoutResource = getFileEmptyLayoutId()
    }

    private fun setEmptyState(viewState: Int) {
        if (emptyView == null) {
            emptyView = vb?.zfileQwEmptyStub?.inflate()
            emptyView?.findViewById<ImageView>(R.id.zfile_list_emptyPic)?.setImageResource(emptyRes)
        }
        emptyView?.visibility = viewState
    }
}
