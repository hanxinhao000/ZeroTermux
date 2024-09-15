package com.termux.zerocore.utermux_windows.qemu.activity;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.xh_lib.utils.UUtils;
import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.zerocore.dialog.LoadingDialog;
import com.termux.zerocore.url.FileUrl;
import com.termux.zerocore.utermux_windows.qemu.data.TermuxData;
import com.termux.zerocore.utermux_windows.qemu.dialog.EditTextDialog;
import com.termux.zerocore.utermux_windows.qemu.dialog.EndDialog;
import com.termux.zerocore.utermux_windows.qemu.dialog.FileListDialog;
import com.termux.zerocore.utermux_windows.qemu.dialog.FileNameDialog;
import com.termux.zerocore.utermux_windows.qemu.dialog.SwitchQemuDialog;
import com.termux.zerocore.utils.SaveData;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;



public class RunWindowActivity extends AppCompatActivity implements TermuxData.IsQemuSul {
    private TextView qemu_state;
    private TextView dis_state;
    private TextView qemu_install;
    private TextView dis_install;
    private TextView tsu_install;
    private TextView copy_commit;

    private LinearLayout win10;
    private LinearLayout win8;
    private LinearLayout win7;
    private LinearLayout winxp;
    private LinearLayout mac;
    private LinearLayout other;


    private File mFile = new File("/data/data/com.termux/files/usr/bin/qemu-system-x86_64");
    private File mFileQemuStart = new File("/data/data/com.termux/files/home/.qemustart/start.sh");
    private File mFileQemuStart1 = new File("/data/data/com.termux/files/home/.qemustart");
    private File mFile1 = new File("/data/data/com.termux/files/usr/bin/qemu-x86_64-static");
    private File mFile2 = new File("/data/data/com.termux/files/usr/bin/qemu-system-i386");
    private File mWin10 = new File(Environment.getExternalStorageDirectory(),"/xinhao/windows/Utermux_win10.vhd");
    private File mWin10RunPath =new File("/data/data/com.termux/files/home/storage/shared/xinhao/windows/Utermux_win10.vhd");


    private File mWin7 = new File(Environment.getExternalStorageDirectory(),"/xinhao/windows/Utermux_win7.vhd");
    private File mWin7RunPath =new File("/data/data/com.termux/files/home/storage/shared/xinhao/windows/Utermux_win7.vhd");

    private File mWinConfig = new File(Environment.getExternalStorageDirectory(),"/xinhao/windows_config/");
    private File mWinWindows = new File(Environment.getExternalStorageDirectory(),"/xinhao/windows/");

    private File mWinxp = new File(Environment.getExternalStorageDirectory(),"/xinhao/windows/Utermux_xp.qcow2");
    private File mWinxpRunPath =new File("/data/data/com.termux/files/home/storage/shared/xinhao/windows/Utermux_xp.qcow2");

    private File mMac = new File(Environment.getExternalStorageDirectory(),"/xinhao/windows/Utermux-mac.qcow2");
    private File mMacRunPath =new File("/data/data/com.termux/files/home/storage/shared/xinhao/windows/Utermux-mac.qcow2");

