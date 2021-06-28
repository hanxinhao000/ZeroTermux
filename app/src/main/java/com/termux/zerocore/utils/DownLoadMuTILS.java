package com.termux.zerocore.utils;

import android.content.Context;

import com.arialyy.annotations.Download;
import com.arialyy.aria.core.Aria;
import com.arialyy.aria.core.task.DownloadTask;
import com.example.xh_lib.utils.UUtils;

public class DownLoadMuTILS {

    public void register(){
        Aria.download(this).register();

    }

    public void unRegister(){
        Aria.download(this).unRegister();

    }

    @Download.onTaskRunning public void onTaskRunning(DownloadTask task) {
      /*  if(task.getKey() == (url)){

        }*/

        if(mDownLoadMuTILSListener != null){
            mDownLoadMuTILSListener.onTaskRunning(task);
        }

        int percent = task.getPercent();//任务进度百分比
        String convertSpeed = task.getConvertSpeed();//转换单位后的下载速度，单位转换需要在配置文件中打开
        task.getSpeed(); //原始byte长度速度

        UUtils.showLog("任务状态(当前执行任务百分比):" + percent);
        UUtils.showLog("任务状态(当前执行任务下载速度):" + convertSpeed);
    }

    @Download.onTaskComplete
    public void taskComplete( DownloadTask task) {
        //在这里处理任务完成的状态
        UUtils.showLog("任务状态(当前执行任务):完成");
        mDownLoadMuTILSListener.taskComplete(task);
    }
    @Download.onTaskFail
    public void onTaskFail(DownloadTask task){
        mDownLoadMuTILSListener.onTaskFail(task);
        UUtils.showLog("任务状态(当前执行任务):失败");
    }

    public void setDownLoadMuTILSListener(DownLoadMuTILSListener mDownLoadMuTILSListener){
        this.mDownLoadMuTILSListener = mDownLoadMuTILSListener;
    }

    private DownLoadMuTILSListener mDownLoadMuTILSListener;

    public interface  DownLoadMuTILSListener{


        void onTaskRunning(DownloadTask task);
        void taskComplete( DownloadTask task);
        void onTaskFail( DownloadTask task);




    }

}
