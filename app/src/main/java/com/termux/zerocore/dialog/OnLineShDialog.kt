package com.termux.zerocore.dialog

import android.content.Context
import android.os.Message
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.ub.util.custom.dialog.BaseDialogDown
import com.blockchain.ub.utils.httputils.BaseHttpUtils
import com.blockchain.ub.utils.httputils.HttpResponseListenerBase
import com.example.xh_lib.utils.UUtils
import com.google.gson.Gson
import com.lzy.okgo.model.Response
import com.termux.R
import com.termux.app.TermuxActivity
import com.termux.shared.logger.Logger
import com.termux.zerocore.bean.online_sh.Data
import com.termux.zerocore.bean.online_sh.OnLineShBean
import com.termux.zerocore.data.DownLoadShEntey
import com.termux.zerocore.http.HTTPIP
import com.termux.zerocore.url.FileUrl

class OnLineShDialog : BaseDialogDown {
    private var recycler_view:RecyclerView? = null
    private var loading_data:LinearLayout? = null
    private var show_data: RelativeLayout? = null
    private var service_name: TextView? = null
    private var mOnItemClickListener:OnItemClickListener? = null
    private val LOG_TAG = "Termux--Apk:OnLineShDialog"
    constructor(context: Context) : super(context)
    constructor(context: Context, themeResId: Int) : super(context, themeResId)


    override fun initViewDialog(mView: View) {
        recycler_view = mView.findViewById(R.id.recycler_view)
        loading_data = mView.findViewById(R.id.loading_data)
        show_data = mView.findViewById(R.id.show_data)
        service_name = mView.findViewById(R.id.service_name)

        downloadHttpData()
    }

    //下载数据
    private fun downloadHttpData(){
        Logger.logDebug(LOG_TAG, "downloadHttpData start on url: ${HTTPIP.IP}/on_line_sh/main.json")
        BaseHttpUtils().getUrl("${HTTPIP.IP}/on_line_sh/main.json",object :HttpResponseListenerBase{
            override fun onSuccessful(msg: Message, mWhat: Int) {

                try {
                    val fromJson =
                        Gson().fromJson<OnLineShBean>(msg.obj as String, OnLineShBean::class.java)
                    show_data?.visibility = View.VISIBLE
                    loading_data?.visibility = View.GONE
                    recycler_view?.layoutManager = LinearLayoutManager(this@OnLineShDialog.mContext)
                    service_name?.text = fromJson.serviceName
                    val data = fromJson.data
                    val onLineShDialogAdapter = OnLineShDialogAdapter(data,fromJson.ip)
                    recycler_view?.adapter = onLineShDialogAdapter

                    onLineShDialogAdapter.setAdapterOnStartItemClickListener(object :OnLineShDialogAdapter.AdapterOnStartItemClickListener{
                        override fun click(msg: String) {
                            show_data?.visibility = View.GONE
                            loading_data?.visibility = View.VISIBLE
                        }

                    })

                    onLineShDialogAdapter.setAdapterOnItemClickListener(object :OnLineShDialogAdapter.AdapterOnItemClickListener{
                        override fun click(msg: String) {
                            mOnItemClickListener?.click(msg)
                        }

                    })


                }catch (e:Exception){
                    e.printStackTrace()
                    UUtils.runOnUIThread {
                        UUtils.showMsg(UUtils.getString(R.string.服务器在线但数据格式出错))
                        dismiss()
                    }
                }




            }

            override fun onFailure(response: Response<String>?, msg: String, mWhat: Int) {

             UUtils.runOnUIThread {
                 UUtils.showMsg(UUtils.getString(R.string.服务器已离线))
                 dismiss()
             }
            }

        }, HashMap(),558)


    }

    override fun getContentView(): Int {
        return R.layout.dialog_on_line_sh
    }

    class OnLineShDialogAdapter : RecyclerView.Adapter<OnLineShDialogViewHolder>{

