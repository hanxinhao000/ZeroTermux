package com.termux.zerocore.config.mainmenu.config;

import static com.termux.zerocore.config.mainmenu.MainMenuConfig.CODE_ZT_FEATURES;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;

import com.termux.R;

public class GitHubClickConfig extends BaseMenuClickConfig {
    @Override
    public int getType() {
        return CODE_ZT_FEATURES;
    }

    @Override
    public Drawable getIcon(Context context) {
        return context.getDrawable(R.mipmap.github);
    }

    @Override
    public String getString(Context context) {
        return context.getString(R.string.开源地址);
    }

    @Override
    public void onClick(View view, Context context) {
        Intent intent = new Intent();
        intent.setData(Uri.parse("https://github.com/hanxinhao000/ZeroTermux"));//Url 就是你要打开的网址
        intent.setAction(Intent.ACTION_VIEW);
        context.startActivity(intent); //启动浏览器
    }

    @Override
    public void setTextView(TextView textView) {
        textView.setGravity(Gravity.CENTER_HORIZONTAL);
    }
}
