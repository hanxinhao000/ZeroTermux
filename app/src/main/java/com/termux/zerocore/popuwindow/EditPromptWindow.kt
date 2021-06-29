package com.termux.zerocore.popuwindow

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Message
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.ub.utils.httputils.BaseHttpUtils
import com.blockchain.ub.utils.httputils.HttpResponseListenerBase
import com.example.xh_lib.utils.SaveData
import com.example.xh_lib.utils.UUtils
import com.google.gson.Gson
import com.lzy.okgo.model.Response
import com.termux.R
import com.termux.zerocore.bean.EditPromptBean
import com.termux.zerocore.bean.ZDYDataBean
import com.termux.zerocore.dialog.DownLoadDialogBoom
import java.util.*
import kotlin.collections.HashMap

class EditPromptWindow :BasePuPuWindow {

    private var recycler_view:RecyclerView? = null
    private var mList: ArrayList<EditPromptBean.EditPromptData>? = null
    private var mEditPromptAdapter: EditPromptAdapter? = null
    private var del_img: ImageView? = null

    @SuppressLint("HandlerLeak")
    private val mHandler: Handler = object : Handler() {

        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)

            var i = msg.obj as Int
            UUtils.showLog("连接测试:开始$i")
            BaseHttpUtils().getUrl("${mList!![i].ip}/repository/main.json",object :HttpResponseListenerBase{
                override fun onSuccessful(msg: Message, mWhat: Int) {
                    try {
                        val test = Gson().fromJson(msg.obj as String, ZDYDataBean::class.java)
                        UUtils.showLog("连接测试:成功")
                        mList!![i].connection = 1
                    } catch (e: Exception) {
                        e.printStackTrace()
                        mList!![i].connection = 2
                        UUtils.showLog("连接测试:失败")
                    }

                    mEditPromptAdapter!!.notifyDataSetChanged()


                }

                override fun onFailure(response: Response<String>?, msg: String, mWhat: Int) {
                    try {
                        UUtils.showLog("连接测试:失败")
                        mList!![i].connection = 2
                        mEditPromptAdapter!!.notifyDataSetChanged()
                    }catch (e:Exception){
                        e.printStackTrace()
                    }


                }
            }, HashMap(),555)

