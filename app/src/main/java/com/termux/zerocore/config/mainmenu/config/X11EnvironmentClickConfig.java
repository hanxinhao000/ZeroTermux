package com.termux.zerocore.config.mainmenu.config;

import static com.termux.zerocore.config.mainmenu.MainMenuConfig.CODE_X11_FEATURES;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;

import com.termux.R;
import com.termux.app.TermuxActivity;

public class X11EnvironmentClickConfig extends BaseMenuClickConfig {
    @Override
    public int getType() {
        return CODE_X11_FEATURES;
    }

    @Override
    public Drawable getIcon(Context context) {
        return context.getDrawable(R.mipmap.copy_command);
    }

    @Override
    public String getString(Context context) {
        return context.getString(R.string.x11_environment);
    }

    @Override
    public void onClick(View view, Context context) {
        TermuxActivity termuxActivity = (TermuxActivity) context;
        // 复制环境
        // am start -a android.intent.action.zt.termux.x11
        TermuxActivity.mTerminalView.sendTextToTerminal("pkg install x11-repo " +
            "&& pkg install termux-x11-nightly " +
            "&& termux-x11 \n");
        termuxActivity.getDrawer().smoothClose();
    }
}
