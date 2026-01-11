package com.termux.zerocore.config.mainmenu.config;

import static com.termux.zerocore.config.mainmenu.MainMenuConfig.CODE_ZT_FEATURES;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;

import com.termux.R;
import com.termux.zerocore.dialog.BoomCommandDialog;

public class CommandDefinitionCLickConfig extends BaseMenuClickConfig {
    @Override
    public int getType() {
        return CODE_ZT_FEATURES;
    }

    @Override
    public Drawable getIcon(Context context) {
        return context.getDrawable(R.mipmap.zidingyi_cmd);
    }

    @Override
    public String getString(Context context) {
        return context.getString(R.string.命令定义);
    }

    @Override
    public void onClick(View view, Context context) {
        BoomCommandDialog boomCommandDialog = new BoomCommandDialog(context);
        boomCommandDialog.show();
        boomCommandDialog.setCancelable(true);
    }
}
