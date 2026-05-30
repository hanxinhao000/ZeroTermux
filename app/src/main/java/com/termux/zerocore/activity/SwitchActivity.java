package com.termux.zerocore.activity;

import static com.termux.shared.termux.TermuxConstants.TERMUX_INTERNAL_PRIVATE_APP_DATA_DIR_PATH;

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
import androidx.cardview.widget.CardView;

import com.example.xh_lib.utils.LogUtils;
import com.example.xh_lib.utils.UUtils;
import com.google.gson.Gson;
import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.app.TermuxInstaller;
import com.termux.app.TermuxService;
import com.termux.shared.termux.TermuxConstants;
import com.termux.zerocore.activity.adapter.CreateSystemAdapter;
import com.termux.zerocore.bean.CreateSystemBean;
import com.termux.zerocore.bean.ReadSystemBean;
import com.termux.zerocore.dialog.MyDialog;
import com.termux.zerocore.settings.BaseTitleActivity;
import com.termux.zerocore.shell.ExeCommand;
import com.termux.zerocore.url.FileUrl;

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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class SwitchActivity extends BaseTitleActivity implements View.OnClickListener {
    private final String TAG = SwitchActivity.class.getSimpleName();
    private ListView list;

    private CardView mAddContainers;

    private File mFile = new File(TERMUX_INTERNAL_PRIVATE_APP_DATA_DIR_PATH + "/");
    private File mFileTEMP = new File(FileUrl.INSTANCE.getMainHomeTemp());
    private File mDefFile = new File(FileUrl.INSTANCE.getXinhaoSystemPath());
    private File mFileHomeStatic = new File(FileUrl.INSTANCE.getBusyboxStaticPath());
    private File mFileHome = new File(FileUrl.INSTANCE.getBusyboxPath());
    private static final String ACTION_STOP_SERVICE = TermuxConstants.TERMUX_APP.TERMUX_SERVICE.ACTION_STOP_SERVICE;
    private CreateSystemAdapter createSystemAdapter;
    private SimpleDateFormat mSimpleDateFormat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_switch);
        setBaseTitle(UUtils.getString(R.string.容器切换));
        list = findViewById(R.id.list_switch);
        mAddContainers = findViewById(R.id.add_containers);
        mAddContainers.setOnClickListener(this);
        mSimpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        clickList();
        isIofo();
        readFile();
        //  testJson();
    }

    //判断默认系统
    private void isIofo() {
        LogUtils.i(TAG, "isIofo mDefFile: " + mDefFile.getAbsolutePath());
        if (!mDefFile.exists()) {
            try {
                mDefFile.createNewFile();
                CreateSystemBean createSystemBean = new CreateSystemBean();
                createSystemBean.systemName = UUtils.getString(R.string.item_containers_main_name);
                createSystemBean.dir = TermuxConstants.TERMUX_FILES_DIR_PATH;
                createSystemBean.time = mSimpleDateFormat.format(new Date());
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
                            a.setTitle(UUtils.getString(R.string.item_containers_dialog_delete_system_title));
                            a.setMessage(UUtils.getString(R.string.item_containers_dialog_delete_system_msg));
                            a.setNegativeButton(UUtils.getString(R.string.item_containers_dialog_i_m_sure), (dialog5, which5) -> {
                                a.create().dismiss();

                                new Thread(() -> {
                                    if (mList.get(position).dir.equals(TermuxConstants.TERMUX_FILES_DIR_PATH)) {
                                        runOnUiThread(() -> Toast.makeText(SwitchActivity.this, UUtils.getString(R.string.item_containers_toast_not_delete_main_system), Toast.LENGTH_SHORT).show());
                                        return;
                                    }
                                    Log.e("XINHAO_HAN", "删除系统: " + mList.get(position).dir);
                                    runOnUiThread(() -> {
                                        MyDialog myDialog = new MyDialog(SwitchActivity.this);
                                        myDialog.show();
                                        myDialog.getDialog_title().setText(UUtils.getString(R.string.item_containers_dialog_delete_system_loading_title));
                                        myDialog.getDialog_pro().setText(UUtils.getString(R.string.item_containers_dialog_delete_system_loading_msg));
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
                                    ExeCommand cmd2 = new ExeCommand(false).run(mFileHome.getAbsolutePath() + " rm -rf " + mList.get(position).dir, 60000, false);

                                    while (cmd2.isRunning()) {
                                        try {
                                            Thread.sleep(5);
                                        } catch (Exception e) {

                                        }
                                        String buf = cmd2.getResult();
                                        //do something}
                                        Log.e("XINHAO_HAN", "run: " + buf);
                                    }

                                    runOnUiThread(() -> {
                                        if (new File(mList.get(position).dir).exists()) {
                                            Toast.makeText(SwitchActivity.this, UUtils.getString(R.string.item_containers_toast_clearing_system_residues), Toast.LENGTH_SHORT).show();
                                            com.termux.zerocore.utils.SingletonCommunicationUtils.getInstance().getmSingletonCommunicationListener().sendTextToTerminal("chmod 777 -R " + mList.get(position).dir + "&& rm -rf " + mList.get(position).dir + " \n");
                                            finish();
                                        } else {
                                            Toast.makeText(SwitchActivity.this, UUtils.getString(R.string.item_containers_dialog_deleted_successfully), Toast.LENGTH_SHORT).show();
                                            finish();
                                        }
                                    });
                                }).start();
                            });
                            a.setPositiveButton(UUtils.getString(R.string.item_containers_dialog_i_don_t_delete_it), (dialog4, which4) -> {
                                a.create().dismiss();
                                Toast.makeText(SwitchActivity.this, UUtils.getString(R.string.item_containers_toast_ignoring_operations), Toast.LENGTH_SHORT).show();
                            });
                            //setNegativeButton
                            a.setNeutralButton(UUtils.getString(R.string.item_containers_dialog_don_t_delete_it), (dialog3, which3) -> {
                                a.create().dismiss();
                                Toast.makeText(SwitchActivity.this, UUtils.getString(R.string.item_containers_toast_ignoring_operations), Toast.LENGTH_SHORT).show();
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
                                    readSystemBean1.time = mSimpleDateFormat.format(new Date());
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
                                    // readSystemBean1.dir = "/data/data/com.termux/files";
                                    LogUtils.i(TAG, "clickList: TermuxConstants.TERMUX_FILES_DIR_PATH:" + TermuxConstants.TERMUX_FILES_DIR_PATH);
                                    readSystemBean1.dir = TermuxConstants.TERMUX_FILES_DIR_PATH;
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
                                Toast.makeText(SwitchActivity.this, UUtils.getString(R.string.item_containers_dialog_read_failed), Toast.LENGTH_SHORT).show();
                            }
                            setAllFlase(mList);
                            mList.get(position).isCkeck = true;
                            createSystemAdapter.notifyDataSetChanged();
                            AlertDialog.Builder ab = new AlertDialog.Builder(SwitchActivity.this);
                            ab.setTitle(UUtils.getString(R.string.提示));
                            ab.setCancelable(false);
                            ab.setMessage(UUtils.getString(R.string.item_containers_dialog_msg));
                            ab.setNegativeButton(UUtils.getString(R.string.item_containers_dialog_need), (dialog2, which2) -> {
                                Toast.makeText(SwitchActivity.this, UUtils.getString(R.string.item_containers_dialog_manual_exit), Toast.LENGTH_SHORT).show();
                                new Intent(SwitchActivity.this, TermuxService.class).setAction(ACTION_STOP_SERVICE);
                                System.exit(0);
                                finish();
                            });

                            ab.setPositiveButton(UUtils.getString(R.string.item_containers_dialog_no_need), (dialog1, which1) -> {
                                Toast.makeText(SwitchActivity.this, UUtils.getString(R.string.item_containers_toast_settings_ok_reboot), Toast.LENGTH_SHORT).show();
                                finish();
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

    private void setAllFlase(List<ReadSystemBean> mList) {
        for (int i = 0; i < mList.size(); i++) {
            mList.get(i).isCkeck = false;
        }
        createSystemAdapter.notifyDataSetChanged();
    }
    //先读取
    private void readFile() {
        File[] files = mFile.listFiles();
        ArrayList<ReadSystemBean> arrayList = new ArrayList<>();
        for (int i = 0; i < files.length; i++) {
            if (files[i].getName().startsWith("files")) {
                ReadSystemBean readSystemBean = new ReadSystemBean();
                readSystemBean.dir = files[i].getAbsolutePath();
                CreateSystemBean createSystemBean = readInfo(files[i].getAbsolutePath());
                String name = createSystemBean.systemName;
                if (name == null) {
                    new File(files[i], "/xinhao_system.infoJson").delete();
                    Toast.makeText(this, UUtils.getString(R.string.item_containers_toast_config_error), Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
                readSystemBean.name = name;
                readSystemBean.time = createSystemBean.time;
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
            File file = new File(FileUrl.INSTANCE.getXinhaoSystemPath());

            if (!file.exists()) {
                Toast.makeText(this, UUtils.getString(R.string.item_containers_toast_not_config), Toast.LENGTH_SHORT).show();
                return;
            }
            String temp;

            String tempStr = "";
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));

            while ((temp = bufferedReader.readLine()) != null) {
                tempStr += temp;
            }


            CreateSystemBean createSystemBean = new Gson().fromJson(tempStr, CreateSystemBean.class);

            if (createSystemBean == null) {
                createSystemBean = new CreateSystemBean();
                createSystemBean.dir = TermuxConstants.TERMUX_FILES_DIR_PATH;
                createSystemBean.time = mSimpleDateFormat.format(new Date());
                createSystemBean.systemName = UUtils.getString(R.string.item_containers_toast_def_system);
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
    private CreateSystemBean readInfo(String path) {
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
            if (createSystemBean == null) {
                createSystemBean.systemName = UUtils.getString(R.string.item_containers_error_system);
            }
            return createSystemBean;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        CreateSystemBean createSystemBean = new CreateSystemBean();
        createSystemBean.systemName = UUtils.getString(R.string.item_containers_toast_def_system);
        return createSystemBean;
    }


    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.add_containers) {
            createSystemDialog();
        }
    }

    //创建新的系统
    private void createSystemDialog() {

        final EditText et = new EditText(this);
        new AlertDialog.Builder(this).setTitle(UUtils.getString(R.string.item_containers_dialog_input_linux_name))
            .setIcon(R.mipmap.linux_new_ico)
            .setView(et)
            .setPositiveButton(UUtils.getString(R.string.确定), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    //按下确定键后的事件
                    createSystem(et.getText().toString());
                }
            }).setNegativeButton(UUtils.getString(R.string.取消), null).show();
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
            createSystemBean.time = mSimpleDateFormat.format(new Date());
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
                Toast.makeText(this, UUtils.getString(R.string.item_containers_toast_create_system_error), Toast.LENGTH_SHORT).show();
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
            createSystemBean.time = mSimpleDateFormat.format(new Date());
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
                Toast.makeText(this, UUtils.getString(R.string.item_containers_toast_create_system_error), Toast.LENGTH_SHORT).show();
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
