package com.termux.zerocore.config.mainmenu.config;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.xh_lib.utils.LogUtils;
import com.example.xh_lib.utils.UUtils;
import com.termux.R;
import com.termux.zerocore.dialog.SwitchDialog;
import com.termux.zerocore.popuwindow.MenuLeftPopuListWindow;

import java.util.ArrayList;

public abstract class BaseMenuClickConfig implements MainMenuClickConfig {
    private static final String TAG = BaseMenuClickConfig.class.getSimpleName();
    protected Context mContext;
    @Override
    public boolean onLongClick(View view, Context context) {
        return false;
    }

    @Override
    public void initViewStatus(Context context) {
        mContext = context;
    }

    @Override
    public void release() {
        LogUtils.e(TAG, "release.");
        mContext = null;
    }

    @Override
    public void setImageView(ImageView imageView) {

    }

    @Override
    public void setTextView(TextView textView) {

    }

    public SwitchDialog switchDialogShow(String title, String msg, Context context) {
        SwitchDialog switchDialog = new SwitchDialog(context);
        switchDialog.getTitle().setText(title);
        switchDialog.getMsg().setText(msg);
        switchDialog.getOther().setVisibility(View.GONE);
        switchDialog.getOk().setText(UUtils.getString(R.string.确定));
        switchDialog.getCancel().setText(UUtils.getString(R.string.取消));
        switchDialog.show();
        return switchDialog;
    }

}
