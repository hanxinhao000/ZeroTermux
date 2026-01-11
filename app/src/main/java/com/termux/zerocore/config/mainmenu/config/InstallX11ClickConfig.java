package com.termux.zerocore.config.mainmenu.config;

import static com.termux.zerocore.config.mainmenu.MainMenuConfig.CODE_X11_FEATURES;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.view.View;

import com.termux.R;
import com.termux.zerocore.settings.ZTInstallActivity;

public class InstallX11ClickConfig extends BaseMenuClickConfig {
    @Override
    public int getType() {
        return CODE_X11_FEATURES;
    }

    @Override
    public Drawable getIcon(Context context) {
        return context.getDrawable(R.drawable.ic_x11_icon);
    }

    @Override
    public String getString(Context context) {
        return context.getString(R.string.x11_install_apk);
    }

    @Override
    public void onClick(View view, Context context) {
        context.startActivity(new Intent(context, ZTInstallActivity.class));
    }
}
