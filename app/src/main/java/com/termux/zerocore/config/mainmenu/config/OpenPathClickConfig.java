package com.termux.zerocore.config.mainmenu.config;

import static com.termux.zerocore.config.mainmenu.MainMenuConfig.CODE_ZT_FEATURES;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.view.View;

import com.example.xh_lib.utils.UUtils;
import com.termux.R;

public class OpenPathClickConfig extends BaseMenuClickConfig {
    @Override
    public int getType() {
        return CODE_ZT_FEATURES;
    }

    @Override
    public Drawable getIcon(Context context) {
        return context.getDrawable(R.mipmap.file_launcher_icon);
    }

    @Override
    public String getString(Context context) {
        return context.getString(R.string.打开目录);
    }

    @Override
    public void onClick(View view, Context context) {
        try {
            Intent intent = new Intent();
            intent.setAction("com.utermux.files.action");
            context.startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
            UUtils.showMsg(UUtils.getString(R.string.zt_install_file));
        }
    }
}
