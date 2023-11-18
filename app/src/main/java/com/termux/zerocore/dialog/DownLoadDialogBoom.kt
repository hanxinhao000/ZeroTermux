package com.termux.zerocore.dialog

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Paint
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
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.lzy.okgo.model.Response
import com.termux.R
import com.termux.zerocore.activity.BackNewActivity
import com.termux.zerocore.bean.Data
import com.termux.zerocore.bean.ZDYDataBean
import com.termux.zerocore.http.HTTPIP
import com.termux.zerocore.url.FileUrl
import com.termux.zerocore.url.FileUrl.zeroTermuxApk
import com.termux.zerocore.url.FileUrl.zeroTermuxData
import com.termux.zerocore.utils.DownLoadMuTILS
import java.io.File
import java.util.*
import kotlin.collections.ArrayList


class DownLoadDialogBoom : BaseDialogDown, DownLoadMuTILS.DownLoadMuTILSListener {

    private var recycler_view:RecyclerView? = null
    private var service_name:TextView? = null
    private var pro:ProgressBar? = null
    private var chongzhi_img:ImageView? = null

    private var mArray: ArrayList<Data>? = null
    private var mDownLoadAdapter: DownLoadAdapter? = null
    private var mDownLoadMuTILS: DownLoadMuTILS? = null
    private var ip: String = ""
    //当前点击的ID
    private var clickId = 0
    constructor(context: Context) : super(context)
    constructor(context: Context, themeResId: Int) : super(context, themeResId)

    override fun initViewDialog(mView: View) {
        recycler_view = mView.findViewById(R.id.recycler_view)
        service_name = mView.findViewById(R.id.service_name)
        chongzhi_img = mView.findViewById(R.id.chongzhi_img)
        pro = mView.findViewById(R.id.pro)
        service_name?.let {
            it.paint!!.flags = Paint.UNDERLINE_TEXT_FLAG
            it.setOnClickListener {
                UUtils.startUrl(HTTPIP.IP + "/repository/main/")
            }
        }
        mDownLoadMuTILS = DownLoadMuTILS()
        mDownLoadMuTILS?.register()
        mDownLoadMuTILS?.setDownLoadMuTILSListener(this)
        initAdapter()
        createWJ()

        chongzhi_img?.setOnClickListener {


            val switchDialog = switchDialogShow(UUtils.getString(R.string.警告), UUtils.getString(R.string.确定重置所有任务))

            switchDialog.cancel!!.setOnClickListener {


                switchDialog.dismiss()


            }
            switchDialog.ok!!.setOnClickListener {

                dismiss()
                switchDialog.dismiss()
                Aria.download(this).removeAllTask(false)
                UUtils.showMsg(UUtils.getString(R.string.重置成功))



            }

        }
    }

    override fun getContentView(): Int {

        return R.layout.dialog_text_download
    }


