package com.termux.zerocore.activity;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.xh_lib.utils.UUtils;
import com.google.gson.Gson;
import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.app.TermuxInstaller;
import com.termux.app.TermuxService;
import com.termux.zerocore.activity.adapter.CreateSystemAdapter;
import com.termux.zerocore.bean.CreateSystemBean;
import com.termux.zerocore.bean.ReadSystemBean;
import com.termux.zerocore.dialog.MyDialog;
import com.termux.zerocore.shell.ExeCommand;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;


public class SwitchActivity extends AppCompatActivity  implements View.OnClickListener {


    private ListView list;

    private ImageView create_img;

    private File mFile = new File("/data/data/com.termux/");
    private File mFileTEMP = new File("/data/data/com.termux/temp");
    private File mDefFile = new File("/data/data/com.termux/files/xinhao_system.infoJson");
    private File mFileHomeStatic = new File("/data/data/com.termux/busybox_static");
    private File mFileHome = new File("/data/data/com.termux/busybox");
    private static final String ACTION_STOP_SERVICE = "com.termux.service_stop";
    private CreateSystemAdapter createSystemAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_switch);

        list = findViewById(R.id.list_switch);

        create_img = findViewById(R.id.create_img);

        create_img.setOnClickListener(this);

        clickList();
        isIofo();

        readFile();
        //  testJson();


    }


    //判断默认系统
    private void isIofo() {


        if (!mDefFile.exists()) {

            try {
                mDefFile.createNewFile();

                CreateSystemBean createSystemBean = new CreateSystemBean();

                createSystemBean.systemName = UUtils.getString(R.string.无名称系统);

                createSystemBean.dir = "/data/data/com.termux/files";

                String s = new Gson().toJson(createSystemBean);

                PrintWriter printWriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(mDefFile)));

                printWriter.print(s);

                printWriter.flush();

                printWriter.close();


            } catch (IOException e) {
                e.printStackTrace();
            }


        }


    }

    //点击事件

    private void clickList() {

        // Toast.makeText(this, "执行了", Toast.LENGTH_SHORT).show();

        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                List<ReadSystemBean> mList = createSystemAdapter.mList;


                // Toast.makeText(SwitchActivity.this, "" + position, Toast.LENGTH_SHORT).show();

                //当前系统


                String[] strings = {UUtils.getString(R.string.删除), UUtils.getString(R.string.切换)};

                android.app.AlertDialog.Builder builder = new android.app.AlertDialog
                    .Builder(SwitchActivity.this);
                builder.setTitle(UUtils.getString(R.string.删除完成_需要重进才能刷新));
                // builder.setMessage("这是个滚动列表，往下滑");
                builder.setItems(strings, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Toast.makeText(TermuxActivity.this, "选择了第" + which + "个", Toast.LENGTH_SHORT).show();

                        if (which == 0) {

                            builder.create().dismiss();
                            AlertDialog.Builder a = new AlertDialog.Builder(SwitchActivity.this);
                            a.setTitle("你确定要删除吗");
                            a.setMessage("请确保你的sd卡权限未被获取!\n否则很可能会清空你SD卡(内部存储)上的所有内容!\n否则很可能会清空你SD卡(内部存储)上的所有内容!\n否则很可能会清空你SD卡(内部存储)上的所有内容!\n你确定要删除掉你的系统吗?\n请慎重,删除后你的数据不可恢复!!!!!!");
                            a.setNegativeButton("我确定", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    a.create().dismiss();

                                    new Thread(new Runnable() {
                                        @Override
                                        public void run() {
                                            if (mList.get(position).dir.equals("/data/data/com.termux/files")) {
                                                runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        Toast.makeText(SwitchActivity.this, "你不能删除主系统", Toast.LENGTH_SHORT).show();

                                                    }
                                                });
                                                return;
                                            }

                                            Log.e("XINHAO_HAN", "删除系统: " + mList.get(position).dir);

                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    MyDialog myDialog = new MyDialog(SwitchActivity.this);
                                                    myDialog.show();
                                                    myDialog.getDialog_title().setText("正在删除,请耐心等待...");
                                                    myDialog.getDialog_pro().setText("时间长短，由您的系统大小决定的");
                                                }
                                            });
                                            Log.e("XINHAO_HAN", "删除目录: " + mList.get(position).dir);

                                            String cpu = TermuxInstaller.determineTermuxArchName();

                                            switch (cpu) {
                                                case "aarch64":
                                                    writerFile("arm_64/busybox", mFileHome, 1024);
                                                    writerFile("arm_64/busybox_static", mFileHomeStatic, 1024);
                                                    // writerFile("arm_64/proot", mFileHomeProot, 1024);
                                                    break;
                                                case "arm":
                                                    writerFile("arm/busybox", mFileHome, 1024);
                                                    // writerFile("arm/busybox_static", mFileHomeStatic, 1024);
                                                    //   writerFile("arm/proot", mFileHomeProot, 1024);
                                                    break;
                                                case "x86_64":
                                                    writerFile("x86/busybox", mFileHome, 1024);
                                                    //  writerFile("x86/busybox_static", mFileHomeStatic, 1024);
                                                    //  writerFile("x86/proot", mFileHomeProot, 1024);
                                                    break;
                                            }

                                            try {
                                                Runtime.getRuntime().exec("chmod 777 " + mFileHome.getAbsolutePath());
                                                Runtime.getRuntime().exec("chmod 777 " + mFileHomeStatic.getAbsolutePath());
                                                //Runtime.getRuntime().exec("chmod 777 " + mFileHomeProot.getAbsolutePath());
                                            } catch (IOException e) {
                                                e.printStackTrace();
                                            }
                                            ExeCommand cmd2 = new ExeCommand(false).run(mFileHome.getAbsolutePath() + " rm -rf " + mList.get(position).dir, 60000,false);

                                            while (cmd2.isRunning()) {
                                                try {
                                                    Thread.sleep(5);
                                                } catch (Exception e) {

                                                }
                                                String buf = cmd2.getResult();
                                                //do something}

                                                Log.e("XINHAO_HAN", "run: " + buf);
                                            }


                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {

                                                    if(new File(mList.get(position).dir).exists()){
                                                        Toast.makeText(SwitchActivity.this, "正在清除系统残留文件", Toast.LENGTH_SHORT).show();
                                                        if (TermuxActivity.mTerminalView != null) {
                                                            TermuxActivity.mTerminalView.sendTextToTerminal("chmod 777 -R "+mList.get(position).dir+"&& rm -rf " + mList.get(position).dir + " \n");
                                                        } else {
                                                            Toast.makeText(SwitchActivity.this, "部分文件删除失败，请重试或手动删除!", Toast.LENGTH_SHORT).show();
                                                        }
                                                        finish();
                                                    }else{
                                                        Toast.makeText(SwitchActivity.this, "删除成功!", Toast.LENGTH_SHORT).show();
                                                        finish();
                                                    }


                                                }
                                            });

                                        }
                                    }).start();

                                }
                            });
                            a.setPositiveButton("我不删除", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    a.create().dismiss();
                                    Toast.makeText(SwitchActivity.this, "操作忽略", Toast.LENGTH_SHORT).show();
                                }
                            });
                            //setNegativeButton
                            a.setNeutralButton("别删除", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    a.create().dismiss();
                                    Toast.makeText(SwitchActivity.this, "操作忽略", Toast.LENGTH_SHORT).show();
                                }
                            });
                            a.show();


                        } else {

                            try {


                                //本目录系统
                                String temp;

                                String tempStr = "";
                                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(mDefFile)));

                                while ((temp = bufferedReader.readLine()) != null) {
                                    tempStr += temp;
                                }

                                //要被替换的系统
                                ReadSystemBean readSystemBean = mList.get(position);
                                //本目录的系统
                                CreateSystemBean readSystemBean1 = new Gson().fromJson(tempStr, CreateSystemBean.class);

                                //要被替换的
                                String path = readSystemBean.dir;
                                //  Log.e("XINHAO_HAN", "要被替换的: " + path);
                                //本目录的
                                String pathThis = readSystemBean1.dir;
                                // Log.e("XINHAO_HAN", "本目录的: " + pathThis);


                                File filePath = new File(path);

                                File filePathThis = new File(pathThis);


                                File fileOutPath = new File(filePath, "/xinhao_system.infoJson");
                                // /data/data/com.termux/files1/xinhao_system.infoJson

                                File fileOutPathThis = new File(filePathThis, "/xinhao_system.infoJson");
                                // /data/data/com.termux/files/xinhao_system.infoJson

                                try {
                                    fileOutPathThis.delete();

                                    fileOutPathThis.createNewFile();

                                    readSystemBean1.dir = readSystemBean.dir;

                                    PrintWriter printWriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(fileOutPathThis)));
                                    printWriter.print(new Gson().toJson(readSystemBean1));
                                    Log.e("XINHAO_HAN", "写入json: " + new Gson().toJson(readSystemBean1));
                                    printWriter.flush();

                                    printWriter.close();


                                } catch (IOException e) {
                                    e.printStackTrace();
                                }


                                try {
                                    fileOutPath.delete();

                                    fileOutPath.createNewFile();

                                    readSystemBean1.dir = "/data/data/com.termux/files";
                                    readSystemBean1.systemName = readSystemBean.name;
                                    // readSystemBean1.systemName = readSystemBean.name;
                                    // readSystemBean1.systemName = readSystemBean1.systemName;
                                    //  Log.e("XINHAO_HAN", "本目录的: " + pathThis);

                                    PrintWriter printWriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(fileOutPath)));


                                    printWriter.print(new Gson().toJson(readSystemBean1));

                                    printWriter.flush();

                                    printWriter.close();


                                } catch (IOException e) {
                                    e.printStackTrace();
                                }


                                filePath.renameTo(mFileTEMP);
                                filePathThis.renameTo(filePath);
                                mFileTEMP.renameTo(filePathThis);


                            } catch (Exception e) {
                                Toast.makeText(SwitchActivity.this, "读取失败!", Toast.LENGTH_SHORT).show();
                            }


                            setAllFlase(mList);

                            mList.get(position).isCkeck = true;

                            createSystemAdapter.notifyDataSetChanged();

                            AlertDialog.Builder ab = new AlertDialog.Builder(SwitchActivity.this);

                            ab.setTitle("提示");

                            ab.setCancelable(false);

                            ab.setMessage("切换成功!\n重启APP生效\n需要重启吗\n其实点击需要没用\n自己手动重启\n进入任务管理器,一般点击 '□'这个建，然后滑退APP ");

                            ab.setNegativeButton("需要", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Toast.makeText(SwitchActivity.this, "请手动退出APP", Toast.LENGTH_SHORT).show();
                                    new Intent(SwitchActivity.this, TermuxService.class).setAction(ACTION_STOP_SERVICE);
                                    System.exit(0);
                                    finish();
                                }
                            });

                            ab.setPositiveButton("不需要", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Toast.makeText(SwitchActivity.this, "设置已生效!请重启APP", Toast.LENGTH_SHORT).show();
                                    finish();

                                }
                            });

                            ab.show();

                        }

                    }
                });
                builder.show();
                //readSystemBean.dir


            }
        });


    }

    //全部flase

    private void setAllFlase(List<ReadSystemBean> mList) {

        for (int i = 0; i < mList.size(); i++) {

            mList.get(i).isCkeck = false;
        }
        createSystemAdapter.notifyDataSetChanged();


    }


    //全部ture


    //先读取
    private void readFile() {

        File[] files = mFile.listFiles();

        ArrayList<ReadSystemBean> arrayList = new ArrayList<>();

        for (int i = 0; i < files.length; i++) {


            if (files[i].getName().startsWith("files")) {

                ReadSystemBean readSystemBean = new ReadSystemBean();

                readSystemBean.dir = files[i].getAbsolutePath();

                String name = readInfo(files[i].getAbsolutePath());

                if (name == null) {
                    new File(files[i],"/xinhao_system.infoJson").delete();

                    Toast.makeText(this, "配置文件已损坏，请重新进入创建,多点几次,直到进入为止", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }

                readSystemBean.name = name;
                arrayList.add(readSystemBean);

            }

        }

        Log.e("XINHAO_HAN", "readFile: " + arrayList);
        setAdapter(arrayList);
    }

    //开始设置

    private void setAdapter(ArrayList<ReadSystemBean> arrayList) {

        createSystemAdapter = new CreateSystemAdapter(arrayList, this);

        list.setAdapter(createSystemAdapter);

        setDefSystem(arrayList);
    }
    //设置默认

    private void setDefSystem(ArrayList<ReadSystemBean> arrayList) {

        //取本地的系统目录
        try {
            File file = new File("/data/data/com.termux/files/xinhao_system.infoJson");

            if (!file.exists()) {
                Toast.makeText(this, "你当前的默认系统没有配置文件", Toast.LENGTH_SHORT).show();
                return;
            }
            String temp;

            String tempStr = "";
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));

            while ((temp = bufferedReader.readLine()) != null) {
                tempStr += temp;
            }


            CreateSystemBean createSystemBean = new Gson().fromJson(tempStr, CreateSystemBean.class);

            if(createSystemBean == null){
                createSystemBean = new CreateSystemBean();
                createSystemBean.dir = "/data/data/com.termux/files/";
                createSystemBean.systemName = "默认系统";
            }


            for (int i = 0; i < arrayList.size(); i++) {


                if (arrayList.get(i).name.equals(createSystemBean.systemName)) {

                    arrayList.get(i).isCkeck = true;

                    break;
                }


            }

            createSystemAdapter.notifyDataSetChanged();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }


    //读取
    private String readInfo(String path) {


        try {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(new File(new File(path), "/xinhao_system.infoJson"))));

            String temp = "";
            String tempSystem = "";
            while ((temp = bufferedReader.readLine()) != null) {

                tempSystem += temp;

            }
            bufferedReader.close();


            Log.e("XINHAO_HAN", "readInfo: " + tempSystem);
            CreateSystemBean createSystemBean = new Gson().fromJson(tempSystem, CreateSystemBean.class);


            if(createSystemBean == null){

                return"损坏的系统";
            }else{
                return createSystemBean.systemName;
            }


        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


        return "默认系统";
    }


    @Override
    public void onClick(View v) {

        if (v.getId() == R.id.create_img) {
            createSystemDialog();
        }
    }


    //创建新的系统
    private void createSystemDialog() {

        final EditText et = new EditText(this);
        new AlertDialog.Builder(this).setTitle("请输入新的linux名称")
            .setIcon(R.mipmap.linux_new_ico)
            .setView(et)
            .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    //按下确定键后的事件
                    createSystem(et.getText().toString());
                }
            }).setNegativeButton("取消", null).show();
    }

    //创建
    private void createSystem(String name) {
        //先扫描有多少文件
        File[] files = mFile.listFiles();

        if (files.length == 1) {
            //默认只有一个系统
            //直接创建
            File createFile = new File(mFile, "files1");
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
                Toast.makeText(this, "系统创建失败!请重试", Toast.LENGTH_SHORT).show();
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
            File createFile = new File(mFile, "files" + (max + 1));
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
                Toast.makeText(this, "系统创建失败!请重试", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
                return;
            } finally {
                if (printWriter != null) {
                    printWriter.close();
                }

            }
        }


        readFile();
        createSystemAdapter.notifyDataSetChanged();
    }

    private void createSystem() {

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


    private void writerFile(String name, File mFile, int size) {

        try {
            InputStream open = this.getAssets().open(name);

            int len = 0;

            byte[] b = new byte[size];

            if (!mFile.exists()) {
                mFile.createNewFile();
            }

            FileOutputStream fileOutputStream = new FileOutputStream(mFile);

            while ((len = open.read(b)) != -1) {
                fileOutputStream.write(b, 0, len);
            }

            fileOutputStream.flush();
            open.close();
            fileOutputStream.close();
        } catch (Exception e) {

            Log.e("XINHAO_HAN_FILE ", "writerFile: " + e.toString());
        }

    }


}
