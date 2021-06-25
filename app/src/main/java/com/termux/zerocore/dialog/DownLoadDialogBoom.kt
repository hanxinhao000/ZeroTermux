package com.termux.zerocore.dialog

import android.content.Context
import android.os.Message
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.arialyy.annotations.Download
import com.arialyy.aria.core.Aria
import com.blockchain.ub.util.custom.dialog.BaseDialogDown
import com.blockchain.ub.utils.httputils.BaseHttpUtils
import com.blockchain.ub.utils.httputils.HttpResponseListenerBase
import com.daimajia.numberprogressbar.NumberProgressBar
import com.example.xh_lib.utils.UUtils
import com.google.gson.Gson
import com.lzy.okgo.model.Response
import com.termux.R
import com.termux.zerocore.bean.ZDYDataBean
import com.termux.zerocore.url.FileUrl
import java.util.*


class DownLoadDialogBoom : BaseDialogDown {

    private var recycler_view:RecyclerView? = null
    private var service_name:TextView? = null
    private var pro:ProgressBar? = null

    constructor(context: Context) : super(context)
    constructor(context: Context, themeResId: Int) : super(context, themeResId)

    override fun initViewDialog(mView: View) {
        recycler_view = mView.findViewById(R.id.recycler_view)
        service_name = mView.findViewById(R.id.service_name)
        pro = mView.findViewById(R.id.pro)
    }

    override fun getContentView(): Int {

        return R.layout.dialog_text_download
    }


    class DownLoadAdapter : RecyclerView.Adapter<DownLoadViewHolder>{

        private var mList:ArrayList<com.termux.zerocore.bean.Data>? = null
        private var ip:String? = null

        constructor(mList:ArrayList<com.termux.zerocore.bean.Data>?,ip:String?) : super(){

            this.mList = mList
            this.ip = ip

        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DownLoadViewHolder {

            return DownLoadViewHolder(UUtils.getViewLayViewGroup(R.layout.item_download,parent))
        }

        override fun onBindViewHolder(holder: DownLoadViewHolder, position: Int) {

            val data = mList!![position]
            holder.name?.text = data.name
            holder.size?.text = data.size
            holder.note?.text = data.note
            holder.download?.setOnClickListener {


                val taskId: Long = Aria.download(this)
                    .load("$ip${data.download}") //读取下载地址
                    .setFilePath("${FileUrl.zeroTermuxData}/${data.name}") //设置文件保存的完整路径
                    .create()


                Aria.download(this).register();



            }






        }




        override fun getItemCount(): Int {

            return mList!!.size
        }
    }

    //name
    class DownLoadViewHolder : RecyclerView.ViewHolder{

        public var name:TextView? = null
        public var size:TextView? = null
        public var note:TextView? = null
        public var download:ImageView? = null
        public var number_progress_bar: NumberProgressBar? = null

        constructor(itemView: View) : super(itemView){

            name = itemView.findViewById(R.id.name)
            size = itemView.findViewById(R.id.size)
            note = itemView.findViewById(R.id.note)
            download = itemView.findViewById(R.id.download)
            number_progress_bar = itemView.findViewById(R.id.number_progress_bar)

        }
    }


    public fun setIP(ip:String){


        startHttp(ip)


    }

    /**
     *
     *
     *
     * 连接到服务器
     *
     *
     */
    private fun startHttp(ip: String) {

        pro?.visibility = View.VISIBLE
        BaseHttpUtils().getUrl("$ip", object : HttpResponseListenerBase {
            override fun onSuccessful(msg: Message, mWhat: Int) {
                pro?.visibility = View.GONE
                UUtils.showLog("连接成功:" + msg.obj)
                try {
                    val serviceName = Gson().fromJson(msg.obj as String, ZDYDataBean::class.java)


                    service_name?.text = serviceName.serviceName
                    val downLoadAdapter = DownLoadAdapter(serviceName.data!!,serviceName.ip)

                    recycler_view?.layoutManager = LinearLayoutManager(UUtils.getContext())
                    recycler_view?.adapter = downLoadAdapter

                } catch (e: Exception) {
                    e.printStackTrace()
                    UUtils.showMsg(UUtils.getString(R.string.服务器数据格式不正确))
                }
            }

            override fun onFailure(response: Response<String>?, msg: String, mWhat: Int) {
                pro?.visibility = View.GONE
                UUtils.showMsg(UUtils.getString(R.string.无法连接到自定义服务器))
            }
        }, HashMap(), 5555)
    }
}
