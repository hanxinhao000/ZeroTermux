package com.termux.zerocore.config.mainmenu.config;

import static com.termux.zerocore.config.mainmenu.MainMenuConfig.CODE_ZT_FEATURES;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.TextView;

import com.example.xh_lib.utils.LogUtils;
import com.termux.R;
import com.termux.zerocore.utils.StartRunCommandUtils;

public class BootCommandClickConfig extends BaseMenuClickConfig {
    private static final String TAG = BootCommandClickConfig.class.getSimpleName();
    private TextView mTextView;
    @Override
    public int getType() {
        return CODE_ZT_FEATURES;
    }

    @Override
    public Drawable getIcon(Context context) {
        return context.getDrawable(R.mipmap.run_ico);
    }

    @Override
    public String getString(Context context) {
        if (StartRunCommandUtils.INSTANCE.isRun()) {
            return context.getString(R.string.开机启动开);
        } else {
            return context.getString(R.string.开机启动);
        }
    }

    @Override
    public void onClick(View view, Context context) {
        LogUtils.e(TAG, "onClick.");
        if (StartRunCommandUtils.INSTANCE.isRun()) {
            StartRunCommandUtils.INSTANCE.endRun();
            mTextView.setText(context.getString(R.string.开机启动));
        } else {
            mTextView.setText(context.getString(R.string.开机启动开));
            StartRunCommandUtils.INSTANCE.startRun();
        }
    }

    @Override
    public void setTextView(TextView textView) {
        super.setTextView(textView);
        mTextView = textView;
    }

    @Override
    public void release() {
        super.release();
        mTextView = null;
    }

    @Override
    public void initViewStatus(Context context) {
        super.initViewStatus(context);
        if (StartRunCommandUtils.INSTANCE.isRun()) {
            mTextView.setText(context.getString(R.string.开机启动开));
        } else {
            mTextView.setText(context.getString(R.string.开机启动));
        }
    }
}
