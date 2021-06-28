package com.termux.zerocore.dialog

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Message
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.arialyy.aria.core.Aria
import com.arialyy.aria.core.common.AbsEntity
import com.arialyy.aria.core.task.DownloadTask
import com.arialyy.aria.util.CommonUtil
import com.blockchain.ub.util.custom.dialog.BaseDialogDown
import com.blockchain.ub.utils.httputils.BaseHttpUtils
import com.blockchain.ub.utils.httputils.HttpResponseListenerBase
import com.daimajia.numberprogressbar.NumberProgressBar
import com.example.xh_lib.utils.SaveData
import com.example.xh_lib.utils.UUtils
import com.google.gson.Gson
import com.lzy.okgo.model.Response
import com.termux.R
import com.termux.zerocore.activity.BackNewActivity
import com.termux.zerocore.bean.Data
import com.termux.zerocore.bean.ZDYDataBean
import com.termux.zerocore.url.FileUrl
import com.termux.zerocore.utils.DownLoadMuTILS
import java.io.File
import java.util.*
import kotlin.collections.ArrayList


class DownLoadDialogBoom : BaseDialogDown, DownLoadMuTILS.DownLoadMuTILSListener {

    private var recycler_view:RecyclerView? = null
    private var service_name:TextView? = null
    private var pro:ProgressBar? = null

    private var mArray: ArrayList<Data>? = null
    private var mDownLoadAdapter: DownLoadAdapter? = null
    private var mDownLoadMuTILS: DownLoadMuTILS? = null
    private var ip: String = ""
    constructor(context: Context) : super(context)
    constructor(context: Context, themeResId: Int) : super(context, themeResId)

    override fun initViewDialog(mView: View) {
        recycler_view = mView.findViewById(R.id.recycler_view)
        service_name = mView.findViewById(R.id.service_name)
        pro = mView.findViewById(R.id.pro)
        mDownLoadMuTILS = DownLoadMuTILS()
        mDownLoadMuTILS?.register()
        mDownLoadMuTILS?.setDownLoadMuTILSListener(this)
        initAdapter()

    }

    override fun getContentView(): Int {

        return R.layout.dialog_text_download
    }


    class DownLoadAdapter : RecyclerView.Adapter<DownLoadViewHolder>{

        private var mList:ArrayList<com.termux.zerocore.bean.Data>? = null
        private var ip:String? = null
        private var mDownLoadDialogBoom:DownLoadDialogBoom? = null

        constructor(mList:ArrayList<com.termux.zerocore.bean.Data>?,mDownLoadDialogBoom:DownLoadDialogBoom) : super(){

            this.mList = mList
            this.mDownLoadDialogBoom = mDownLoadDialogBoom


        }

