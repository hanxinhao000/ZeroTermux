package com.termux.zerocore.config.mainmenu.config;

import static com.termux.zerocore.config.mainmenu.MainMenuConfig.CODE_COMMON_FUNCTIONS;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.view.View;

import com.termux.R;
import com.termux.zerocore.settings.TimerActivity;

// 定时任务
public class ScheduledTaskClickConfig extends BaseMenuClickConfig {
    @Override
    public int getType() {
        return CODE_COMMON_FUNCTIONS;
    }

    @Override
    public Drawable getIcon(Context context) {
        return context.getDrawable(R.mipmap.timer);
    }

    @Override
    public String getString(Context context) {
        return context.getString(R.string.zt_timer);
    }

    @Override
    public void onClick(View view, Context context) {
        context.startActivity(new Intent(context, TimerActivity.class));
    }
}
