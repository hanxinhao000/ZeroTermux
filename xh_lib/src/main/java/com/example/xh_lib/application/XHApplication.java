package com.example.xh_lib.application;

import android.app.Application;
import android.content.Context;
import android.os.Handler;

import com.example.xh_lib.utils.UUtils;

/**
 * @author ZEL
 * @create By ZEL on 2020/7/16 16:06
 **/
public class XHApplication extends Application {


    public Context mContext;

    public Handler mHandler;

    @Override
    public void onCreate() {
        super.onCreate();

        mContext = this;

        mHandler = new Handler();

        UUtils.initUUtils(mContext,mHandler);


    }
}
