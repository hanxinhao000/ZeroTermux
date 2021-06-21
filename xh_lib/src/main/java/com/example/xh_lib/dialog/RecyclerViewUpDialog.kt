package com.example.xh_lib.dialog


import android.content.Context
import android.graphics.Color
import android.os.Message
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.ub.util.custom.dialog.BaseDialogDown


import com.blockchain.ub.utils.httputils.BaseHttpUtils
import com.blockchain.ub.utils.httputils.HttpResponseListenerBase
import com.example.xh_lib.R
import com.example.xh_lib.utils.UUtils
import com.google.gson.Gson
import com.lzy.okgo.model.Response
import java.lang.Exception
import java.util.*


/**
 * @author ZEL
 * @create By ZEL on 2020/6/10 15:00
 **/
class RecyclerViewUpDialog : BaseDialogDown {
    constructor(context: Context) : super(context)
    constructor(context: Context, themeResId: Int) : super(context, themeResId)

    private lateinit var recycler_view: RecyclerView
    private lateinit var cancel: TextView

    private  var mSwitchDialogListener:SwitchDialogListener? = null

    override fun initViewDialog(mView: View) {

        recycler_view = mView.findViewById(R.id.recycler_view)
        cancel = mView.findViewById(R.id.cancel)

        cancel.setOnClickListener {

            dismiss()
        }

       // getServerB()
    }

    override fun getContentView(): Int {

        return R.layout.switch_b_dialog
    }

    public fun setSwitchDialogListener(mSwitchDialogListener:SwitchDialogListener){

        this.mSwitchDialogListener = mSwitchDialogListener

    }

    //获取服务端币种
    public fun setAdapter(mArrayList: ArrayList<String>) {


        val switchBAdapter = SwitchBAdapter(mArrayList,mSwitchDialogListener)

        recycler_view.layoutManager = LinearLayoutManager(mContext)
        recycler_view.adapter = switchBAdapter


    }

    //ViewHolder

    class SwitchBViewHolder : RecyclerView.ViewHolder {

        public lateinit var title: TextView

        constructor(itemView: View) : super(itemView) {

            title = itemView.findViewById(R.id.title)


        }
    }

    //Adapter

    class SwitchBAdapter : RecyclerView.Adapter<SwitchBViewHolder> {

        private lateinit var mArrayList: ArrayList<String>

        private  var mSwitchDialogListener:SwitchDialogListener? = null

        constructor(mArrayList: ArrayList<String>,mSwitchDialogListener:SwitchDialogListener?) : super(){

            this.mArrayList = mArrayList
            this.mSwitchDialogListener = mSwitchDialogListener

        }


        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SwitchBViewHolder {

            return SwitchBViewHolder(UUtils.getViewLayViewGroup(R.layout.switch_b_item, parent))
        }

        override fun getItemCount(): Int {

            return mArrayList.size
        }

        override fun onBindViewHolder(holder: SwitchBViewHolder, position: Int) {


            val s = mArrayList[position].split(",")

            if(s.size > 2){

                holder.title.setTextColor(Color.parseColor(s[2]))


            }


            holder.title.text = mArrayList[position].split(",")[0]



            holder.itemView.setOnClickListener {


                UUtils.showLog("参数1:${ mArrayList[position].split(",")[0]}")
                UUtils.showLog("参数2:${ mArrayList[position].split(",")[1]}")
                UUtils.showLog("参数3:${ mSwitchDialogListener}")
                if(mSwitchDialogListener!= null){

                    mSwitchDialogListener?.click(mArrayList[position].split(",")[1])
                }


            }

            //
        }

    }


    interface SwitchDialogListener{


        fun click(str:String)

    }
}




