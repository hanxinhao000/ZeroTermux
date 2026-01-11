package com.termux.zerocore.config.mainmenu.config;

import static com.termux.zerocore.config.mainmenu.MainMenuConfig.CODE_COMMON_FUNCTIONS;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.view.View;

import com.example.xh_lib.utils.UUtils;
import com.hjq.permissions.OnPermissionCallback;
import com.hjq.permissions.Permission;
import com.hjq.permissions.XXPermissions;
import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.zerocore.code.CodeString;
import com.termux.zerocore.dialog.LoadingDialog;
import com.termux.zerocore.dialog.SwitchDialog;
import com.termux.zerocore.popuwindow.MenuLeftPopuListWindow;
import com.termux.zerocore.url.FileUrl;
import com.termux.zerocore.utermux_windows.qemu.activity.RunWindowActivity;

import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class QEMUClickConfig extends BaseMenuClickConfig implements MenuLeftPopuListWindow.ItemClickPopuListener {
    @Override
    public int getType() {
        return CODE_COMMON_FUNCTIONS;
    }

    @Override
    public Drawable getIcon(Context context) {
        return context.getDrawable(R.mipmap.windows);
    }

    @Override
    public String getString(Context context) {
        return context.getString(R.string.QEMU);
    }

    @Override
    public void onClick(View view, Context context) {
        ArrayList<MenuLeftPopuListWindow.MenuLeftPopuListData> menuLeftPopuListData1 = new ArrayList<>();
        //官方
        MenuLeftPopuListWindow.MenuLeftPopuListData qemuData = new MenuLeftPopuListWindow.MenuLeftPopuListData(R.mipmap.qemu_ico_hai, UUtils.getString(R.string.海的QEMU), 5);
        menuLeftPopuListData1.add(qemuData);

        MenuLeftPopuListWindow.MenuLeftPopuListData zeroData = new MenuLeftPopuListWindow.MenuLeftPopuListData(R.mipmap.windows_xp, UUtils.getString(R.string.Zero), 501);
        menuLeftPopuListData1.add(zeroData);

        MenuLeftPopuListWindow.MenuLeftPopuListData win7Data = new MenuLeftPopuListWindow.MenuLeftPopuListData(R.mipmap.windows, UUtils.getString(R.string.Win7模拟), 502);
        menuLeftPopuListData1.add(win7Data);

        MenuLeftPopuListWindow.MenuLeftPopuListData winXpData = new MenuLeftPopuListWindow.MenuLeftPopuListData(R.mipmap.windows_xp_ico, UUtils.getString(R.string.WinXp), 503);
        menuLeftPopuListData1.add(winXpData);

        showMenuDialog(menuLeftPopuListData1, view, context);
    }

    private void showMenuDialog(ArrayList<MenuLeftPopuListWindow.MenuLeftPopuListData> arrayList, View showView, Context context) {
        MenuLeftPopuListWindow menuLeftPopuListWindow = new MenuLeftPopuListWindow(context);
        menuLeftPopuListWindow.setItemClickPopuListener(this);
        menuLeftPopuListWindow.setListData(arrayList);
        menuLeftPopuListWindow.showAsDropDown(showView, 250, -200);
    }

    @Override
    public void itemClick(int id, int index, @Nullable MenuLeftPopuListWindow mMenuLeftPopuListWindow) {
        switch (id) {
            case 5:
                SwitchDialog msgQemuLine = switchDialogShow(UUtils.getString(R.string.选择方式), UUtils.getString(R.string.要获取最新版本), mContext);
                msgQemuLine.show();
                msgQemuLine.getOk().setText(UUtils.getString(R.string.线上脚本));
                msgQemuLine.getOk().setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        msgQemuLine.dismiss();
                        TermuxActivity.mTerminalView.sendTextToTerminal(CodeString.INSTANCE.getRunLineQemu() + "\n");

                    }
                });
                msgQemuLine.getCancel().setText(UUtils.getString(R.string.本地脚本));
                msgQemuLine.getCancel().setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        msgQemuLine.dismiss();
                        LoadingDialog loadingDialog = new LoadingDialog(mContext);
                        loadingDialog.show();
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                UUtils.writerFile("qemu/utqemu.sh", new File(FileUrl.INSTANCE.getMainHomeUrl(), "/utqemu.sh"));
                                ((Activity) mContext).runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        TermuxActivity.mTerminalView.sendTextToTerminal(CodeString.INSTANCE.getRunQemuSh());
                                        loadingDialog.dismiss();
                                    }
                                });
                            }
                        }).start();
                    }
                });
                msgQemuLine.setCancelable(true);
                break;

            case 501:
                mContext.startActivity(new Intent(mContext, RunWindowActivity.class));
                break;
            case 502:
                XXPermissions.with(mContext)
                    .permission(Permission.WRITE_EXTERNAL_STORAGE)
                    .permission(Permission.READ_EXTERNAL_STORAGE)
                    .request(new OnPermissionCallback() {

                        @Override
                        public void onGranted(List<String> permissions, boolean all) {
                            if (all) {
                                File zeroTermuxShare = FileUrl.INSTANCE.getZeroTermuxShare();
                                if (!zeroTermuxShare.exists()) {
                                    zeroTermuxShare.mkdirs();
                                }
                                UUtils.writerFile("qemu/qemu_win7.sh", new File(FileUrl.INSTANCE.getMainHomeUrl(), "/qemu_win7.sh"));
                                TermuxActivity.mTerminalView.sendTextToTerminal(CodeString.INSTANCE.getRunWin7Sh());
                            } else {
                                UUtils.showMsg(UUtils.getString(R.string.no_permission));
                            }
                        }

                        @Override
                        public void onDenied(List<String> permissions, boolean never) {
                            if (never) {
                                UUtils.showMsg(UUtils.getString(R.string.no_permission));
                                // 如果是被永久拒绝就跳转到应用权限系统设置页面
                                XXPermissions.startPermissionActivity(mContext, permissions);
                            } else {
                                UUtils.showMsg(UUtils.getString(R.string.no_permission));
                            }
                        }
                    });
                break;

            case 503:
                XXPermissions.with(mContext)
                    .permission(Permission.WRITE_EXTERNAL_STORAGE)
                    .permission(Permission.READ_EXTERNAL_STORAGE)
                    .request(new OnPermissionCallback() {

                        @Override
                        public void onGranted(List<String> permissions, boolean all) {
                            if (all) {
                                File zeroTermuxShare = FileUrl.INSTANCE.getZeroTermuxShare();
                                if (!zeroTermuxShare.exists()) {
                                    zeroTermuxShare.mkdirs();
                                }
                                UUtils.writerFile("qemu/qemu_winxp.sh", new File(FileUrl.INSTANCE.getMainHomeUrl(), "/qemu_winxp.sh"));
                                TermuxActivity.mTerminalView.sendTextToTerminal(CodeString.INSTANCE.getRunWinXPSh());
                            } else {
                                UUtils.showMsg(UUtils.getString(R.string.no_permission));
                            }
                        }

                        @Override
                        public void onDenied(List<String> permissions, boolean never) {
                            if (never) {
                                UUtils.showMsg(UUtils.getString(R.string.no_permission));
                                // 如果是被永久拒绝就跳转到应用权限系统设置页面
                                XXPermissions.startPermissionActivity(mContext, permissions);
                            } else {
                                UUtils.showMsg(UUtils.getString(R.string.no_permission));
                            }
                        }
                    });
                break;
        }
    }
}