        public fun setIp(ip:String?){

            this.ip = ip


        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DownLoadViewHolder {

            return DownLoadViewHolder(UUtils.getViewLayViewGroup(R.layout.item_download,parent))
        }

        override fun onBindViewHolder(holder: DownLoadViewHolder, position: Int) {

            val data = mList!![position]
            holder.name?.text = data.name
            val file = File("${FileUrl.zeroTermuxData}/${data.fileName}")
            if(file.exists()){
                holder.size?.text = "${UUtils.getString(R.string.大小)}:${data.size}"
            }else{

                if(data.speed != null && data.speed.isNotEmpty()){
                    holder.size?.text = "${UUtils.getString(R.string.大小)}:(${data.convertCurrentProgress}/${data.size})[${data.speed}] ${UUtils.getString(R.string.剩余)}:${data.timeLeft}"
                }else{
                    holder.size?.text = "${UUtils.getString(R.string.大小)}:${data.size}"
                }
            }


            holder.note?.text = data.note

            if(file.exists()){
                holder.number_progress_bar?.visibility = View.GONE
                holder.download?.setImageResource(R.mipmap.huifu_download)
            }else{
                if(data.isRun){
                    holder.download?.setImageResource(R.mipmap.jixu_download)
                }else{
                    holder.download?.setImageResource(R.mipmap.download)
                }
            }


            /**
             *
             * 下载失败
             *
             */

            if(data.isFail){

                holder.download?.setImageResource(R.mipmap.restart)

            }






            holder.number_progress_bar?.progress = data.progress
            holder.download?.setOnClickListener {

                if(file.exists()){
                    val activity = mDownLoadDialogBoom!!.mContext as Activity
                    activity.startActivity(Intent(activity,BackNewActivity::class.java))

                }else{

                    if(data.isFail){

                        holder.download?.setImageResource(R.mipmap.jixu_download)
                        data.isRun = true
                        data.isFail = false


                        val stringOther = SaveData.getStringOther("${FileUrl.zeroTermuxData}/${data.fileName}")





                        if(stringOther != null && stringOther.isNotEmpty() && stringOther != "def" ) {

                            try {

                                val toLong = stringOther.toLong()

                                data.id = toLong

                                Aria.download(mDownLoadDialogBoom).load(data.id).resume()

                            } catch (e: Exception) {
                                e.printStackTrace()

                                data.id = Aria.download(mDownLoadDialogBoom)
                                    .load("$ip${data.download}") //读取下载地址
                                    .setFilePath("${FileUrl.zeroTermuxData}/${data.fileName}") //设置文件保存的完整路径
                                    .create()
                                SaveData.saveStringOther("${FileUrl.zeroTermuxData}/${data.fileName}", "${data.id}")
                            }
                        }


                            return@setOnClickListener

                    }

                    if(data.isRun){
                        data.isRun = false
                        data.isFail = false
                        holder.download?.setImageResource(R.mipmap.download)
                        Aria.download(this).load(data.id).stop()

                        UUtils.showLog("任务状态-----------恢复的ID:${data.id}")
                    }else{
                        holder.download?.setImageResource(R.mipmap.jixu_download)
                        data.isRun = true


                        val stringOther = SaveData.getStringOther("${FileUrl.zeroTermuxData}/${data.fileName}")





                        if(stringOther != null && stringOther.isNotEmpty() && stringOther != "def" ){

                            try {

                                val toLong = stringOther.toLong()

                                data.id = toLong

                                Aria.download(mDownLoadDialogBoom).load(data.id).resume()

                            }catch (e:Exception){
                                e.printStackTrace()

                                data.id = Aria.download(mDownLoadDialogBoom)
                                    .load("$ip${data.download}") //读取下载地址
                                    .setFilePath("${FileUrl.zeroTermuxData}/${data.fileName}") //设置文件保存的完整路径
                                    .create()
                                SaveData.saveStringOther("${FileUrl.zeroTermuxData}/${data.fileName}","${data.id}")
                            }


                        }else{


                            data.id = Aria.download(mDownLoadDialogBoom)
                                .load("$ip${data.download}") //读取下载地址
                                .setFilePath("${FileUrl.zeroTermuxData}/${data.fileName}") //设置文件保存的完整路径
                                .create()
                            SaveData.saveStringOther("${FileUrl.zeroTermuxData}/${data.fileName}","${data.id}")
                            UUtils.showLog("任务状态------------创建的ID:${data.id}")
                        }


                    }
                }



              //  mDownLoadDialogBoom!!.taskSynchronization()
              //  val taskList = Aria.download(mDownLoadDialogBoom).taskList;
              //  UUtils.showLog("任务状态:${taskList}")


            }


            UUtils.showLog("列表状态:${data.mDownloadEntity}")
            if(data.mDownloadEntity != null && !data.isRun){
                val progress: Long = data.mDownloadEntity.currentProgress
                val formatFileSizeProgress = CommonUtil.formatFileSize(progress.toDouble())
                holder.number_progress_bar?.progress = data.mDownloadEntity.percent
                holder.size?.text = "${UUtils.getString(R.string.大小)}:(${formatFileSizeProgress}/${data.size})"
            }




        }




        override fun getItemCount(): Int {

            return mList!!.size
        }
    }

