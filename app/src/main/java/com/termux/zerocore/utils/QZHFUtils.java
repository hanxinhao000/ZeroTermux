package com.termux.zerocore.utils;

import android.util.Log;
import android.widget.Toast;

import com.example.xh_lib.utils.UUtils;
import com.google.gson.Gson;
import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.zerocore.activity.BackNewActivity;
import com.termux.zerocore.dialog.MyDialog;
import com.termux.zerocore.fragment.BackupFragment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;



public class QZHFUtils {

    private File mFile = new File("/data/data/com.termux/");
    private File createFile;

    public void main(MyDialog myDialog, String systemName, BackupFragment restoreFragment) {

        new Thread(new Runnable() {
            @Override
            public void run() {


                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                UUtils.getHandler().post(new Runnable() {
                    @Override
                    public void run() {
                        myDialog.getDialog_pro().setText(UUtils.getString(R.string.开始检测备份环境));
                        myDialog.getDialog_pro_prog().setProgress(15);
                    }
                });

                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                UUtils.getHandler().post(new Runnable() {
                    @Override
                    public void run() {
                        myDialog.getDialog_pro().setText(UUtils.getString(R.string.备份环境监测完成));
                        myDialog.getDialog_pro_prog().setProgress(50);
                    }
                });

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                UUtils.getHandler().post(new Runnable() {
                    @Override
                    public void run() {
                        myDialog.getDialog_pro().setText(UUtils.getString(R.string.开始检测是否有sd卡软链接));
                        myDialog.getDialog_pro_prog().setProgress(75);
                    }
                });


            /* TermuxApplication.mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        TermuxActivity.mTerminalView.sendTextToTerminal("termux-setup-storage \n");
                    }
                });*/

                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if (!(new File("/data/data/com.termux/files/home/storage").exists())) {


                    UUtils.getHandler().post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(UUtils.getContext(), UUtils.getString(R.string.没有找到storage目录), Toast.LENGTH_SHORT).show();

                            myDialog.dismiss();
                            TermuxActivity.mTerminalView.sendTextToTerminal("termux-setup-storage");
                            restoreFragment.getActivity().finish();
                        }
                    });
                    return;
                }

                try {
                    Thread.sleep(1500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                UUtils.getHandler().post(new Runnable() {
                    @Override
                    public void run() {
                        myDialog.getDialog_pro().setText(UUtils.getString(R.string.已检测到软连接));
                        myDialog.getDialog_pro_prog().setProgress(80);
                    }
                });

                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                UUtils.getHandler().post(new Runnable() {
                    @Override
                    public void run() {
                        myDialog.getDialog_pro().setText(UUtils.getString(R.string.秒后开始备份));
                        myDialog.getDialog_pro_prog().setProgress(100);
                    }
                });


                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                UUtils.getHandler().post(new Runnable() {
                    @Override
                    public void run() {
                        myDialog.dismiss();

                        TermuxActivity.mTerminalView.sendTextToTerminal("cd ~ && cd ~ && tar -zcvf ./storage/shared/xinhao/data/" + systemName + " /data/data/com.termux/files && echo \"备份完成，备份文件在->内部存储/xinhao/data/|目录下\" \n");


                        restoreFragment.getActivity().finish();

                        BackNewActivity.mIsRun = false;
                    }
                });


            }
        }).start();

    }




}
