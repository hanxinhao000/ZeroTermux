package com.termux.zerocore.dialog

import android.app.Activity
import android.content.Context
import android.view.View
import android.widget.RelativeLayout
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.ub.util.custom.dialog.BaseDialogDown
import com.example.xh_lib.utils.UUtils
import com.google.gson.Gson
import com.termux.R
import com.termux.zerocore.activity.adapter.MinLAdapter
import com.termux.zerocore.bean.MinLBean
import com.termux.zerocore.utils.SaveData

class BoomCommandDialog : BaseDialogDown {

    private var jiandan_mingl_click:RelativeLayout? = null
    private var recyclerView:RecyclerView? = null
    private var daoru_daochu:CardView? = null
    private var mMinLAdapter: MinLAdapter? = null
    private var minglArrayList: ArrayList<MinLBean.DataNum>? = null
    constructor(context: Context) : super(context)
    constructor(context: Context, themeResId: Int) : super(context, themeResId)

    override fun initViewDialog(mView: View) {

        jiandan_mingl_click = mView.findViewById(R.id.jiandan_mingl_click)
        recyclerView = mView.findViewById(R.id.recyclerView)
        daoru_daochu = mView.findViewById(R.id.daoru_daochu)

        initAdapter()
        viewClick()
        shuaxingML()
    }

    private fun initAdapter(){

        minglArrayList = ArrayList()
        mMinLAdapter = MinLAdapter(minglArrayList, mContext as Activity)
        recyclerView?.setLayoutManager(GridLayoutManager(UUtils.getContext(), 4))
        recyclerView?.setAdapter(mMinLAdapter)
        mMinLAdapter?.setSXListener {

            shuaxingML()
        }

    }

    private fun viewClick(){

        jiandan_mingl_click?.setOnClickListener {

            val mingLShowDialog = MingLShowDialog(mContext)
            mingLShowDialog.show()
            mingLShowDialog.setCancelable(false)

            mingLShowDialog.setAddCommitListener(object : MingLShowDialog.AddCommitListener {
                override fun commit() {
                    shuaxingML()
                }

            })

        }
        daoru_daochu?.setOnClickListener {


            val minglingDaoruDaoChuDialog = MinglingDaoruDaoChuDialog(mContext)

            minglingDaoruDaoChuDialog.show()
            minglingDaoruDaoChuDialog.setSXXXXlistener(object : MinglingDaoruDaoChuDialog.SXXXXlistener {
                override fun shuaxin() {
                    shuaxingML()
                }

            })
        }


    }

    fun shuaxingML() {
        try {
            minglArrayList!!.clear()
            val commi22: String = SaveData.getData("commi22")
            if (commi22 == null || commi22.isEmpty() || commi22 == "def") {
                return
            }
            val minLBean = Gson().fromJson(commi22, MinLBean::class.java)
            minglArrayList!!.addAll(minLBean.data.list)
            mMinLAdapter!!.notifyDataSetChanged()
        } catch (e: Exception) {
            UUtils.showMsg(UUtils.getString(R.string.配置文件出错))
            e.printStackTrace()
        }
    }

    override fun getContentView(): Int {

        return R.layout.dialog_boom_command
    }
}
