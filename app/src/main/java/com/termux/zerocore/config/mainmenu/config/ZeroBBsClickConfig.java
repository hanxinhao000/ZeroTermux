package com.termux.zerocore.config.mainmenu.config;

import static com.termux.zerocore.config.mainmenu.MainMenuConfig.CODE_ONLINE_FEATURES;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.view.View;

import com.termux.R;
import com.termux.zerocore.activity.WebViewActivity;
import com.termux.zerocore.http.HTTPIP;

public class ZeroBBsClickConfig extends BaseMenuClickConfig {
    @Override
    public int getType() {
        return CODE_ONLINE_FEATURES;
    }

    @Override
    public Drawable getIcon(Context context) {
        return context.getDrawable(R.mipmap.bbs_zero);
    }

    @Override
    public String getString(Context context) {
        return context.getString(R.string.Zero论坛);
    }

    @Override
    public void onClick(View view, Context context) {
        Intent intent2 = new Intent(context, WebViewActivity.class);
        intent2.putExtra("title", "ZeroTermux 论坛");
        intent2.putExtra("content", HTTPIP.ZERO_BBS);
        context.startActivity(intent2);
    }
}