    public fun createWJ(){


        if (!zeroTermuxData.exists()) {
            val mkdirs = zeroTermuxData.mkdirs()
            if(!mkdirs){
                UUtils.showMsg(UUtils.getString(R.string.无法创建))
                dismiss()
            }
        }
        if (!zeroTermuxApk.exists()) {
            val mkdirs = zeroTermuxApk.mkdirs()
            if(!mkdirs){
                UUtils.showMsg(UUtils.getString(R.string.无法创建1))
                dismiss()
            }
        }


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

            var file:File = File("${FileUrl.zeroTermuxData}/${data.fileName}")
            var tempFile:File = File("${FileUrl.zeroTermuxData}/${data.fileName}.0.part")
            if(data.type == "apk"){
                file = File("${FileUrl.zeroTermuxApk}/${data.fileName}")
                tempFile = File("${FileUrl.zeroTermuxApk}/${data.fileName}.0.part")
            }else{
                 file = File("${FileUrl.zeroTermuxData}/${data.fileName}")
                tempFile = File("${FileUrl.zeroTermuxData}/${data.fileName}.0.part")
            }

            UUtils.showLog("文件查看--------------------------------------------------")
            UUtils.showLog("文件查看$position:${file.absolutePath}")

            if(file.exists()){
                holder.size?.text = "${UUtils.getString(R.string.大小)}:${data.size}"
            }else{

                if(data.speed != null && data.speed.isNotEmpty()){
                    holder.size?.text = "${UUtils.getString(R.string.大小)}:(${data.convertCurrentProgress}/${data.size})[${data.speed}] ${UUtils.getString(R.string.剩余)}:${data.timeLeft}"
                }else{
                    holder.size?.text = "${UUtils.getString(R.string.大小)}:${data.size}"
                }
            }




            if(data.type == "apk"){
                holder?.show_img?.setImageResource(R.mipmap.apk_img)
            }else{
                holder?.show_img?.setImageResource(R.mipmap.zip)
            }



            holder.note?.text = data.note
            UUtils.showLog("文件查看(存在)$position:${file.exists()}")
            if(file.exists()){
                holder.number_progress_bar?.visibility = View.GONE
                if(data.type == "apk"){
                    holder.download?.setImageResource(R.mipmap.install_apk)
                }else{

                    holder.download?.setImageResource(R.mipmap.huifu_download)

                }


            }else{
                holder.number_progress_bar?.visibility = View.VISIBLE
                if(data.isRun){
                    holder.download?.setImageResource(R.mipmap.jixu_download)
                    // holder.number_progress_bar?.visibility = View.VISIBLE
                }else{
                    holder.download?.setImageResource(R.mipmap.download)
                }
            }









            holder.number_progress_bar?.progress = data.progress
            holder.download?.setOnClickListener {

                mDownLoadDialogBoom?.createWJ()

                if(data.isDownload == "1"){

                    UUtils.showMsg(UUtils.getString(R.string.暂未开放下载))
                    return@setOnClickListener

                }




                if(file.exists()){
                    val activity = mDownLoadDialogBoom!!.mContext as Activity

                    if(data.type == "apk"){
                        mDownLoadDialogBoom?.installApk(file)
                    }else{
                        activity.startActivity(Intent(activity,BackNewActivity::class.java))
                        mDownLoadDialogBoom!!.dismiss()
                    }


                }else{

                    if(data.isFail){

                        holder.download?.setImageResource(R.mipmap.jixu_download)
                        data.isRun = true
                        data.isFail = false


                        val stringOther = SaveData.getStringOther("$ip${data.download}")





                        if(stringOther != null && stringOther.isNotEmpty() && stringOther != "def") {

                            try {

                                val toLong = stringOther.toLong()

                                data.id = toLong

                                Aria.download(this).load(data.id).stop()
                                Aria.download(this).load(data.id).cancel(true);

                                if(toLong == -1L){


                                    if(data.type == "apk"){

                                        data.id = Aria.download(mDownLoadDialogBoom)
                                            .load("$ip${data.download}") //读取下载地址
                                            .setFilePath("${FileUrl.zeroTermuxApk}/${data.fileName}") //设置文件保存的完整路径
                                            .create()
                                    }else{
                                        data.id = Aria.download(mDownLoadDialogBoom)
                                            .load("$ip${data.download}") //读取下载地址
                                            .setFilePath("${FileUrl.zeroTermuxData}/${data.fileName}") //设置文件保存的完整路径
                                            .create()
                                    }


                                    holder.download?.setImageResource(R.mipmap.jiazai)

                                    if(data.id == -1L){
                                        SaveData.saveStringOther("$ip${data.download}","def")
                                    }else{
                                        SaveData.saveStringOther("$ip${data.download}","${data.id}")
                                    }

                                }else{
                                    val taskState =
                                        Aria.download(mDownLoadDialogBoom).load(data.id).taskExists()

                                    if(taskState){
                                        Aria.download(mDownLoadDialogBoom).load(data.id).resume()
                                        holder.download?.setImageResource(R.mipmap.jiazai)
                                    }else{

                                        if(data.type == "apk"){

                                            data.id = Aria.download(mDownLoadDialogBoom)
                                                .load("$ip${data.download}") //读取下载地址
                                                .setFilePath("${FileUrl.zeroTermuxApk}/${data.fileName}") //设置文件保存的完整路径
                                                .create()
                                        }else{
                                            data.id = Aria.download(mDownLoadDialogBoom)
                                                .load("$ip${data.download}") //读取下载地址
                                                .setFilePath("${FileUrl.zeroTermuxData}/${data.fileName}") //设置文件保存的完整路径
                                                .create()
                                        }


                                        holder.download?.setImageResource(R.mipmap.jiazai)

                                        if(data.id == -1L){
                                            SaveData.saveStringOther("$ip${data.download}","def")
                                        }else{
                                            SaveData.saveStringOther("$ip${data.download}","${data.id}")
                                        }


                                    }


                                }


                            } catch (e: Exception) {
                                e.printStackTrace()

                                if(data.type == "apk"){

                                    data.id = Aria.download(mDownLoadDialogBoom)
                                        .load("$ip${data.download}") //读取下载地址
                                        .setFilePath("${FileUrl.zeroTermuxApk}/${data.fileName}") //设置文件保存的完整路径
                                        .create()
                                }else{
                                    data.id = Aria.download(mDownLoadDialogBoom)
                                        .load("$ip${data.download}") //读取下载地址
                                        .setFilePath("${FileUrl.zeroTermuxData}/${data.fileName}") //设置文件保存的完整路径
                                        .create()
                                }


                                holder.download?.setImageResource(R.mipmap.jiazai)

                                if(data.id == -1L){
                                    SaveData.saveStringOther("$ip${data.download}","def")
                                }else{
                                    SaveData.saveStringOther("$ip${data.download}","${data.id}")
                                }
                            }
                        }else{


                            if(data.type == "apk"){

                                data.id = Aria.download(mDownLoadDialogBoom)
                                    .load("$ip${data.download}") //读取下载地址
                                    .setFilePath("${FileUrl.zeroTermuxApk}/${data.fileName}") //设置文件保存的完整路径
                                    .create()
                            }else{
                                data.id = Aria.download(mDownLoadDialogBoom)
                                    .load("$ip${data.download}") //读取下载地址
                                    .setFilePath("${FileUrl.zeroTermuxData}/${data.fileName}") //设置文件保存的完整路径
                                    .create()
                            }

                            holder.download?.setImageResource(R.mipmap.jiazai)
                            if(data.id == -1L){
                                SaveData.saveStringOther("$ip${data.download}","def")
                            }else{
                                SaveData.saveStringOther("$ip${data.download}","${data.id}")
                            }


                        }


                            return@setOnClickListener

                    }

                    if(data.isRun){
                        data.isRun = false
                        data.isFail = false
                        holder.download?.setImageResource(R.mipmap.download)
                        Aria.download(this).load(data.id).stop()

                        UUtils.showLog("任务状态-----------恢复的ID(暂停):${data.id}")
                    }else{
                        holder.download?.setImageResource(R.mipmap.jixu_download)
                        data.isRun = true


                        val stringOther = SaveData.getStringOther("$ip${data.download}")





                        if(stringOther != null && stringOther.isNotEmpty() && stringOther != "def" ){

                            try {

                                val toLong = stringOther.toLong()



                                data.id = toLong
                                UUtils.showLog("任务状态-----------恢复的ID(恢复):${data.id}")
                                if(toLong == -1L){

                                    Aria.download(this).load(data.id).stop()
                                    Aria.download(this).load(data.id).cancel(true);

                                    if(data.type == "apk"){

                                        data.id = Aria.download(mDownLoadDialogBoom)
                                            .load("$ip${data.download}") //读取下载地址
                                            .setFilePath("${FileUrl.zeroTermuxApk}/${data.fileName}") //设置文件保存的完整路径
                                            .create()
                                    }else{
                                        data.id = Aria.download(mDownLoadDialogBoom)
                                            .load("$ip${data.download}") //读取下载地址
                                            .setFilePath("${FileUrl.zeroTermuxData}/${data.fileName}") //设置文件保存的完整路径
                                            .create()
                                    }


                                    holder.download?.setImageResource(R.mipmap.jiazai)

                                    if(data.id == -1L){
                                        SaveData.saveStringOther("$ip${data.download}","def")
                                    }else{
                                        SaveData.saveStringOther("$ip${data.download}","${data.id}")
                                    }

                                }else{
                                    val taskState =
                                        Aria.download(mDownLoadDialogBoom).load(data.id).taskExists()

                                    if(taskState){
                                        Aria.download(mDownLoadDialogBoom).load(data.id).resume()
                                        holder.download?.setImageResource(R.mipmap.jiazai)
                                    }else{

                                        if(data.type == "apk"){

                                            data.id = Aria.download(mDownLoadDialogBoom)
                                                .load("$ip${data.download}") //读取下载地址
                                                .setFilePath("${FileUrl.zeroTermuxApk}/${data.fileName}") //设置文件保存的完整路径
                                                .create()
                                        }else{
                                            data.id = Aria.download(mDownLoadDialogBoom)
                                                .load("$ip${data.download}") //读取下载地址
                                                .setFilePath("${FileUrl.zeroTermuxData}/${data.fileName}") //设置文件保存的完整路径
                                                .create()
                                        }


                                        holder.download?.setImageResource(R.mipmap.jiazai)

                                        if(data.id == -1L){
                                            SaveData.saveStringOther("$ip${data.download}","def")
                                        }else{
                                            SaveData.saveStringOther("$ip${data.download}","${data.id}")
                                        }


                                    }
                                }
                            }catch (e:Exception){
                                e.printStackTrace()

                                if(data.type == "apk"){
                                    data.id = Aria.download(mDownLoadDialogBoom)
                                        .load("$ip${data.download}") //读取下载地址
                                        .setFilePath("${FileUrl.zeroTermuxApk}/${data.fileName}") //设置文件保存的完整路径
                                        .create()
                                }else{
                                    data.id = Aria.download(mDownLoadDialogBoom)
                                        .load("$ip${data.download}") //读取下载地址
                                        .setFilePath("${FileUrl.zeroTermuxData}/${data.fileName}") //设置文件保存的完整路径
                                        .create()
                                }
                                holder.download?.setImageResource(R.mipmap.jiazai)

                                if(data.id == -1L){
                                    SaveData.saveStringOther("$ip${data.download}","def")
                                }else{
                                    SaveData.saveStringOther("$ip${data.download}","${data.id}")
                                }

                            }


                        }else{


                            if(data.type == "apk"){
                                data.id = Aria.download(mDownLoadDialogBoom)
                                    .load("$ip${data.download}") //读取下载地址
                                    .setFilePath("${FileUrl.zeroTermuxApk}/${data.fileName}") //设置文件保存的完整路径
                                    .create()
                            }else{
                                data.id = Aria.download(mDownLoadDialogBoom)
                                    .load("$ip${data.download}") //读取下载地址
                                    .setFilePath("${FileUrl.zeroTermuxData}/${data.fileName}") //设置文件保存的完整路径
                                    .create()
                            }
                            holder.download?.setImageResource(R.mipmap.jiazai)
                            if(data.id == -1L){
                                SaveData.saveStringOther("$ip${data.download}","def")
                            }else{
                                SaveData.saveStringOther("$ip${data.download}","${data.id}")
                            }
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




            val stringOther = SaveData.getStringOther("$ip${data.download}")

            if(stringOther != null && stringOther.isNotEmpty() && stringOther != "def") {

                if(!tempFile.exists()){

                    val taskState = Aria.download(this).load(data.id).taskState

                    if(taskState == 4 || taskState == -1) {
                        UUtils.showLog("当前状态:$taskState")

                        Aria.download(this).load(data.id).stop()
                        Aria.download(this).load(data.id).cancel(true);

                        data.isFail = true

                        SaveData.saveStringOther("$ip${data.download}", "def")
                    }


                }





            }



            if (!zeroTermuxData.exists()) {
                Aria.download(this).stopAllTask();
                data.isRun = false
                holder.download?.setImageResource(R.mipmap.download)
            }
            if (!zeroTermuxApk.exists()) {
                data.isRun = false
                Aria.download(this).stopAllTask();
                holder.download?.setImageResource(R.mipmap.download)
            }


            /**
             *
             * 下载失败
             *
             */

            if(data.isFail){

                holder.download?.setImageResource(R.mipmap.restart)

            }


            UUtils.showLog("是否禁止:${data.isDownload }")
            if(data.isDownload == "1"){
                holder!!.show_img_jz?.visibility = View.VISIBLE
            }else{
                holder!!.show_img_jz?.visibility = View.GONE
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
        public var show_img:ImageView? = null
        public var show_img_jz:ImageView? = null
        public var number_progress_bar: NumberProgressBar? = null

        constructor(itemView: View) : super(itemView){

            name = itemView.findViewById(R.id.name)
            size = itemView.findViewById(R.id.size)
            show_img_jz = itemView.findViewById(R.id.show_img_jz)
            note = itemView.findViewById(R.id.note)
            show_img = itemView.findViewById(R.id.show_img)
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
               // UUtils.showLog("连接成功:" + msg.obj)
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

                if( task.convertSpeed != null){
                    mArray!![i].speed = task.convertSpeed
                }else{
                    mArray!![i].speed = "--"
                }

                if(task.convertTimeLeft != null){
                    mArray!![i].timeLeft = task.convertTimeLeft
                }else{
                    mArray!![i].timeLeft ="--"
                }

                if(task.convertCurrentProgress != null){
                    mArray!![i].convertCurrentProgress = task.convertCurrentProgress
                }else{
                    mArray!![i].convertCurrentProgress = "--"
                }

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


    private fun installApk(mFile:File) {
        dismiss()
        val switchDialog1: SwitchDialog = switchDialogShow(UUtils.getString(R.string.警告), UUtils.getString(R.string.第三方服务器上的可执行文件))
        switchDialog1.cancel!!.setOnClickListener { switchDialog1.dismiss() }
        switchDialog1.ok!!.setOnClickListener {
            switchDialog1.dismiss()
            XXPermissions.with(mContext)
                .permission(Permission.WRITE_EXTERNAL_STORAGE)
                .permission(Permission.READ_EXTERNAL_STORAGE)
                .permission(Permission.REQUEST_INSTALL_PACKAGES)
                .request(object : OnPermissionCallback {
                    override fun onGranted(permissions: List<String>, all: Boolean) {
                        if (all) {
                            UUtils.installApk(UUtils.getContext(), mFile.absolutePath)
                        } else {
                            UUtils.showMsg("无权限")
                        }
                    }

                    override fun onDenied(permissions: List<String>, never: Boolean) {
                        if (never) {
                            UUtils.showMsg("无权限")
                            // 如果是被永久拒绝就跳转到应用权限系统设置页面
                            XXPermissions.startPermissionActivity(mContext, permissions)
                        } else {
                            UUtils.showMsg("无权限")
                        }
                    }
                })
        }
    }


    private fun switchDialogShow(title: String, msg: String): SwitchDialog {
        val switchDialog = SwitchDialog(mContext)
        switchDialog.title!!.text = title
        switchDialog.msg!!.text = msg
        switchDialog.other!!.visibility = View.GONE
        switchDialog.ok!!.text = UUtils.getString(R.string.确定)
        switchDialog.cancel!!.text = UUtils.getString(R.string.取消)
        switchDialog.show()
        return switchDialog
    }

}
