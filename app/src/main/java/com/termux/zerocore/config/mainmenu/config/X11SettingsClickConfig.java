package com.termux.zerocore.config.mainmenu.config;

import static com.termux.zerocore.config.mainmenu.MainMenuConfig.CODE_X11_FEATURES;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.view.View;

import com.termux.R;
import com.termux.zerocore.settings.ZeroTermuxX11Settings;

public class X11SettingsClickConfig extends BaseMenuClickConfig {
    @Override
    public int getType() {
        return CODE_X11_FEATURES;
    }

    @Override
    public Drawable getIcon(Context context) {
        return context.getDrawable(R.mipmap.settings);
    }

    @Override
    public String getString(Context context) {
        return context.getString(R.string.x11_features_settings);
    }

    @Override
    public void onClick(View view, Context context) {
        context.startActivity(new Intent(context, ZeroTermuxX11Settings.class));
    }
}
