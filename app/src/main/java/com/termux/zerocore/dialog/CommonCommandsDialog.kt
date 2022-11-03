package com.termux.zerocore.dialog

import android.content.Context
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.ub.util.custom.dialog.BaseDialogDown
import com.example.xh_lib.utils.UUtils
import com.termux.R
import com.termux.app.TermuxActivity
import com.termux.zerocore.bean.ClipboardBean
import com.termux.zerocore.bean.ItemMenuBean
import com.termux.zerocore.dialog.adapter.CommonCommandsAdapter
import com.termux.zerocore.dialog.adapter.ItemMenuAdapter
import com.termux.zerocore.utils.FileIOUtils
import com.termux.zerocore.utils.WindowsUtils.getGridNumber

class CommonCommandsDialog : BaseDialogDown {

    object CommonCommandsDialogConstant{
        public val VIDEO_KEY = 1000
        public val KEYBOARD_KEY = 1001
    }

    private val CLIPBOARD_SELECT = 0
    private val OTHER_SELECT = 1

    private var recycler_view:RecyclerView? = null
    private var item_menu_rec:RecyclerView? = null
    private var clipboard_note:TextView? = null
    private var clear_text:TextView? = null
    private var select_1_ll:LinearLayout? = null
    private var select_2_ll: LinearLayout? = null
    private var clipboard_container: LinearLayout? = null
    private var other_container: LinearLayout? = null
    private var mItemMenuAdapter: ItemMenuAdapter? = null
    private var mCommonDialogListener: ItemMenuAdapter.CommonDialogListener? = null

    constructor(context: Context) : super(context)
    constructor(context: Context, themeResId: Int) : super(context, themeResId)


    override fun initViewDialog(mView: View) {
        initView(mView)
        initAdapter()
        initClick()
    }
    private fun initView(mView: View) {
        recycler_view = mView.findViewById(R.id.recycler_view)
        clipboard_note = mView.findViewById(R.id.clipboard_note)
        clear_text = mView.findViewById(R.id.clear_text)
        select_1_ll = mView.findViewById(R.id.select_1_ll)
        select_2_ll = mView.findViewById(R.id.select_2_ll)
        clipboard_container = mView.findViewById(R.id.clipboard_container)
        other_container = mView.findViewById(R.id.other_container)
        item_menu_rec = mView.findViewById(R.id.item_menu_rec)

    }

    private fun initMenuData() {
        var mList:ArrayList<ItemMenuBean.Data> = ArrayList()
        /**
         * 视屏背景
         */
        var mVideoBackData: ItemMenuBean.Data = ItemMenuBean.Data()
        mVideoBackData.title = UUtils.getString(R.string.vedio_select_name)
        mVideoBackData.id = R.mipmap.video_img_menu
        mVideoBackData.key = CommonCommandsDialogConstant.VIDEO_KEY
        mList.add(mVideoBackData)

/*        *//**
         * 内置键盘
         *//*

        var mKeyData: ItemMenuBean.Data = ItemMenuBean.Data()
        mKeyData.title = UUtils.getString(R.string.keyboard_select_name)
        mKeyData.id = R.mipmap.keyboard_img_menu
        mKeyData.key = CommonCommandsDialogConstant.KEYBOARD_KEY
        mList.add(mKeyData)*/

        mItemMenuAdapter = ItemMenuAdapter(mList, mContext)
        mItemMenuAdapter?.setCommonDialogListener(mCommonDialogListener)
        val gridLayoutManager = GridLayoutManager(UUtils.getContext(), getGridNumber())
        item_menu_rec?.layoutManager = gridLayoutManager
        item_menu_rec?.adapter = mItemMenuAdapter

    }

    public fun setCommonDialogListener(mCommonDialogListener: ItemMenuAdapter.CommonDialogListener) {
        this.mCommonDialogListener = mCommonDialogListener
    }
    private fun initClick() {
        select_1_ll?.setOnClickListener {
            selectIndex(CLIPBOARD_SELECT)
        }
        select_2_ll?.setOnClickListener {
            selectIndex(OTHER_SELECT)
        }
    }

    private fun initAdapter() {
        val clipBoardData = FileIOUtils.getClipBoardData()
        clipBoardData?.let {
            if (it.isNotEmpty()) {
                clipboard_note?.visibility = View.GONE
                recycler_view?.visibility = View.VISIBLE
                val arrayList = ArrayList<ClipboardBean.Clipboard>()
                arrayList.addAll(it)
                val mCommonCommandsAdapter = CommonCommandsAdapter(arrayList)
                recycler_view?.adapter = mCommonCommandsAdapter
                val linearLayoutManager = LinearLayoutManager(UUtils.getContext())
                linearLayoutManager.orientation = LinearLayoutManager.VERTICAL
                recycler_view?.layoutManager = linearLayoutManager
                mCommonCommandsAdapter.setNoDataListener(object : CommonCommandsAdapter.NoDataListener {
                    override fun noData() {
                        clipboard_note?.visibility = View.VISIBLE
                        recycler_view?.visibility = View.INVISIBLE
                    }
                })
                mCommonCommandsAdapter.setClickDataListener(object :CommonCommandsAdapter.ClickDataListener {
                    override fun data(data: String) {
                        TermuxActivity.mTerminalView.sendTextToTerminal(data)
                        dismiss()
                    }
                })
            }
        }

        clear_text?.let {
            it.setOnClickListener {
                FileIOUtils.clearClipBoardString()
                clipboard_note?.visibility = View.VISIBLE
                recycler_view?.visibility = View.INVISIBLE
            }
        }
    }

    override fun getContentView(): Int {
        return R.layout.dialog_common_command
    }

    private fun selectIndex(index: Int) {
        select_1_ll?.setBackgroundResource(R.drawable.shape_line_2e84e6)
        select_2_ll?.setBackgroundResource(R.drawable.shape_line_2e84e6)
        when (index) {
            CLIPBOARD_SELECT ->{
                select_1_ll?.setBackgroundResource(R.drawable.shape_line_8cff5a)
                clipboard_container?.visibility = View.VISIBLE
                other_container?.visibility = View.INVISIBLE
            }
            OTHER_SELECT->{
                select_2_ll?.setBackgroundResource(R.drawable.shape_line_8cff5a)
                clipboard_container?.visibility = View.INVISIBLE
                other_container?.visibility = View.VISIBLE
                initMenuData()
            }
        }
    }
}
