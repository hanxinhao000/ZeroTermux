package com.termux.zerocore.utils;

import android.util.Log;
import android.widget.Toast;

import com.example.xh_lib.utils.UUtils;
import com.google.gson.Gson;
import com.termux.app.TermuxActivity;
import com.termux.zerocore.bean.CreateSystemBean;
import com.termux.zerocore.dialog.MyDialog;
import com.termux.zerocore.fragment.RestoreFragment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;



public class QZUtils {

    private File mFile = new File("/data/data/com.termux/");
    private File createFile;

    public void main(MyDialog myDialog, String systemName, File tarFle, RestoreFragment restoreFragment) {

        new Thread(new Runnable() {
            @Override
            public void run() {

                Log.e("系统:", "run: " + tarFle.getName());

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                UUtils.getHandler().post(new Runnable() {
                    @Override
                    public void run() {
                        myDialog.getDialog_pro().setText("系统搜索完成!");
                        myDialog.getDialog_pro_prog().setProgress(15);
                    }
                });

                //搜索系统


                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }


                UUtils.getHandler().post(new Runnable() {
                    @Override
                    public void run() {
                        myDialog.getDialog_pro().setText("开始创建新的系统盘!");
                        myDialog.getDialog_pro_prog().setProgress(25);
                    }
                });


             /*   UUtils.getHandler().post(new Runnable() {
                    @Override
                    public void run() {
                        TermuxActivity.mTerminalView.sendTextToTerminal("termux-setup-storage \n");
                    }
                });*/

                if (!(new File("/data/data/com.termux/files/home/storage").exists())) {


                    UUtils.getHandler().post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(UUtils.getContext(), "没有找到storage目录,请手动创建", Toast.LENGTH_SHORT).show();

                            myDialog.dismiss();
                            TermuxActivity.mTerminalView.sendTextToTerminal("termux-setup-storage");
                            restoreFragment.getActivity().finish();
                        }
                    });
                    return;
                }

                //创建新的系统盘
                createSystem(systemName);

                if (createFile == null) {
                    UUtils.getHandler().post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(UUtils.getContext(), "系统盘创建失败,请使用全手动模式恢复", Toast.LENGTH_SHORT).show();
                        }
                    });
                }

                Log.e("系统:", "run: " + createFile.getAbsolutePath());

                try {
                    Thread.sleep(1500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                UUtils.getHandler().post(new Runnable() {
                    @Override
                    public void run() {
                        myDialog.getDialog_pro().setText("新的系统盘创建完成!");
                        myDialog.getDialog_pro_prog().setProgress(45);
                    }
                });

                try {
                    Thread.sleep(1500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                UUtils.getHandler().post(new Runnable() {
                    @Override
                    public void run() {
                        myDialog.getDialog_pro().setText("处理最后的一些事请!");
                        myDialog.getDialog_pro_prog().setProgress(75);
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
                        myDialog.getDialog_pro().setText("3秒后开始恢复!");
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
                        Toast.makeText(UUtils.getContext(), "开始恢复..", Toast.LENGTH_SHORT).show();

                        TermuxActivity.mTerminalView.sendTextToTerminal("echo \"----手动恢复开始----\" \n");

                        TermuxActivity.mTerminalView.sendTextToTerminal("cd ~ && cd ~ && tar -xzvf ./storage/shared/xinhao/data/" + tarFle.getName().replace(" ","") + "  -C ../../" + createFile.getName() + " && mv ../../" + createFile.getName() + "/data/data/com.termux/files/home ../../" + createFile.getName() +" && "+ "mv ../../" + createFile.getName() + "/data/data/com.termux/files/usr ../../" + createFile.getName()+" && rm -rf ../../"+createFile.getName()+"/data && echo \"系统恢复完成,请在切换系统，切换您的系统\" \n");
                        //TermuxActivity.mTerminalView.sendTextToTerminal("tar -xzvf./storage/shared/xinhao/data/" + tarFle.getName() + "  -C ../../" + createFile.getName() + " && mv ../../" + createFile.getName() + "/data/data/com.termux/files/home ../../" + createFile.getName() +" && "+ "mv ../../" + createFile.getName() + "/data/data/com.termux/files/usr ../../" + createFile.getName()+" && rm -rf ../../"+createFile.getName()+"/data && echo \"系统恢复完成,请在切换系统，切换您的系统\" \n");

                        try {
                            restoreFragment.getActivity().finish();
                        }catch (Exception e){
                            Toast.makeText(UUtils.getContext(), "出现了一个微弱的警告，可忽略", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

            }
        }).start();

    }


    //创建
    private void createSystem(String name) {
        //先扫描有多少文件
        File[] files = mFile.listFiles();

        if (files.length == 1) {
            //默认只有一个系统
            //直接创建
            createFile = new File(mFile, "files1");
            createFile.mkdirs();
            CreateSystemBean createSystemBean = new CreateSystemBean();
            createSystemBean.dir = createFile.getAbsolutePath();
            createSystemBean.systemName = name;

            String s = new Gson().toJson(createSystemBean);


            File fileInfo = new File(createFile, "/xinhao_system.infoJson");
            PrintWriter printWriter = null;
            try {

                fileInfo.createNewFile();
                printWriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(fileInfo)));

                printWriter.print(s);
                printWriter.flush();
                printWriter.close();

            } catch (IOException e) {
                Toast.makeText(UUtils.getContext(), "系统创建失败!请重试", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
                return;
            } finally {
                if (printWriter != null) {
                    printWriter.close();
                }

            }


        } else {
            //有多个系统

            ArrayList<Integer> arrayList = new ArrayList<>();


            for (int i = 0; i < files.length; i++) {
                if (files[i].getName().startsWith("files")) {
                    // Log.e("XINHAO_HAN", "readFile: " + files[i].getAbsolutePath());
                    String name1 = files[i].getName();
                    String substring = name1.substring(5, name1.length());

                    if (substring.isEmpty()) {
                        arrayList.add(0);
                    } else {
                        arrayList.add(Integer.parseInt(substring));
                    }

                }
            }

            // Log.e("XINHAO_HAN", "createSystem: " + arrayList);


            int max = getMax(arrayList);
            Log.e("XINHAO_HAN", "最大值: " + max);


            //直接创建
            createFile = new File(mFile, "files" + (max + 1));
            createFile.mkdirs();
            CreateSystemBean createSystemBean = new CreateSystemBean();
            createSystemBean.dir = createFile.getAbsolutePath();
            createSystemBean.systemName = name;

            String s = new Gson().toJson(createSystemBean);


            File fileInfo = new File(createFile, "/xinhao_system.infoJson");
            PrintWriter printWriter = null;
            try {

                fileInfo.createNewFile();
                printWriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(fileInfo)));

                printWriter.print(s);
                printWriter.flush();
                printWriter.close();

            } catch (IOException e) {
                Toast.makeText(UUtils.getContext(), "系统创建失败!请重试", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
                return;
            } finally {
                if (printWriter != null) {
                    printWriter.close();
                }

            }
        }


    }


    //比大小
    private int getMax(ArrayList<Integer> number) {

        int temp = number.get(0);

        for (int i = 0; i < number.size(); i++) {

            if (number.get(i) > temp) {
                temp = number.get(i);
            }

        }


        return temp;
    }
}
