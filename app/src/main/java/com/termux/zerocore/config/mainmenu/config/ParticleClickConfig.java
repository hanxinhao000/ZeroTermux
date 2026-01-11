package com.termux.zerocore.config.mainmenu.config;

import static com.termux.zerocore.config.mainmenu.MainMenuConfig.CODE_BEAUTIFICATION_FUNCTION;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;

import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.zerocore.bean.ZTUserBean;
import com.termux.zerocore.ftp.utils.UserSetManage;

public class ParticleClickConfig extends BaseMenuClickConfig {
    @Override
    public int getType() {
        return CODE_BEAUTIFICATION_FUNCTION;
    }

    @Override
    public Drawable getIcon(Context context) {
        return context.getDrawable(R.mipmap.particle);
    }

    @Override
    public String getString(Context context) {
        return context.getString(R.string.zt_particle_animation);
    }

    @Override
    public void onClick(View view, Context context) {
        TermuxActivity termuxActivity = (TermuxActivity) context;
        ZTUserBean ztRainUserBean = UserSetManage.Companion.get().getZTUserBean();
        ztRainUserBean.setSnowflakeShow(false);
        termuxActivity.xue_fragment.removeAllViews();
        if (!ztRainUserBean.isRainShow()) {
            termuxActivity.firework_view.setVisibility(View.VISIBLE);
            ztRainUserBean.setRainShow(true);
            UserSetManage.Companion.get().setZTUserBean(ztRainUserBean);
        } else {
            termuxActivity.firework_view.setVisibility(View.GONE);
            ztRainUserBean.setRainShow(false);
            UserSetManage.Companion.get().setZTUserBean(ztRainUserBean);
        }
    }

    @Override
    public void initViewStatus(Context context) {
        TermuxActivity termuxActivity = (TermuxActivity) context;
        ZTUserBean ztRainUserBean = UserSetManage.Companion.get().getZTUserBean();
        termuxActivity.xue_fragment.removeAllViews();
        if (ztRainUserBean.isRainShow()) {
            termuxActivity.firework_view.setVisibility(View.VISIBLE);
        } else {
            termuxActivity.firework_view.setVisibility(View.GONE);
        }
    }
}
