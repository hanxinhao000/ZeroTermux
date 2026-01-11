package com.termux.zerocore.config.mainmenu.config;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.view.View;

import com.termux.R;
import com.termux.zerocore.activity.SwitchActivity;
import com.termux.zerocore.config.mainmenu.MainMenuConfig;

// 容器切换
public class ContainerSwitchClickConfig extends BaseMenuClickConfig {
    @Override
    public int getType() {
        return MainMenuConfig.CODE_COMMON_FUNCTIONS;
    }

    @Override
    public Drawable getIcon(Context context) {
        return context.getDrawable(R.mipmap.rongqi_ico);
    }

    @Override
    public String getString(Context context) {
        return context.getString(R.string.容器切换);
    }

    @Override
    public void onClick(View view, Context context) {
        context.startActivity(new Intent(context, SwitchActivity.class));
    }
}