        private var data: List<Data>? = null
        private var ip: String = ""
        private val LOG_TAG = "Termux--Apk:OnLineShDialogAdapter"
        private var mAdapterOnItemClickListener: AdapterOnItemClickListener? = null
        private var mAdapterOnStartItemClickListener:AdapterOnStartItemClickListener? = null
        constructor(data: List<Data>,ip:String) : super(){
            this.data = data
            this.ip = ip
        }

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): OnLineShDialogViewHolder {

            return OnLineShDialogViewHolder(UUtils.getViewLayViewGroup(R.layout.item_on_line_sh,parent))
        }

        override fun onBindViewHolder(holder: OnLineShDialogViewHolder, position: Int) {
            val data1 = data!![position]
            if(data1.isDownload == "1"){
                holder.show_img_jz?.visibility = View.VISIBLE
            }else{
                holder.show_img_jz?.visibility = View.GONE
            }

            holder.name?.setText(data1.name)
            holder.size?.text = "${UUtils.getString(R.string.大小)}:1KB~500KB"
            holder.note?.text = data1.note
            holder.download?.setOnClickListener {

                if(data1.isDownload == "1"){
                    UUtils.showMsg(UUtils.getString(R.string.当前文件下载已被禁止))
                    return@setOnClickListener
                }
                try {
                    Logger.logDebug(LOG_TAG, "file path: ${FileUrl.mainHomeUrl}/${data1.download.split("/")[data1.download.split("/").size - 1]}")
                    DownLoadShEntey.downLoadSh("$ip${data1.download}","${FileUrl.mainHomeUrl}/${data1.download.split("/")[data1.download.split("/").size - 1]}")
                    mAdapterOnStartItemClickListener?.click("")
                    DownLoadShEntey.setDownLoadShEnteyListener(object :DownLoadShEntey.DownLoadShEnteyListener{
                        override fun downLoadFile(filePath: String) {
                            mAdapterOnItemClickListener?.click("cd ~ && chmod 777 ${data1.download.split("/")[data1.download.split("/").size - 1]} && ./${data1.download.split("/")[data1.download.split("/").size - 1]}")
                        }

                        override fun error(msg: String) {
                            UUtils.showMsg(msg)
                        }

                    })

                }catch (e:Exception){
                    UUtils.showMsg(UUtils.getString(R.string.当前文件下载已被禁止))
                }

            }



        }

        override fun getItemCount(): Int {

            return data!!.size
        }

        public fun setAdapterOnItemClickListener(mAdapterOnItemClickListener:AdapterOnItemClickListener){
            this.mAdapterOnItemClickListener = mAdapterOnItemClickListener
        }
        public fun setAdapterOnStartItemClickListener(mAdapterOnStartItemClickListener:AdapterOnStartItemClickListener){
            this.mAdapterOnStartItemClickListener = mAdapterOnStartItemClickListener
        }
        public interface AdapterOnItemClickListener{

            fun click(msg:String)

        }

        public interface AdapterOnStartItemClickListener{

            fun click(msg:String)

        }

    }

    class OnLineShDialogViewHolder : RecyclerView.ViewHolder{

        public var name:TextView? = null
        public var size:TextView? = null
        public var note:TextView? = null
        public var show_img_jz:ImageView? = null
        public var download:ImageView? = null

        constructor(itemView: View) : super(itemView){
            name = itemView.findViewById(R.id.name)
            size = itemView.findViewById(R.id.size)
            note = itemView.findViewById(R.id.note)
            show_img_jz = itemView.findViewById(R.id.show_img_jz)
            download = itemView.findViewById(R.id.download)
        }
    }

    public fun setOnItemClickListener(mOnItemClickListener:OnItemClickListener){
        this.mOnItemClickListener = mOnItemClickListener
    }

    public interface OnItemClickListener{
        fun click(msg:String)
    }

}
