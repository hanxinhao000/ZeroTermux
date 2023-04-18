package com.termux.zerocore.utils;

import android.app.Activity;
import android.os.Build;
import android.view.View;
import android.view.WindowManager;

import com.gyf.immersionbar.BarHide;
import com.gyf.immersionbar.ImmersionBar;
import com.termux.app.TermuxActivity;

public class WindowUtils {

    public static boolean isFullScreen = false;

    /**
     * 全屏显示
     */
    public static void setFullScreen(Activity activity) {
        ImmersionBar.with(activity)
            .transparentStatusBar()  //透明状态栏，不写默认透明色
            .transparentNavigationBar()  //透明导航栏，不写默认黑色(设置此方法，fullScreen()方法自动为true)
            .fullScreen(true)      //有导航栏的情况下，activity全屏显示，也就是activity最下面被导航栏覆盖，不写默认非全屏
            .hideBar(BarHide.FLAG_HIDE_BAR)
            .transparentBar().init();
        isFullScreen = true;
    }

    /**
     * 退出全屏
     */
    public static void exitFullScreen(Activity activity) {
        ImmersionBar.with(activity)
            .transparentStatusBar()  //透明状态栏，不写默认透明色
            .transparentNavigationBar()  //透明导航栏，不写默认黑色(设置此方法，fullScreen()方法自动为true)
            .fullScreen(false)      //有导航栏的情况下，activity全屏显示，也就是activity最下面被导航栏覆盖，不写默认非全屏
            .hideBar(BarHide.FLAG_SHOW_BAR)
            .transparentBar().init();
        isFullScreen = false;
    }

    /**
     *
     * 设置状态栏
     */
    public static void setImmersionBar(Activity mActivity, float status2navigationBarAlpha) {
        ImmersionBar.with(mActivity)
            .transparentStatusBar()  //透明状态栏，不写默认透明色
            .transparentNavigationBar()  //透明导航栏，不写默认黑色(设置此方法，fullScreen()方法自动为true)
            .statusBarAlpha(status2navigationBarAlpha)  //状态栏透明度，不写默认0.0f
            .navigationBarAlpha(status2navigationBarAlpha)  //导航栏透明度，不写默认0.0F
            .transparentBar().init();
    }

    public static void setStatus2navigationBarAlpha(Activity mActivity, float status2navigationBarAlpha) {
        ImmersionBar.with(mActivity)
            .statusBarAlpha(status2navigationBarAlpha)  //状态栏透明度，不写默认0.0f
            .navigationBarAlpha(status2navigationBarAlpha)  //导航栏透明度，不写默认0.0F
            .transparentBar().init();
    }
}
