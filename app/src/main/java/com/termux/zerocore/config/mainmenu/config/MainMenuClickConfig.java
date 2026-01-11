package com.termux.zerocore.config.mainmenu.config;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public interface MainMenuClickConfig {
    // 返回分类
    int getType();
    // 返回图标
    Drawable getIcon(Context context);
    // 返回名称
    String getString(Context context);
    // 单击
    void onClick(View view, Context context);
    // 长按
    boolean onLongClick(View view, Context context);
    // 可能要初始化当前状态
    void initViewStatus(Context context);
    //释放资源
    void release();
    //设置TextView
    void setTextView(TextView textView);
    //设置ImageView
    void setImageView(ImageView imageView);
}
