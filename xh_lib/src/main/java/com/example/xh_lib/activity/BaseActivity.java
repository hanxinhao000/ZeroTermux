package com.example.xh_lib.activity;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.DisplayMetrics;


import com.example.xh_lib.statusBar.StatusBarCompat;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;


public abstract class BaseActivity extends AppCompatActivity {



    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //设置状态栏颜色
//        StatusBarCompat.setStatusBarColor(this, ContextCompat.getColor(this, R.color.color_ffffff));
        //字体颜色
        //黑色
        StatusBarCompat.changeToLightStatusBar(this);
        //白色
//        StatusBarCompat.cancelLightStatusBar(this);
        //是否显示状态栏
//        StatusBarCompat.translucentStatusBar(this, false);
        // setContentView(getLayoutId());



        ActivityUtils.addDestoryActivityToMap(this, this.getClass().getSimpleName());

    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(attachBaseContext1(newBase));
    }

    public static Context attachBaseContext1(Context context) {
        setConfiguration(context);
        return context;
    }

    public static void setConfiguration(Context context) {
        Resources resources = context.getResources();
        Configuration config = resources.getConfiguration();
        DisplayMetrics dm = resources.getDisplayMetrics();

//        switch (SpUtil.getLanguage(context)) {
//            case 0:
//                config.locale = Locale.SIMPLIFIED_CHINESE;
//                break;
//            case 1:
//                config.locale = Locale.TRADITIONAL_CHINESE;
//                break;
//            case 2:
//                config.locale = Locale.ENGLISH;
//                break;
//        }
//        resources.updateConfiguration(config, dm);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ActivityUtils.destoryActivity(this.getClass().getSimpleName());
    }


    /**
     * 不携带数据的页面跳转
     *
     * @param clz
     */
    public void startActivity(Class<?> clz) {
        Intent intent = new Intent();
        intent.setClass(this, clz);
        startActivity(intent);
    }

    /**
     * 携带数据的页面跳转
     *
     * @param clz
     * @param bundle
     */
    public void startActivity(Class<?> clz, Bundle bundle) {
        Intent intent = new Intent();
        intent.setClass(this, clz);
        if (bundle != null) {
            intent.putExtras(bundle);
        }
        startActivity(intent);
    }

    /**
     * 含有Bundle通过Class打开编辑界面
     *
     * @param cls
     * @param bundle
     * @param requestCode
     */
    public void startActivityForResult(Class<?> cls, Bundle bundle,
                                       int requestCode) {
        Intent intent = new Intent();
        intent.setClass(this, cls);
        if (bundle != null) {
            intent.putExtras(bundle);
        }
        startActivityForResult(intent, requestCode);
    }
}
