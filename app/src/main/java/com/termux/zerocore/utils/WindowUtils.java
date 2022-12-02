package com.termux.zerocore.utils;

import android.app.Activity;
import android.view.WindowManager;

import com.gyf.immersionbar.ImmersionBar;
import com.termux.app.TermuxActivity;

public class WindowUtils {

    public static boolean isFullScreen = false;

    /**
     * 全屏显示
     */
    public static void setFullScreen(Activity activity) {
        WindowManager.LayoutParams params = activity.getWindow().getAttributes();
        params.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
        activity.getWindow().setAttributes(params);
        activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        isFullScreen = true;
        //getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN); // Activity全屏显示，且状态栏被覆盖掉
    }

    /**
     * 退出全屏
     */
    public static void exitFullScreen(Activity activity) {
        WindowManager.LayoutParams params = activity.getWindow().getAttributes();
        params.flags &= (~WindowManager.LayoutParams.FLAG_FULLSCREEN);
        activity.getWindow().setAttributes(params);
        activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        isFullScreen = false;
        //getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN); // Activity全屏显示，但是状态栏不会被覆盖掉，而是正常显示，只是Activity顶端布局会被覆盖住
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
