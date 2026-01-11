package com.termux.zerocore.config.mainmenu.config;

import static com.termux.zerocore.config.mainmenu.MainMenuConfig.CODE_ONLINE_FEATURES;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;

import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.zerocore.dialog.OnLineShDialog;

import org.jetbrains.annotations.NotNull;

public class OnLineCommandClickConfig extends BaseMenuClickConfig {
    @Override
    public int getType() {
        return CODE_ONLINE_FEATURES;
    }

    @Override
    public Drawable getIcon(Context context) {
        return context.getDrawable(R.mipmap.online_sh);
    }

    @Override
    public String getString(Context context) {
        return context.getString(R.string.在线脚本);
    }

    @Override
    public void onClick(View view, Context context) {
        OnLineShDialog mOnLineShDialog = new OnLineShDialog(context);
        mOnLineShDialog.setOnItemClickListener(msg -> {
            TermuxActivity.mTerminalView.sendTextToTerminal(msg + "\n");
            mOnLineShDialog.dismiss();
        });
        mOnLineShDialog.show();
        mOnLineShDialog.setCancelable(true);
    }
}
