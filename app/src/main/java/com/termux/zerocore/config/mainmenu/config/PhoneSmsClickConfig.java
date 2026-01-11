package com.termux.zerocore.config.mainmenu.config;

import static com.termux.zerocore.config.mainmenu.MainMenuConfig.CODE_ZT_FEATURES;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;

import com.example.xh_lib.utils.UUtils;
import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.zerocore.code.CodeString;
import com.termux.zerocore.dialog.SwitchDialog;
import com.termux.zerocore.popuwindow.MenuLeftPopuListWindow;
import com.termux.zerocore.url.FileUrl;

import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;

public class PhoneSmsClickConfig extends BaseMenuClickConfig implements MenuLeftPopuListWindow.ItemClickPopuListener {
    @Override
    public int getType() {
        return CODE_ZT_FEATURES;
    }

    @Override
    public Drawable getIcon(Context context) {
        return context.getDrawable(R.mipmap.duanxin_ico);
    }

    @Override
    public String getString(Context context) {
        return context.getString(R.string.短信通话);
    }

    @Override
    public void onClick(View view, Context context) {
        ArrayList<MenuLeftPopuListWindow.MenuLeftPopuListData> menuphoneMsg = new ArrayList<>();
        MenuLeftPopuListWindow.MenuLeftPopuListData msg_phone = new MenuLeftPopuListWindow.MenuLeftPopuListData(R.mipmap.install_msg_phone, UUtils.getString(R.string.安装短信读取工具), 6);
        menuphoneMsg.add(msg_phone);
        showMenuDialog(menuphoneMsg, view);
    }

    private void showMenuDialog(ArrayList<MenuLeftPopuListWindow.MenuLeftPopuListData> arrayList, View showView) {
        MenuLeftPopuListWindow menuLeftPopuListWindow = new MenuLeftPopuListWindow(mContext);
        menuLeftPopuListWindow.setItemClickPopuListener(this);
        menuLeftPopuListWindow.setListData(arrayList);
        menuLeftPopuListWindow.showAsDropDown(showView, 250, -200);
    }

    @Override
    public void itemClick(int id, int index, @Nullable MenuLeftPopuListWindow mMenuLeftPopuListWindow) {
        switch (id) {
            case 6:
                SwitchDialog msg = switchDialogShow(UUtils.getString(R.string.警告),
                    UUtils.getString(R.string.该操作有风险), mContext);
                msg.getCancel().setOnClickListener(v -> msg.dismiss());
                msg.getOk().setOnClickListener(v -> {
                    msg.dismiss();
                    File file = new File(FileUrl.INSTANCE.getSmsUrl());
                    if (file.exists()) {
                        UUtils.showMsg(UUtils.getString(R.string.您已安装工具));
                    } else {
                        UUtils.writerFile("runcommand/smsread", new File(FileUrl.INSTANCE.getSmsUrl()));
                        UUtils.writerFile("runcommand/readcontacts", new File(FileUrl.INSTANCE.getPhoneUrl()));
                        TermuxActivity.mTerminalView.sendTextToTerminal(CodeString.INSTANCE.getRunsmsChomdSh());
                        TermuxActivity.mTerminalView.sendTextToTerminal(CodeString.INSTANCE.getRunPhoneChomdSh());
                        UUtils.showMsg(UUtils.getString(R.string.安装完成));
                    }
                });
                break;
        }
    }
}
