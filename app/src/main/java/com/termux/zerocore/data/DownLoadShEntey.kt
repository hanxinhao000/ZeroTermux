package com.termux.zerocore.data

import android.os.Message
import com.blockchain.ub.utils.httputils.BaseHttpUtils
import com.blockchain.ub.utils.httputils.HttpResponseListenerBase
import com.lzy.okgo.model.Response
import java.io.File
import java.io.PrintWriter

object DownLoadShEntey {

    private var mDownLoadShEnteyListener:DownLoadShEnteyListener? = null

    public fun downLoadSh(ip:String,filePath:String){


        BaseHttpUtils().getUrl(ip,object :HttpResponseListenerBase{
            override fun onSuccessful(msg: Message, mWhat: Int) {

                saveFile(filePath,msg.obj as String)


            }

            override fun onFailure(response: Response<String>?, msg: String, mWhat: Int) {

                mDownLoadShEnteyListener?.error("not download file,server error!!!")

            }
        },HashMap(),5559)



    }

    public fun setDownLoadShEnteyListener(mDownLoadShEnteyListener:DownLoadShEnteyListener){
        this.mDownLoadShEnteyListener = mDownLoadShEnteyListener
    }

    public interface DownLoadShEnteyListener{

        fun downLoadFile(filePath:String)

        fun error(msg:String)


    }

    private fun saveFile(filePath:String?,msg:String?){

        if(msg == null || msg.isEmpty()){
            mDownLoadShEnteyListener?.error("file is empty!!!return")
            return
        }

        val printWriter = PrintWriter(File(filePath!!))
        printWriter.println(msg)
        printWriter.flush()
        printWriter.close()

        mDownLoadShEnteyListener?.downLoadFile(filePath)
    }


}
