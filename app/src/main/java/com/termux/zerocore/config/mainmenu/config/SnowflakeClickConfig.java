package com.termux.zerocore.config.mainmenu.config;

import static com.termux.zerocore.config.mainmenu.MainMenuConfig.CODE_BEAUTIFICATION_FUNCTION;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.TextView;

import com.example.xh_lib.utils.UUtils;
import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.zerocore.bean.ZTUserBean;
import com.termux.zerocore.ftp.utils.UserSetManage;
import com.termux.zerocore.view.xuehua.SnowView;

public class SnowflakeClickConfig extends BaseMenuClickConfig {
    private TextView mTextView;
    @Override
    public int getType() {
        return CODE_BEAUTIFICATION_FUNCTION;
    }

    @Override
    public Drawable getIcon(Context context) {
        return context.getDrawable(R.mipmap.xuehua_ico);
    }

    @Override
    public String getString(Context context) {
        if (UserSetManage.Companion.get().getZTUserBean().isSnowflakeShow()) {
            return context.getString(R.string.雪花开);
        } else {
            return context.getString(R.string.雪花关);
        }
    }

    @Override
    public void onClick(View view, Context context) {
        TermuxActivity termuxActivity = (TermuxActivity) context;
        ZTUserBean ztUserBean = UserSetManage.Companion.get().getZTUserBean();
        ztUserBean.setRainShow(false);
        termuxActivity.firework_view.setVisibility(View.GONE);
        if (!ztUserBean.isSnowflakeShow()) {
            mTextView.setText(UUtils.getString(R.string.雪花开));
            SnowView snowView = new SnowView(termuxActivity);
            termuxActivity.xue_fragment.removeAllViews();
            termuxActivity.xue_fragment.addView(snowView);
            ztUserBean.setSnowflakeShow(true);
            UserSetManage.Companion.get().setZTUserBean(ztUserBean);
        } else {
            mTextView.setText(UUtils.getString(R.string.雪花关));
            termuxActivity.xue_fragment.removeAllViews();
            ztUserBean.setSnowflakeShow(false);
            UserSetManage.Companion.get().setZTUserBean(ztUserBean);
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
        TermuxActivity termuxActivity = (TermuxActivity) context;
        boolean snowflakeShow = UserSetManage.Companion.get().getZTUserBean().isSnowflakeShow();
        if (!snowflakeShow) {
            //xue_hua_start.setText(UUtils.getString(R.string.雪花关));
            termuxActivity.xue_fragment.removeAllViews();
        } else {
            //xue_hua_start.setText(UUtils.getString(R.string.雪花开));
            SnowView snowView = new SnowView(termuxActivity);
            termuxActivity.xue_fragment.removeAllViews();
            termuxActivity.xue_fragment.addView(snowView);
        }
    }
}
