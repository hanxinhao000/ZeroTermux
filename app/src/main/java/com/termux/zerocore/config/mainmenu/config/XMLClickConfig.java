package com.termux.zerocore.config.mainmenu.config;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.View;

import com.termux.R;

public class XMLClickConfig extends BaseMenuClickConfig {
    private String mName;
    private Drawable mDrawable;
    private ConfigClickListener mConfigClickListener;
    @Override
    public Drawable getIcon(Context context) {
        if (mDrawable == null) {
            return context.getDrawable(R.mipmap.custom);
        }
        return mDrawable;
    }

    @Override
    public String getString(Context context) {
        if (TextUtils.isEmpty(mName)) {
            return "";
        }
        return mName;
    }

    @Override
    public void onClick(View view, Context context) {
        if (mConfigClickListener != null) {
            mConfigClickListener.onClick(view, context);
        }
    }

    public void setConfigClickListener(ConfigClickListener mConfigClickListener) {
        this.mConfigClickListener = mConfigClickListener;
    }

    public void setName(String mName) {
        this.mName = mName;
    }

    public void setDrawable(Drawable mDrawable) {
        this.mDrawable = mDrawable;
    }

    public static interface ConfigClickListener {
        void onClick(View view, Context context);
    }
}
