package com.termux.zerocore.config.mainmenu.config;

import static com.termux.zerocore.config.mainmenu.MainMenuConfig.CODE_BEAUTIFICATION_FUNCTION;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;

import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.zerocore.utils.WindowUtils;

public class FullScreenClickConfig extends BaseMenuClickConfig {
    @Override
    public int getType() {
        return CODE_BEAUTIFICATION_FUNCTION;
    }

    @Override
    public Drawable getIcon(Context context) {
        return context.getDrawable(R.mipmap.quanping_ico);
    }

    @Override
    public String getString(Context context) {
        return context.getString(R.string.全屏模式);
    }

    @Override
    public void onClick(View view, Context context) {
        TermuxActivity termuxActivity = (TermuxActivity) context;
        if (view.getTag() == null) {
            WindowUtils.setFullScreen(termuxActivity);
            view.setTag("fff");
            //mExtraKeysView.setVisibility(View.GONE);
            termuxActivity.setExtraKeysViewVisible(false);
        } else {
            WindowUtils.exitFullScreen(termuxActivity);
            view.setTag(null);
            //mExtraKeysView.setVisibility(View.VISIBLE);
            termuxActivity.setExtraKeysViewVisible(true);
        }
    }
}
