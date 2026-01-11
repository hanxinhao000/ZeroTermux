package com.termux.zerocore.config.mainmenu.config;

import static com.termux.zerocore.config.mainmenu.MainMenuConfig.CODE_BEAUTIFICATION_FUNCTION;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.view.View;

import com.example.xh_lib.utils.UUtils;
import com.termux.R;

public class FloatWindowsClickConfig extends BaseMenuClickConfig {
    @Override
    public int getType() {
        return CODE_BEAUTIFICATION_FUNCTION;
    }

    @Override
    public Drawable getIcon(Context context) {
        return context.getDrawable(R.mipmap.xuanfu_window);
    }

    @Override
    public String getString(Context context) {
        return context.getString(R.string.悬浮窗口);
    }

    @Override
    public void onClick(View view, Context context) {
        try {
            Intent intent1 = new Intent();
            intent1.setAction("com.zero_float.action.ENTER");
            context.startActivity(intent1);
        } catch (Exception e) {
            e.printStackTrace();
            UUtils.showMsg(UUtils.getString(R.string.zt_install_float));
        }
    }
}
