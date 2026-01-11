package com.termux.zerocore.config.mainmenu.config;

import static com.termux.zerocore.config.mainmenu.MainMenuConfig.CODE_ZT_FEATURES;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.view.View;

import com.example.xh_lib.utils.UUtils;
import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.zerocore.dialog.VNCConnectionDialog;
import com.termux.zerocore.http.HTTPIP;
import com.termux.zerocore.popuwindow.MenuLeftPopuListWindow;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

public class VNCClickConfig extends BaseMenuClickConfig implements MenuLeftPopuListWindow.ItemClickPopuListener {
    @Override
    public int getType() {
        return CODE_ZT_FEATURES;
    }

    @Override
    public Drawable getIcon(Context context) {
        return context.getDrawable(R.mipmap.vnc_ico);
    }

    @Override
    public String getString(Context context) {
        return context.getString(R.string.VNC);
    }

    @Override
    public void onClick(View view, Context context) {
        ArrayList<MenuLeftPopuListWindow.MenuLeftPopuListData> menuLeftPopuListDatavnc = new ArrayList<>();
        //快速vnc
        MenuLeftPopuListWindow.MenuLeftPopuListData ksvnc = new MenuLeftPopuListWindow.MenuLeftPopuListData(R.mipmap.dsk, UUtils.getString(R.string.快速VNC), 10);
        menuLeftPopuListDatavnc.add(ksvnc);
        //自定vnc
        MenuLeftPopuListWindow.MenuLeftPopuListData zdvnc = new MenuLeftPopuListWindow.MenuLeftPopuListData(R.mipmap.dsk, UUtils.getString(R.string.自定VNC), 11);
        menuLeftPopuListDatavnc.add(zdvnc);
        //高级vnc
        MenuLeftPopuListWindow.MenuLeftPopuListData gjvnc = new MenuLeftPopuListWindow.MenuLeftPopuListData(R.mipmap.dsk, UUtils.getString(R.string.高级VNC), 12);
        menuLeftPopuListDatavnc.add(gjvnc);

        showMenuDialog(menuLeftPopuListDatavnc, view);
    }

    private void showMenuDialog(ArrayList<MenuLeftPopuListWindow.MenuLeftPopuListData> arrayList, View showView) {
        MenuLeftPopuListWindow menuLeftPopuListWindow = new MenuLeftPopuListWindow(mContext);
        menuLeftPopuListWindow.setItemClickPopuListener(this);
        menuLeftPopuListWindow.setListData(arrayList);
        menuLeftPopuListWindow.showAsDropDown(showView, 250, -200);
    }

    @Override
    public void itemClick(int id, int index, @Nullable MenuLeftPopuListWindow mMenuLeftPopuListWindow) {
        TermuxActivity termuxActivity = (TermuxActivity) mContext;
        switch (id) {
            //快速
            case 10:
                UUtils.showLog("插件:快速");
                try {
                    Intent intent = new Intent();
                    intent.setAction("com.utermux.action.vnc");
                    intent.putExtra("utermux_as", "false");
                    intent.putExtra("address", "127.0.0.1");
                    intent.putExtra("port", "5901");
                    intent.putExtra("password", "123456");
                    termuxActivity.startActivity(intent);
                } catch (Exception e) {
                    e.printStackTrace();
                    UUtils.showLog("插件:" + e.toString());
                    UUtils.showMsg(UUtils.getString(R.string.请下载插件));
                    termuxActivity.startHttp1(HTTPIP.IP);
                }
                break;
            //自定
            case 11:
                VNCConnectionDialog vncConnectionDialog = new VNCConnectionDialog(mContext);
                vncConnectionDialog.show();
                vncConnectionDialog.setCancelable(false);
                vncConnectionDialog.getOk().setOnClickListener(v -> {
                    try {
                        Intent intent = new Intent();
                        intent.setAction("com.utermux.action.vnc");
                        intent.putExtra("utermux_as", "false");
                        intent.putExtra("address", vncConnectionDialog.getAddress().getText().toString());
                        intent.putExtra("port", vncConnectionDialog.getPort().getText().toString());
                        intent.putExtra("password", vncConnectionDialog.getPassword().getText().toString());
                        termuxActivity.startActivity(intent);
                    } catch (Exception e) {
                        e.printStackTrace();
                        UUtils.showLog("插件:" + e.toString());
                        UUtils.showMsg(UUtils.getString(R.string.请下载插件));
                        termuxActivity.startHttp1(HTTPIP.IP);
                    }
                });
                break;
            //高级
            case 12:
                try {
                    Intent intent = new Intent();
                    intent.setAction("com.utermux.action.vnc");
                    intent.putExtra("utermux_as", "true");
                    mContext.startActivity(intent);
                } catch (Exception e) {
                    e.printStackTrace();
                    UUtils.showLog("插件:" + e.toString());
                    UUtils.showMsg(UUtils.getString(R.string.请下载插件));
                    termuxActivity.startHttp1(HTTPIP.IP);
                }
                break;
        }
    }
}