    private boolean mkdirs = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_run_window);
        qemu_state = findViewById(R.id.qemu_state);
        dis_state = findViewById(R.id.dis_state);
        qemu_install = findViewById(R.id.qemu_install);
        dis_install = findViewById(R.id.dis_install);
        tsu_install = findViewById(R.id.tsu_install);
        copy_commit = findViewById(R.id.copy_commit);

        win10 = findViewById(R.id.win10);
        win8 = findViewById(R.id.win8);
        win7 = findViewById(R.id.win7);
        winxp = findViewById(R.id.winxp);
        mac = findViewById(R.id.mac);
        other = findViewById(R.id.other);
        if(!mFileQemuStart1.exists())
         mkdirs = mFileQemuStart1.mkdirs();

        setConfig();
        isQemuExe();

        TermuxData.getInstall().setIsQemuSul(this);
    }

    //判断qemu是否存在
    private void isQemuExe() {

        if(!mWinConfig.exists()) {
            mWinConfig.mkdirs();
        }
        if(!mWinWindows.exists()) {
            mWinWindows.mkdirs();
        }

        copy_commit.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {


                UUtils.copyToClip("");


                return true;
            }
        });

        winxp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(!mMac.exists()){
                    getIso();
                    return;
                }

                if(!mMacRunPath.exists()){

                    UUtils.showMsg(UUtils.getString(R.string.正在疏通所需要的环境));
                    TermuxActivity.mTerminalView.sendTextToTerminal("echo "+UUtils.getString(R.string.请输入回车键继续) + "\n");
                    TermuxActivity.mTerminalView.sendTextToTerminal("termux-setup-storage");
                    finish();
                    return;
                }


                FileListDialog fileListDialog1 = new FileListDialog(RunWindowActivity.this);
                fileListDialog1.show();
                fileListDialog1.setFilePath(mWinConfig);
                fileListDialog1.setTitleText(UUtils.getString(R.string.请选择模拟器配置文件));

                fileListDialog1.setBoomBtnVisible(true);
                fileListDialog1.setOnItemFileClickListener(new FileListDialog.OnItemFileClickListener() {
                    @Override
                    public void onItemClick(File file) {
                        fileListDialog1.dismiss();
                        UUtils.showLog("获取文件目录:" + file.getAbsolutePath());
                        String fileString = UUtils.getFileString(file);
                        EditTextDialog editTextDialog = new EditTextDialog(RunWindowActivity.this);
                        editTextDialog.setStringData(fileString);
                        editTextDialog.setStartCommand(new EditTextDialog.StartCommand() {
                            @Override
                            public void startCommand(String string) {
                                runCommite(string + "\n",true);
                            }
                        });
                        editTextDialog.show();
                        editTextDialog.setVisible(true);
                    }
                });

                fileListDialog1.setOnOnDefClickListener(new FileListDialog.OnDefClickListener() {
                    @Override
                    public void onDefClickListener(String file) {
                        //默认
                        fileListDialog1.dismiss();

                        EditTextDialog editTextDialog = new EditTextDialog(RunWindowActivity.this);

                        editTextDialog.show();

                        editTextDialog.setVisible(true);

                        editTextDialog.setStringData(SaveData.getData("mac_config"));

                        editTextDialog.setEditStartCommand(new EditTextDialog.EditStartCommand() {
                            @Override
                            public void editCommand(String comm) {
                                UUtils.showMsg(UUtils.getString(R.string.成功));
                                SaveData.saveData("mac_config",comm);
                            }
                        });

                        editTextDialog.setSystemSwitchListener(new EditTextDialog.SystemSwitchListener() {
                            @Override
                            public void switchEdit(EditText editText) {
                                SaveData.saveData("mac_config","cd ~ && qemu-system-ppc -hda /data/data/com.termux/files/home/storage/shared/xinhao/windows/" + mMac.getName() + " -M mac99 -m 512 -g 800x600x32 -machine usb=on -device  usb-tablet  -vnc :33\n");
                                editText.setText("cd ~ && qemu-system-ppc -hda /data/data/com.termux/files/home/storage/shared/xinhao/windows/" + mMac.getName() + " -M mac99 -m 512 -g 800x600x32 -machine usb=on -device  usb-tablet  -vnc :33\n");
                            }
                        });

                        editTextDialog.setStartCommand(new EditTextDialog.StartCommand() {
                            @Override
                            public void startCommand(String string) {
                                runCommite(string + "\n",true);
                            }
                        });


                    }
                });
            }
        });


        other.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SwitchQemuDialog switchQemuDialog = new SwitchQemuDialog(RunWindowActivity.this);
                switchQemuDialog.show();
                final String[] cmd = {""};
                switchQemuDialog.setSwitchJGListener(new SwitchQemuDialog.SwitchJGListener() {
                    @Override
                    public void SwitchSystem(String string) {
                        switchQemuDialog.dismiss();

                        FileListDialog fileListDialog = new FileListDialog(RunWindowActivity.this);
                        fileListDialog.show();
                        fileListDialog.setBoomBtnVisible(false);
                        fileListDialog.setOnItemFileClickListener(file -> {
                            fileListDialog.dismiss();
                            switch (string){
                                case "amd":
                                    cmd[0] = "qemu-system-x86_64 -hda /data/data/com.termux/files/home/storage/shared/xinhao/windows/"  + file.getName() + " -boot d -m 800  -device e1000,id=d-net1  -vnc :33 -cpu Skylake-Server-IBRS --accel tcg,thread=multi -smp 4";
                                    break;
                                case "i386":
                                    cmd[0] = "qemu-system-i386 -hda /data/data/com.termux/files/home/storage/shared/xinhao/windows/"  + file.getName() + " -boot d -m 800  -device e1000,id=d-net1  -vnc :33 -cpu Skylake-Server-IBRS --accel tcg,thread=multi -smp 4";
                                    break;
                                case "mac":
                                    cmd[0] = "qemu-system-ppc -hda /data/data/com.termux/files/home/storage/shared/xinhao/windows/"  + file.getName() + " -M mac99 -m 512 -g 800x600x32 -machine usb=on -device  usb-tablet  -vnc :33";
                                    break;
                            }

                            FileListDialog fileListDialog1 = new FileListDialog(RunWindowActivity.this);
                            fileListDialog1.show();
                            fileListDialog1.setFilePath(mWinConfig);
                            fileListDialog1.setTitleText(UUtils.getString(R.string.请选择模拟器配置文件));

                            fileListDialog1.setBoomBtnVisible(true);
                            fileListDialog1.setOnItemFileClickListener(file12 -> {
                                fileListDialog1.dismiss();
                                UUtils.showLog("获取文件目录:" + file12.getAbsolutePath());
                                String fileString = UUtils.getFileString(file12);
                                EditTextDialog editTextDialog = new EditTextDialog(RunWindowActivity.this);
                                editTextDialog.setStringData(fileString);
                                editTextDialog.setVisible(false);
                                editTextDialog.setStartCommand(string12 -> runCommite(string12 + "\n",true));
                                editTextDialog.show();
                                editTextDialog.setVisible(true);

                            });

                            fileListDialog1.setOnOnDefClickListener(file1 -> {
                                //默认
                                fileListDialog1.dismiss();
                                EditTextDialog editTextDialog = new EditTextDialog(RunWindowActivity.this);
                                editTextDialog.setStringData(cmd[0]);
                                editTextDialog.setVisible(false);
                                editTextDialog.setStartCommand(string1 -> runCommite(string1 + "\n",true));
                                editTextDialog.setSystemSwitchListener(editText -> editTextDialog.setStringData(cmd[0]));
                                editTextDialog.setEditStartCommand(string13 -> {
                                    FileNameDialog fileNameDialog = new FileNameDialog(RunWindowActivity.this);
                                    fileNameDialog.setOnSaveFileNameListener(name -> {
                                            fileNameDialog.dismiss();
                                            UUtils.setFileString(new File(FileUrl.INSTANCE.getZeroTermuxWindowsConfig(), name), string13);
                                        });
                                    fileNameDialog.show();
                                });
                                editTextDialog.show();
                                editTextDialog.setVisible(true);
                            });

                        });
                    }
                });



            }
        });

        mac.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                try {
                    Intent intent = new Intent();

                    intent.setAction("com.utermux.action.vnc");
                    intent.putExtra("utermux_as", "false");
                    intent.putExtra("address", "127.0.0.1");
                    intent.putExtra("port", "5933");
                    intent.putExtra("password", "");
                    startActivity(intent);
                } catch (Exception e) {
                    e.printStackTrace();
                    UUtils.showMsg(UUtils.getString(R.string.请在下载站下载VNC插件));

                }

            }
        });


        File fileProot = new File("/data/data/com.termux/files/usr/bin/termux-chroot");
        File fileWget = new File("/data/data/com.termux/files/usr/bin/wget");



        if (!fileProot.exists() || !fileWget.exists()) {


            AlertDialog.Builder ab = new AlertDialog.Builder(RunWindowActivity.this);

            ab.setTitle(UUtils.getString(R.string.环境不达要求));

            ab.setMessage(UUtils.getString(R.string.你没有安装tyu65t6y5u));

            ab.setNegativeButton(UUtils.getString(R.string.给我安装), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    TermuxActivity.mTerminalView.sendTextToTerminal("pkg in wget proot -y" + "\n");
                    ab.create().dismiss();
                    finish();
                }
            });

            ab.setPositiveButton(UUtils.getString(R.string.不安装), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    ab.create().dismiss();
                    finish();
                }
            });
            ab.setCancelable(false);

            ab.show();


            return;
        }


        if(!mFile2.exists()){
            androidx.appcompat.app.AlertDialog.Builder alertDialog = new androidx.appcompat.app.AlertDialog.Builder(this);

            alertDialog.setTitle(UUtils.getString(R.string.错误));

            alertDialog.setCancelable(false);

            alertDialog.setMessage(UUtils.getString(R.string.你没有安装相关环境qemuppp5555p));

            alertDialog.setNegativeButton(UUtils.getString(R.string.在线安装), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    alertDialog.create().dismiss();

                    LoadingDialog loadingDialog = new LoadingDialog(RunWindowActivity.this);
                    loadingDialog.show();
                    loadingDialog.setCancelable(false);

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            writerFile(new File(("/data/data/com.termux/files/usr/bin/qemu-system-ppc")));

                            try {
                                Thread.sleep(2000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }


                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    loadingDialog.dismiss();
                                    TermuxActivity.mTerminalView.sendTextToTerminal("cd ~ && cd .. && cd usr && cd bin && chmod 777 qemu-system-ppc && cd ~\n");
                                    TermuxActivity.mTerminalView.sendTextToTerminal("pkg update -y && pkg install x11-repo unstable-repo -y && pkg install qemu-utils qemu-system-x86_64-headless  qemu-system-i386-headless -y &&  termux-setup-storage\n");
                                    TermuxActivity.mTerminalView.sendTextToTerminal("y\n");

                                    TermuxActivity.mTerminalView.sendTextToTerminal("y\n");
                                    TermuxActivity.mTerminalView.sendTextToTerminal("y\n");
                                    TermuxActivity.mTerminalView.sendTextToTerminal("y\n");
                                    TermuxActivity.mTerminalView.sendTextToTerminal("y\n");
                                    RunWindowActivity.this.finish();
                                    alertDialog.create().dismiss();
                                    Toast.makeText(RunWindowActivity.this, UUtils.getString(R.string.请等待安装完成在进入), Toast.LENGTH_SHORT).show();
                                    finish();
                                }
                            });

                        }
                    }).start();


                }
            });

            alertDialog.setPositiveButton(UUtils.getString(R.string.稍后), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                    alertDialog.create().dismiss();
                    finish();
                }
            });

            alertDialog.show();

        }

        if (mFile.exists() || mFile1.exists()) {
            qemu_state.setText(UUtils.getString(R.string.qamu环境开));
            qemu_install.setVisibility(View.GONE);
        } else {
            qemu_state.setText(UUtils.getString(R.string.qamu环境关));
            qemu_install.setVisibility(View.VISIBLE);
        }

        qemu_install.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (TermuxActivity.mTerminalView != null) {
                    TermuxActivity.mTerminalView.sendTextToTerminal("pkg update -y && pkg install x11-repo unstable-repo -y && pkg install qemu-utils qemu-system-x86_64-headless  qemu-system-i386-headless -y &&  termux-setup-storage\n");
                    TermuxActivity.mTerminalView.sendTextToTerminal("y\n");

                    TermuxActivity.mTerminalView.sendTextToTerminal("y\n");
                    TermuxActivity.mTerminalView.sendTextToTerminal("y\n");
                    TermuxActivity.mTerminalView.sendTextToTerminal("y\n");
                    TermuxActivity.mTerminalView.sendTextToTerminal("y\n");
                    RunWindowActivity.this.finish();
                }else{
                    UUtils.showMsg(UUtils.getString(R.string.系统初始化失败));
                }
            }
        });


        win8.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(!mWin7.exists()){
                    getIso();
                    return;
                }

                if(!mWin7RunPath.exists()){

                    UUtils.showMsg(UUtils.getString(R.string.正在疏通所需要的环境));
                    TermuxActivity.mTerminalView.sendTextToTerminal("echo "+UUtils.getString(R.string.请输入回车键继续55) + "\n");
                    TermuxActivity.mTerminalView.sendTextToTerminal("termux-setup-storage");
                    finish();
                    return;
                }


                FileListDialog fileListDialog1 = new FileListDialog(RunWindowActivity.this);
                fileListDialog1.show();
                fileListDialog1.setFilePath(mWinConfig);
                fileListDialog1.setTitleText(UUtils.getString(R.string.请选择模拟器配置文件));

                fileListDialog1.setBoomBtnVisible(true);
                fileListDialog1.setOnItemFileClickListener(new FileListDialog.OnItemFileClickListener() {
                    @Override
                    public void onItemClick(File file) {
                        fileListDialog1.dismiss();
                        UUtils.showLog("获取文件目录:" + file.getAbsolutePath());
                        String fileString = UUtils.getFileString(file);

                        EditTextDialog editTextDialog = new EditTextDialog(RunWindowActivity.this);

                        editTextDialog.setStringData(fileString);

                        editTextDialog.setVisible(false);


                        editTextDialog.setStartCommand(new EditTextDialog.StartCommand() {
                            @Override
                            public void startCommand(String string) {
                                runCommite(string + "\n",true);
                            }
                        });

                        editTextDialog.show();

                        editTextDialog.setVisible(true);

                    }
                });

                fileListDialog1.setOnOnDefClickListener(new FileListDialog.OnDefClickListener() {
                    @Override
                    public void onDefClickListener(String file) {
                        //默认
                        fileListDialog1.dismiss();

                        EditTextDialog editTextDialog = new EditTextDialog(RunWindowActivity.this);

                        editTextDialog.show();

                        editTextDialog.setVisible(true);

                        editTextDialog.setStringData(SaveData.getData("win7_config"));

                        editTextDialog.setEditStartCommand(new EditTextDialog.EditStartCommand() {
                            @Override
                            public void editCommand(String comm) {
                                UUtils.showMsg(UUtils.getString(R.string.成功));
                                SaveData.saveData("win7_config",comm);
                            }
                        });

                        editTextDialog.setSystemSwitchListener(new EditTextDialog.SystemSwitchListener() {
                            @Override
                            public void switchEdit(EditText editText) {
                                SaveData.saveData("win7_config","cd ~ && qemu-system-i386 -hda /data/data/com.termux/files/home/storage/shared/xinhao/windows/" + mWin7RunPath.getName() + " -boot d -m 800  -device e1000,id=d-net1  -vnc :33 -cpu Skylake-Server-IBRS --accel tcg,thread=multi -smp 4\n");
                                editText.setText("cd ~ && qemu-system-i386 -hda /data/data/com.termux/files/home/storage/shared/xinhao/windows/" + mWin7RunPath.getName() + " -boot d -m 800  -device e1000,id=d-net1  -vnc :33 -cpu Skylake-Server-IBRS --accel tcg,thread=multi -smp 4\n");
                            }
                        });

                        editTextDialog.setStartCommand(new EditTextDialog.StartCommand() {
                            @Override
                            public void startCommand(String string) {
                                runCommite(string + "\n",true);
                            }
                        });

                    }
                });
            }
        });


        win10.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(!mWin10.exists()){
                    getIso();
                    return;
                }

                if(!mWin10RunPath.exists()){

                    UUtils.showMsg(UUtils.getString(R.string.正在疏通所需要的环境));
                    TermuxActivity.mTerminalView.sendTextToTerminal("echo "+UUtils.getString(R.string.请输入回车键继续55) + "\n");
                    TermuxActivity.mTerminalView.sendTextToTerminal("termux-setup-storage");
                    finish();
                    return;
                }



                FileListDialog fileListDialog1 = new FileListDialog(RunWindowActivity.this);
                fileListDialog1.show();
                fileListDialog1.setFilePath(mWinConfig);
                fileListDialog1.setTitleText(UUtils.getString(R.string.请选择模拟器配置文件));

                fileListDialog1.setBoomBtnVisible(true);
                fileListDialog1.setOnItemFileClickListener(new FileListDialog.OnItemFileClickListener() {
                    @Override
                    public void onItemClick(File file) {
                        fileListDialog1.dismiss();
                        UUtils.showLog("获取文件目录:" + file.getAbsolutePath());
                        String fileString = UUtils.getFileString(file);

                        EditTextDialog editTextDialog = new EditTextDialog(RunWindowActivity.this);

                        editTextDialog.setStringData(fileString);

                        editTextDialog.setVisible(false);


                        editTextDialog.setStartCommand(new EditTextDialog.StartCommand() {
                            @Override
                            public void startCommand(String string) {
                                runCommite(string + "\n",true);
                            }
                        });

                        editTextDialog.show();

                        editTextDialog.setVisible(true);

                    }
                });

                fileListDialog1.setOnOnDefClickListener(new FileListDialog.OnDefClickListener() {
                    @Override
                    public void onDefClickListener(String file) {
                        //默认
                        fileListDialog1.dismiss();

                        EditTextDialog editTextDialog = new EditTextDialog(RunWindowActivity.this);

                        editTextDialog.show();

                        editTextDialog.setVisible(true);

                        editTextDialog.setStringData(SaveData.getData("win10_config"));

                        editTextDialog.setEditStartCommand(new EditTextDialog.EditStartCommand() {
                            @Override
                            public void editCommand(String comm) {
                                UUtils.showMsg(UUtils.getString(R.string.成功));
                                SaveData.saveData("win10_config",comm);
                            }
                        });

                        editTextDialog.setSystemSwitchListener(new EditTextDialog.SystemSwitchListener() {
                            @Override
                            public void switchEdit(EditText editText) {
                                SaveData.saveData("win10_config","cd ~ && qemu-system-i386 -hda /data/data/com.termux/files/home/storage/shared/xinhao/windows/" + mWinxpRunPath.getName() + " -boot d -m 900  -device rtl8139,id=d-net1  -vnc :33 -cpu Skylake-Server-IBRS --accel tcg,thread=multi -smp 4\n");
                                editText.setText("cd ~ && qemu-system-i386 -hda /data/data/com.termux/files/home/storage/shared/xinhao/windows/" + mWin10RunPath.getName() + " -boot d -m 900  -device rtl8139,id=d-net1  -vnc :33 -cpu Skylake-Server-IBRS --accel tcg,thread=multi -smp 4\n");
                            }
                        });

                        editTextDialog.setStartCommand(new EditTextDialog.StartCommand() {
                            @Override
                            public void startCommand(String string) {
                                runCommite(string + "\n",true);
                            }
                        });

                    }
                });
            }
        });

        win7.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(!mWinxp.exists()){
                    getIso();
                    return;
                }

                if(!mWinxpRunPath.exists()){

                    UUtils.showMsg(UUtils.getString(R.string.正在疏通所需要的环境));
                    TermuxActivity.mTerminalView.sendTextToTerminal("echo "+UUtils.getString(R.string.请输入回车键继续55) + "\n");
                    TermuxActivity.mTerminalView.sendTextToTerminal("termux-setup-storage");
                    finish();
                    return;
                }



                FileListDialog fileListDialog1 = new FileListDialog(RunWindowActivity.this);
                fileListDialog1.show();
                fileListDialog1.setFilePath(mWinConfig);
                fileListDialog1.setTitleText(UUtils.getString(R.string.请选择模拟器配置文件));

                fileListDialog1.setBoomBtnVisible(true);
                fileListDialog1.setOnItemFileClickListener(new FileListDialog.OnItemFileClickListener() {
                    @Override
                    public void onItemClick(File file) {
                        fileListDialog1.dismiss();
                        UUtils.showLog("获取文件目录:" + file.getAbsolutePath());
                        String fileString = UUtils.getFileString(file);

                        EditTextDialog editTextDialog = new EditTextDialog(RunWindowActivity.this);

                        editTextDialog.setStringData(fileString);




                        editTextDialog.setStartCommand(new EditTextDialog.StartCommand() {
                            @Override
                            public void startCommand(String string) {
                                runCommite(string + "\n",true);
                            }
                        });

                        editTextDialog.show();

                        editTextDialog.setVisible(true);

                    }
                });

                fileListDialog1.setOnOnDefClickListener(new FileListDialog.OnDefClickListener() {
                    @Override
                    public void onDefClickListener(String file) {
                        //默认
                        fileListDialog1.dismiss();

                        EditTextDialog editTextDialog = new EditTextDialog(RunWindowActivity.this);

                        editTextDialog.show();

                        editTextDialog.setVisible(true);

                        editTextDialog.setStringData(SaveData.getData("winxp_config"));

                        editTextDialog.setEditStartCommand(new EditTextDialog.EditStartCommand() {
                            @Override
                            public void editCommand(String comm) {
                                UUtils.showMsg(UUtils.getString(R.string.成功));
                                SaveData.saveData("winxp_config",comm);
                            }
                        });

                        editTextDialog.setSystemSwitchListener(new EditTextDialog.SystemSwitchListener() {
                            @Override
                            public void switchEdit(EditText editText) {
                                SaveData.saveData("winxp_config","cd ~ && qemu-system-i386 -hda /data/data/com.termux/files/home/storage/shared/xinhao/windows/" + mWinxpRunPath.getName() + " -boot d -m 900  -device rtl8139,id=d-net1  -vnc :33 -cpu Skylake-Server-IBRS --accel tcg,thread=multi -smp 4\n");

                                editText.setText("cd ~ && qemu-system-i386 -hda /data/data/com.termux/files/home/storage/shared/xinhao/windows/" + mWinxpRunPath.getName() + " -boot d -m 900  -device rtl8139,id=d-net1  -vnc :33 -cpu Skylake-Server-IBRS --accel tcg,thread=multi -smp 4\n");
                            }
                        });

                        editTextDialog.setStartCommand(new EditTextDialog.StartCommand() {
                            @Override
                            public void startCommand(String string) {
                                runCommite(string + "\n",true);
                            }
                        });


                    }
                });








            }
        });

        dis_install.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

               UUtils.showMsg(UUtils.getString(R.string.请在下载站下载VNC插件));


            }
        });

    }

    private void setConfig(){

        String win10_config = SaveData.getData("win10_config");
        if(win10_config == null || win10_config.isEmpty() || "def".equals(win10_config)){

            SaveData.saveData("win10_config","cd ~ && qemu-system-i386 -hda /data/data/com.termux/files/home/storage/shared/xinhao/windows/" + mWin10RunPath.getName() + " -boot d -m 900  -device rtl8139,id=d-net1  -vnc :33 -cpu Skylake-Server-IBRS --accel tcg,thread=multi -smp 4\n");

        }

        String win7_config = SaveData.getData("win7_config");
        if(win7_config == null || win7_config.isEmpty() || "def".equals(win7_config)){

            SaveData.saveData("win7_config","cd ~ && qemu-system-i386 -hda /data/data/com.termux/files/home/storage/shared/xinhao/windows/" + mWin7RunPath.getName() + " -boot d -m 800  -device e1000,id=d-net1  -vnc :33 -cpu Skylake-Server-IBRS --accel tcg,thread=multi -smp 4\n");

        }

        String winxp_config = SaveData.getData("winxp_config");
        if(winxp_config == null || winxp_config.isEmpty() || "def".equals(winxp_config)){

            SaveData.saveData("winxp_config","cd ~ && qemu-system-i386 -hda /data/data/com.termux/files/home/storage/shared/xinhao/windows/" + mWinxpRunPath.getName() + " -boot d -m 900  -device rtl8139,id=d-net1  -vnc :33 -cpu Skylake-Server-IBRS --accel tcg,thread=multi -smp 4\n");

        }

        String mac_config = SaveData.getData("mac_config");
        if(mac_config == null || mac_config.isEmpty() || "def".equals(mac_config)){

            SaveData.saveData("mac_config","cd ~ && qemu-system-ppc -hda /data/data/com.termux/files/home/storage/shared/xinhao/windows/" + mMac.getName() + " -M mac99 -m 512 -g 800x600x32 -machine usb=on -device  usb-tablet  -vnc :33\n");

        }


    }

    private void runCommite(String cmd,boolean isStartVnc){


   /*     if (mFileQemuStart.exists()) {
            boolean delete = mFileQemuStart.delete();
            if(!delete){
                UUtils.showMsg(UUtils.getString(R.string.文件操作失败pppp));
                return;
            }
        }
*/
        LoadingDialog loadingDialog = new LoadingDialog(RunWindowActivity.this);

        loadingDialog.show();

        new Thread(new Runnable() {
            @Override
            public void run() {

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        if (TermuxActivity.mTerminalView != null) {
                            TermuxActivity.mTerminalView.sendTextToTerminalCtrl("c",true);
                        }else{
                            UUtils.showMsg(UUtils.getString(R.string.系统初始化失败));
                            loadingDialog.dismiss();
                            return ;
                        }


                    }
                });

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }


                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        if (TermuxActivity.mTerminalView != null) {
                            TermuxActivity.mTerminalView.sendTextToTerminal("cd ~  \n");
                            TermuxActivity.mTerminalView.sendTextToTerminal("cd ~  \n");
                            TermuxActivity.mTerminalView.sendTextToTerminal("cd ~  \n");
                            TermuxActivity.mTerminalView.sendTextToTerminal("cd ~  \n");
                            TermuxActivity.mTerminalView.sendTextToTerminal("cd ~  \n");
                            TermuxActivity.mTerminalView.sendTextToTerminal("termux-setup-storage\n");
                            TermuxActivity.mTerminalView.sendTextToTerminal("y \n");
                            TermuxActivity.mTerminalView.sendTextToTerminal("y \n");
                            TermuxActivity.mTerminalView.sendTextToTerminal("y \n");
                            TermuxActivity.mTerminalView.sendTextToTerminal("y \n");
                            TermuxActivity.mTerminalView.sendTextToTerminal("y \n");

                            //mFileQemuStart

                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("#!/data/data/com.termux/files/usr/bin/bash\n");
                            stringBuilder.append("\n");
                            stringBuilder.append(cmd).append("\n");
                            stringBuilder.append("\n");
                            stringBuilder.append("am broadcast --user 0 \\\n");
                            stringBuilder.append("  --es com.termux.app.reload_style qemu_run_error \\\n");
                            stringBuilder.append("  -a com.termux.app.reload_style com.termux > /dev/null \\\n");


                            UUtils.setFileString(mFileQemuStart,stringBuilder.toString());
                            TermuxActivity.mTerminalView.sendTextToTerminal("cd ~\n");
                            TermuxActivity.mTerminalView.sendTextToTerminal("cd .qemustart\n");
                            TermuxActivity.mTerminalView.sendTextToTerminal("chmod 777 start.sh\n");



                        }else{
                            UUtils.showMsg(UUtils.getString(R.string.系统初始化失败));
                            loadingDialog.dismiss();
                            return ;
                        }

                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }


                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {

                                TermuxActivity.mTerminalView.sendTextToTerminal("./start.sh\n");
                                TermuxActivity.mTerminalView.sendTextToTerminal("cd ~\n");

                               // TermuxActivity.mTerminalView.sendTextToTerminal(cmd);
                            }
                        });

                    }
                });

                isError = false;
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {


                        try {
                            if (loadingDialog != null)
                                loadingDialog.dismiss();
                        }catch (Exception e){
                            e.printStackTrace();
                        }

                        if(isError){

                            EndDialog endDialog = new EndDialog(RunWindowActivity.this);

                            endDialog.show();

                            return;

                        }

                        Log.e("查看参数", "run: isError:" +  isError +"---isStartVnc:" + isStartVnc);
                        Log.e("查看参数", "run: 总体:"  + (isStartVnc && !isError));

                        if(isStartVnc){
                            try {
                                Log.e("查看参数", "开始打开vnc");
                                Intent intent = new Intent();
                                intent.setAction("com.utermux.action.vnc");
                                intent.putExtra("utermux_as", "false");
                                intent.putExtra("address", "127.0.0.1");
                                intent.putExtra("port", "5933");
                                intent.putExtra("password", "");
                                startActivity(intent);
                                Log.e("查看参数", "开始vnc打开结束");

                            } catch (Exception e) {

                                e.printStackTrace();

                                UUtils.showMsg(UUtils.getString(R.string.请在下载站下载VNC插件));

                            }
                        }

                    }
                });

            }
        }).start();


    }

    //写出文件
    private void writerFile(File mFile) {

        try {
            InputStream open = getResources().openRawResource(R.raw.qemu_system_ppc);

            int len = 0;
            byte[] lll = new byte[1024];

            if (!mFile.exists()) {
                mFile.createNewFile();
            }

            FileOutputStream fileOutputStream = new FileOutputStream(mFile);

            while ((len = open.read(lll)) != -1) {
                fileOutputStream.write(lll,0,len);
            }

            fileOutputStream.flush();
            open.close();
            fileOutputStream.close();
        } catch (Exception e) {

        }

    }




    private void getIso(){

        AlertDialog.Builder ab = new AlertDialog.Builder(RunWindowActivity.this);

        ab.setTitle(UUtils.getString(R.string.获取镜像));

        //链接: https://pan.baidu.com/s/17l6_bJ3EQN41I7Axs0USvQ 提取码: bxht

        ab.setMessage(UUtils.getString(R.string.是否前往下载系统));

        ab.setPositiveButton(UUtils.getString(R.string.前往), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                ab.create().dismiss();

                Intent intent = new Intent();
                intent.setData(Uri.parse("https://pan.baidu.com/s/18Ro-q9XMkowNZ1MWrTWCPw"));//Url 就是你要打开的网址
                intent.setAction(Intent.ACTION_VIEW);
                startActivity(intent); //启动浏览器

            }
        });

        ab.setNegativeButton(UUtils.getString(R.string.取消), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ab.create().dismiss();
            }
        });

        ab.show();


    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        TermuxData.getInstall().setIsQemuSul(null);
    }


    private boolean isError = false;

    @Override
    public void error() {

        UUtils.showLog("收到广播...");
        isError = true;

    }

    @Override
    protected void onStart() {
        super.onStart();
        registerReceiver(mBroadcastReceiever, new IntentFilter(RELOAD_STYLE_ACTION));
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(mBroadcastReceiever);
    }

    private static final String RELOAD_STYLE_ACTION = "com.termux.app.reload_style";

    private final BroadcastReceiver mBroadcastReceiever = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

                String whatToReload = intent.getStringExtra(RELOAD_STYLE_ACTION);

                if(whatToReload!= null && (!whatToReload.isEmpty()) &&(whatToReload.trim()).startsWith("qemu_run_error")){


                    TermuxData.IsQemuSul isQemuSul = TermuxData.getInstall().getmIsQemuSul();
                  //  UUtils.showMsg("收到广播ffffffffffffffffffffff" + isQemuSul);
                    isError = true;

                }


            }

    };
}
