package com.termux.zerocore.config.mainmenu.config;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;

import com.example.xh_lib.utils.UUtils;
import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.zerocore.code.CodeString;
import com.termux.zerocore.config.mainmenu.MainMenuConfig;
import com.termux.zerocore.dialog.SwitchDialog;
import com.termux.zerocore.popuwindow.MenuLeftPopuListWindow;
import com.termux.zerocore.url.FileUrl;

import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
// 切换源
public class SwitchSourceClickConfig extends BaseMenuClickConfig implements MenuLeftPopuListWindow.ItemClickPopuListener {
    @Override
    public int getType() {
        return MainMenuConfig.CODE_COMMON_FUNCTIONS;
    }

    @Override
    public Drawable getIcon(Context context) {
        return context.getDrawable(R.mipmap.code_view);
    }

    @Override
    public String getString(Context context) {
        return context.getString(R.string.切换源);
    }

    @Override
    public void initViewStatus(Context context) {
        super.initViewStatus(context);
    }

    @Override
    public void onClick(View view, Context context) {
        ArrayList<MenuLeftPopuListWindow.MenuLeftPopuListData> menuLeftPopuListData = new ArrayList<>();

        //清华
        MenuLeftPopuListWindow.MenuLeftPopuListData qinghua = new MenuLeftPopuListWindow.MenuLeftPopuListData(R.mipmap.qinghua_ico, UUtils.getString(R.string.清华源), 1);
        menuLeftPopuListData.add(qinghua);
        //北京
        MenuLeftPopuListWindow.MenuLeftPopuListData beijing = new MenuLeftPopuListWindow.MenuLeftPopuListData(R.mipmap.beijing, UUtils.getString(R.string.北京源), 2);
        menuLeftPopuListData.add(beijing);
        //官方
        MenuLeftPopuListWindow.MenuLeftPopuListData guanfang = new MenuLeftPopuListWindow.MenuLeftPopuListData(R.mipmap.guanfang, UUtils.getString(R.string.官方源), 3);
        menuLeftPopuListData.add(guanfang);
        //NJU
        MenuLeftPopuListWindow.MenuLeftPopuListData nju = new MenuLeftPopuListWindow.MenuLeftPopuListData(R.mipmap.nju_ico, UUtils.getString(R.string.nju), 496);
        menuLeftPopuListData.add(nju);
        //ustc
        MenuLeftPopuListWindow.MenuLeftPopuListData ustc = new MenuLeftPopuListWindow.MenuLeftPopuListData(R.mipmap.mingl_ico, UUtils.getString(R.string.ustc), 4666);
        menuLeftPopuListData.add(ustc);
        //哈尔滨
        MenuLeftPopuListWindow.MenuLeftPopuListData heb = new MenuLeftPopuListWindow.MenuLeftPopuListData(R.mipmap.mingl_ico, UUtils.getString(R.string.hit), 46667);
        menuLeftPopuListData.add(heb);

        showMenuDialog(menuLeftPopuListData, view, context);
    }

    private void showMenuDialog(ArrayList<MenuLeftPopuListWindow.MenuLeftPopuListData> arrayList, View showView, Context context) {
        MenuLeftPopuListWindow menuLeftPopuListWindow = new MenuLeftPopuListWindow(context);
        menuLeftPopuListWindow.setItemClickPopuListener(this);
        menuLeftPopuListWindow.setListData(arrayList);
        menuLeftPopuListWindow.showAsDropDown(showView, 250, -200);
    }

    @Override
    public void itemClick(int id, int index, @Nullable MenuLeftPopuListWindow mMenuLeftPopuListWindow) {
        mMenuLeftPopuListWindow.dismiss();
        switch (id) {
            //清华
            case 1:
                SwitchDialog switchDialog = switchDialogShow(UUtils.getString(R.string.警告), UUtils.getString(R.string.该操作会覆盖您的文件记录));
                switchDialog.getCancel().setOnClickListener(v -> switchDialog.dismiss());
                switchDialog.getOk().setOnClickListener(v -> {
                    switchDialog.dismiss();
                    TermuxActivity.mTerminalView.sendTextToTerminal(CodeString.INSTANCE.getQH());
                });
                break;
            //北京
            case 2:
                SwitchDialog switchDialog1 = switchDialogShow(UUtils.getString(R.string.警告), UUtils.getString(R.string.该操作会覆盖您的文件记录));
                switchDialog1.getCancel().setOnClickListener(v -> switchDialog1.dismiss());
                switchDialog1.getOk().setOnClickListener(v -> {
                    switchDialog1.dismiss();
                    TermuxActivity.mTerminalView.sendTextToTerminal(CodeString.INSTANCE.getBJ());
                });
                break;
            //官方
            case 3:
                SwitchDialog switchDialog2 = switchDialogShow(UUtils.getString(R.string.警告), UUtils.getString(R.string.该操作会覆盖您的文件记录));
                switchDialog2.getCancel().setOnClickListener(v -> switchDialog2.dismiss());
                switchDialog2.getOk().setOnClickListener(v -> {
                    switchDialog2.dismiss();
                    UUtils.writerFile("code/sources.list", new File(FileUrl.INSTANCE.getSourcesUrl()));
                    UUtils.writerFile("code/science.list", new File(FileUrl.INSTANCE.getScienceUrl()));
                    UUtils.writerFile("code/game.list", new File(FileUrl.INSTANCE.getGameUrl()));
                    TermuxActivity.mTerminalView.sendTextToTerminal(CodeString.INSTANCE.getUpDate());
                });
                break;
            //NJU
            case 496:
                SwitchDialog switchDialog15 = switchDialogShow(UUtils.getString(R.string.警告), UUtils.getString(R.string.该操作会覆盖您的文件记录));
                switchDialog15.getCancel().setOnClickListener(v -> switchDialog15.dismiss());
                switchDialog15.getOk().setOnClickListener(v -> {
                    switchDialog15.dismiss();
                    TermuxActivity.mTerminalView.sendTextToTerminal(CodeString.INSTANCE.getNJU());
                });
                break;

            //USTC
            case 4666:
                SwitchDialog ustc = switchDialogShow(UUtils.getString(R.string.警告), UUtils.getString(R.string.该操作会覆盖您的文件记录));
                ustc.getCancel().setOnClickListener(v -> ustc.dismiss());
                ustc.getOk().setOnClickListener(v -> {
                    ustc.dismiss();
                    TermuxActivity.mTerminalView.sendTextToTerminal(CodeString.INSTANCE.getUSTC());
                });
                break;
            //heb
            case 46667:
                SwitchDialog heb = switchDialogShow(UUtils.getString(R.string.警告), UUtils.getString(R.string.该操作会覆盖您的文件记录));
                heb.getCancel().setOnClickListener(v -> heb.dismiss());
                heb.getOk().setOnClickListener(v -> {
                    heb.dismiss();
                    TermuxActivity.mTerminalView.sendTextToTerminal(CodeString.INSTANCE.getHEB());
                });
                break;
        }
    }
    private SwitchDialog switchDialogShow(String title, String msg) {
        SwitchDialog switchDialog = new SwitchDialog(mContext);
        switchDialog.getTitle().setText(title);
        switchDialog.getMsg().setText(msg);
        switchDialog.getOther().setVisibility(View.GONE);
        switchDialog.getOk().setText(UUtils.getString(R.string.确定));
        switchDialog.getCancel().setText(UUtils.getString(R.string.取消));
        switchDialog.show();
        return switchDialog;
    }
}