    override fun dismiss() {
        super.dismiss()
        mDownLoadMuTILS?.unRegister()
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


    private fun initAdapter(){

        mArray = ArrayList()
        mDownLoadAdapter = DownLoadAdapter(mArray,this)

        recycler_view?.layoutManager = LinearLayoutManager(UUtils.getContext())
        recycler_view?.adapter = mDownLoadAdapter

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
                    mArray?.clear()
                    mArray?.addAll(serviceName.data)
                    this@DownLoadDialogBoom.ip = serviceName.ip
                    taskSynchronization()
                    mDownLoadAdapter?.setIp(serviceName.ip)
                    mDownLoadAdapter?.notifyDataSetChanged()



                } catch (e: Exception) {
                    e.printStackTrace()
                    UUtils.showMsg(UUtils.getString(R.string.服务器数据格式不正确))
                    dismiss()
                }
            }

            override fun onFailure(response: Response<String>?, msg: String, mWhat: Int) {
                pro?.visibility = View.GONE
                UUtils.showMsg(UUtils.getString(R.string.无法连接到自定义服务器))
            }
        }, HashMap(), 5555)
    }

    //处理任务同步状态

    public fun taskSynchronization(){


        val tempList = ArrayList<AbsEntity>()

        val taskList = Aria.download(this).totalTaskList

        if(taskList == null || taskList.isEmpty()){
            return
        }

        for (j in 0 until taskList.size){





            val size: Long = taskList[j].getFileSize()
            val progress: Long = taskList[j].currentProgress

            val current = if (size == 0L) 0 else (progress * 100 / size).toInt()
            val formatFileSize = CommonUtil.formatFileSize(size.toDouble())
            val formatFileSizeProgress = CommonUtil.formatFileSize(progress.toDouble())

            if(formatFileSize != formatFileSizeProgress){
                tempList.add(taskList[j])
            }
            UUtils.showLog("任务状态[离线大小]$j:${formatFileSize}---$formatFileSizeProgress")


            UUtils.showLog("任务状态[离线状态]$j:${current}")

        }


        if(tempList!= null && tempList.isNotEmpty() ){


            for (i in 0 until mArray!!.size){


                for (i1 in 0 until tempList.size){


                    if("$ip${mArray!![i].download}" == tempList[i1].key){

                        mArray!![i].mDownloadEntity = tempList[i1]

                    }


                }



            }



        }



        mDownLoadAdapter!!.notifyDataSetChanged()




    }

    override fun onTaskRunning(task: DownloadTask?) {
        if(task == null){
            return
        }

        for (i in 0 until mArray!! .size){

            //ip

                var htttIp = "$ip${mArray!![i].download}"

            UUtils.showLog("任务状态---比较:${htttIp}---${task.key}")

            if(htttIp == task.key){

                mArray!![i].progress = task.percent
                mArray!![i].speed = task.convertSpeed
                mArray!![i].timeLeft = task.convertTimeLeft
                mArray!![i].convertCurrentProgress = task.convertCurrentProgress
                mArray!![i].isRun = true

                mDownLoadAdapter!!.notifyDataSetChanged()

                return
            }


        }

    }

    override fun taskComplete(task: DownloadTask?) {
        if(task == null){
            return
        }
        SaveData.saveStringOther(task.key,"def")
        mDownLoadAdapter!!.notifyDataSetChanged()

    }

    override fun onTaskFail(task: DownloadTask?) {
        if(task == null){
            return
        }


        for (i in 0 until mArray!! .size){

            //ip

            var htttIp = "$ip${mArray!![i].download}"

            UUtils.showLog("任务状态---比较:${htttIp}---${task.key}")

            if(htttIp == task.key){

                mArray!![i].isFail = true

                mDownLoadAdapter!!.notifyDataSetChanged()

                return
            }


        }

    }


}