            if(i < mList!!.size - 1){

                i+=1

                val message = Message()

                message.obj = i


                mHandler2.sendMessage(message)

            }

        }
    }


    @SuppressLint("HandlerLeak")
    private val mHandler2: Handler = object : Handler() {

        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)

            var i = msg.obj as Int
            UUtils.showLog("连接测试:开始$i")
            BaseHttpUtils().getUrl("${mList!![i].ip}/repository/main.json",object :HttpResponseListenerBase{
                override fun onSuccessful(msg: Message, mWhat: Int) {
                    try {
                        val test = Gson().fromJson(msg.obj as String, ZDYDataBean::class.java)
                        mList!![i].connection = 1
                        UUtils.showLog("连接测试:成功")
                    } catch (e: Exception) {
                        e.printStackTrace()
                        mList!![i].connection = 2
                        UUtils.showLog("连接测试:失败")
                    }
                    mEditPromptAdapter!!.notifyDataSetChanged()


                }

                override fun onFailure(response: Response<String>?, msg: String, mWhat: Int) {
                    try {
                        UUtils.showLog("连接测试:失败")
                        mList!![i].connection = 2
                        mEditPromptAdapter!!.notifyDataSetChanged()
                    }catch (e:Exception){
                        e.printStackTrace()
                    }

                }
            }, HashMap(),555)

            if(i < mList!!.size - 1){

                i+=1

                val message = Message()

                message.obj = i


                mHandler.sendMessage(message)

            }


        }
    }

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    override fun initView(mView: View) {
        index = 0
        recycler_view = mView.findViewById(R.id.recycler_view)
        initAdapter()

    }

    override fun getViewId(): Int {

        return R.layout.popu_window_edit_prompt
    }

    private fun initAdapter(){

        mList = ArrayList()

        mEditPromptAdapter = EditPromptAdapter(mList)

        recycler_view?.layoutManager = LinearLayoutManager(UUtils.getContext())

        recycler_view?.adapter = mEditPromptAdapter

    }

    private var mEditPromptWindowListener:EditPromptWindowListener? = null

    public fun setEditPromptWindowListener(mEditPromptWindowListener:EditPromptWindowListener){

        this.mEditPromptWindowListener = mEditPromptWindowListener

    }
    public interface EditPromptWindowListener{


        fun itemClick(data:EditPromptBean.EditPromptData)

    }

    override fun dismiss() {
        super.dismiss()
        mHandler.removeCallbacksAndMessages(null)
        mHandler2.removeCallbacksAndMessages(null)
        index = 999999
    }

    public fun setList(mList: ArrayList<EditPromptBean.EditPromptData>){

        this.mList!!.addAll(mList)
        mEditPromptAdapter!!.notifyDataSetChanged()

        mEditPromptAdapter?.setEditPromptListener(object : EditPromptAdapter.EditPromptListener{
            override fun clickItem(data: EditPromptBean.EditPromptData) {
                UUtils.showLog("点击事件(内):${data.ip}---${mEditPromptWindowListener}")
                mEditPromptWindowListener?.itemClick(data)
            }


        })

        testConnection()

    }

    private var index = 0

    public fun testConnection(){


        val message = Message()

        message.obj = index

        mHandler.sendMessage(message)


    }

    class EditPromptAdapter : RecyclerView.Adapter<EditPromptViewHolder>{

        private var mList: ArrayList<EditPromptBean.EditPromptData>? = null

        constructor(mList: ArrayList<EditPromptBean.EditPromptData>?) : super(){
            this.mList = mList
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EditPromptViewHolder {

            return EditPromptViewHolder(UUtils.getViewLayViewGroup(R.layout.item_edit_prompt,parent))
        }

        override fun onBindViewHolder(holder: EditPromptViewHolder, position: Int) {

            val editPromptData = mList!![position]

            holder.title?.text = editPromptData.ip

            holder.itemView.setOnClickListener {

                mEditPromptListener?.clickItem(editPromptData)

            }

            holder.del_img?.setOnClickListener {


                del(editPromptData.ip)

                notifyDataSetChanged()

            }

            UUtils.showLog("连接测试(列表刷新):${editPromptData.connection}")
            if(editPromptData.connection == 0){
                holder.img?.visibility = View.GONE
                holder.pro?.visibility = View.GONE
                holder.pro?.visibility = View.VISIBLE

            }else if(editPromptData.connection == 1){

                holder.img?.visibility = View.GONE
                holder.pro?.visibility = View.GONE
                holder.img?.visibility = View.VISIBLE

                holder.img?.setImageResource(R.drawable.shape_red_g)
            } else if(editPromptData.connection == 2){

                holder.img?.visibility = View.GONE
                holder.pro?.visibility = View.GONE
                holder.img?.visibility = View.VISIBLE

                holder.img?.setImageResource(R.drawable.shape_red_red)


            }


        }

        override fun getItemCount(): Int {

            return mList!!.size
        }

        private var mEditPromptListener:EditPromptListener? = null

        public fun setEditPromptListener(mEditPromptListener:EditPromptListener){

            this.mEditPromptListener = mEditPromptListener

        }

        public interface EditPromptListener{


            fun clickItem(data : EditPromptBean.EditPromptData)

        }

        private fun del(ip:String){


            val ip_save = SaveData.getStringOther("ip_save")

            if(ip_save == null || ip_save.isEmpty() || ip_save == "def"){

                return
            }else{

                try {

                    val fromJson = Gson().fromJson<EditPromptBean>(ip_save, EditPromptBean::class.java)

                    if(fromJson.arrayList.isEmpty()){
                        return
                    }

                    val arrayList = fromJson.arrayList

                    for (i in 0 until mList!!.size){

                        if(ip == mList!![i].ip){
                            mList!!.removeAt(i)
                            break
                        }


                    }


                    for (i in 0 until arrayList.size){

                        UUtils.showLog("删除存入:${ip}------${arrayList[i].ip}")
                        if(ip == arrayList[i].ip){
                            arrayList.removeAt(i)


                            fromJson.arrayList = arrayList

                            val toJson = Gson().toJson(fromJson)

                            UUtils.showLog("删除存入:${toJson}")
                            SaveData.saveStringOther("ip_save",toJson)

                            return
                        }


                    }



                }catch (e:Exception){
                    e.printStackTrace()

                }


            }


        }
    }

    class EditPromptViewHolder : RecyclerView.ViewHolder{

        public var title:TextView? = null
        public var img:ImageView? = null
        public var del_img:ImageView? = null
        public var pro:ProgressBar? = null

        constructor(itemView: View) : super(itemView){

            title = itemView.findViewById(R.id.title)
            img = itemView.findViewById(R.id.img)
            del_img = itemView.findViewById(R.id.del_img)
            pro = itemView.findViewById(R.id.pro)


        }
    }


}
