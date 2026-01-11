package com.termux.zerocore.config.mainmenu.config;

import static com.termux.zerocore.config.mainmenu.MainMenuConfig.CODE_ZT_FEATURES;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;

import com.termux.R;
import com.termux.zerocore.dialog.BoomZeroTermuxDialog;

public class ZeroFunctionClickConfig extends BaseMenuClickConfig {
    @Override
    public int getType() {
        return CODE_ZT_FEATURES;
    }

    @Override
    public Drawable getIcon(Context context) {
        return context.getDrawable(R.mipmap.zero_fun_ico);
    }

    @Override
    public String getString(Context context) {
        return context.getString(R.string.Zero功能);
    }

    @Override
    public void onClick(View view, Context context) {
        BoomZeroTermuxDialog boomZeroTermuxDialog = new BoomZeroTermuxDialog(context);
        boomZeroTermuxDialog.show();
        boomZeroTermuxDialog.setCancelable(true);
    }
}
