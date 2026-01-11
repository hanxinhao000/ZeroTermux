package com.termux.zerocore.config.mainmenu.config;

import static com.termux.zerocore.config.mainmenu.MainMenuConfig.CODE_COMMON_FUNCTIONS;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;

import com.example.xh_lib.utils.UUtils;
import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.zerocore.code.CodeString;
import com.termux.zerocore.dialog.LoadingDialog;
import com.termux.zerocore.url.FileUrl;

import java.io.File;

public class ReleaseLinuxVersionClickConfig extends BaseMenuClickConfig {
    @Override
    public int getType() {
        return CODE_COMMON_FUNCTIONS;
    }

    @Override
    public Drawable getIcon(Context context) {
        return context.getDrawable(R.mipmap.linux_ico);
    }

    @Override
    public String getString(Context context) {
        return context.getString(R.string.发行版本);
    }

    @Override
    public void onClick(View view, Context context) {
        LoadingDialog loadingDialog = new LoadingDialog(context);
        loadingDialog.show();
        loadingDialog.setCancelable(false);
        UUtils.runOnThread(new Runnable() {
            @Override
            public void run() {
                UUtils.writerFile("linux/termux_linux_toolx.zip", new File(FileUrl.INSTANCE.getMainHomeUrl(), "/termux_linux_toolx.zip"));
                UUtils.runOnThread(new Runnable() {
                    @Override
                    public void run() {
                        loadingDialog.dismiss();
                        TermuxActivity.mTerminalView.sendTextToTerminal(CodeString.INSTANCE.getRunLinuxSh());
                    }
                });
            }
        });
    }
}
