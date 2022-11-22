package com.termux.zerocore.zero.engine;

import android.content.Context;
import android.os.Handler;
import android.view.View;

import com.example.xh_lib.utils.LogUtils;
import com.example.xh_lib.utils.UUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;

public class ZeroCoreManage {
    public static String TAG = "ZeroCoreManage";
    public static String ZERO_ENGINE_PACKAGE = "com.xinhao.zerocoremanage";
    public static String ZERO_ENGINE_PACKAGE_CLASS = "com.xinhao.zerocoremanage.zeroeg.ZeroEngineManage";
    public static final int INSTALLING = 10002;
    public static final int INSTALL_COMPLETE = 10003;
    public static Context mContext;
    public static Class<?> ZERO_ENGINE_CLASS;
    public static void initEngineManage() {
        try {
            mContext = UUtils.getContext().createPackageContext(ZERO_ENGINE_PACKAGE,
                    Context.CONTEXT_INCLUDE_CODE | Context.CONTEXT_IGNORE_SECURITY);
            ZERO_ENGINE_CLASS = Class.forName(ZERO_ENGINE_PACKAGE_CLASS, true, mContext.getClassLoader());
            ZeroCoreManage.setContext();
            ZeroCoreManage.setEngineContext();
        } catch (Exception e) {
            e.printStackTrace();
            LogUtils.e(TAG, "initEngineManage error:" + e);
        }
    }

    public static ArrayList<String> getEnvironment() {
        try {
            Object object = ZERO_ENGINE_CLASS.newInstance();
            Method mMethod = object.getClass().getMethod("getEnvironment");
            return (ArrayList<String>) mMethod.invoke(object);
        } catch (Exception e) {
            e.printStackTrace();
            LogUtils.e(TAG, "getEnvironment error:" + e);
        }
        return null;
    }

    public static ArrayList<String> getProcessArgs() {
        try {
            Object object = ZERO_ENGINE_CLASS.newInstance();
            Method mMethod = object.getClass().getMethod("getProcessArgs");
            return (ArrayList<String>) mMethod.invoke(object);
        } catch (Exception e) {
            e.printStackTrace();
            LogUtils.e(TAG, "getProcessArgs error:" + e);
        }
        return null;
    }

    public static String getDataDirectory() {
        try {
            Object object = ZERO_ENGINE_CLASS.newInstance();
            Method mMethod = object.getClass().getMethod("getDataDirectory");
            return (String) mMethod.invoke(object);
        } catch (Exception e) {
            e.printStackTrace();
            LogUtils.e(TAG, "getDataDirectory error:" + e);
        }
        return null;
    }

    public static void setContext() {
        try {
            Object object = ZERO_ENGINE_CLASS.newInstance();
            Method mMethod = object.getClass().getMethod("setContext", Context.class);
            mMethod.invoke(object, UUtils.getContext());
        } catch (Exception e) {
            e.printStackTrace();
            LogUtils.e(TAG, "setContext error:" + e);
        }
    }

    public static void setEngineContext() {
        try {
            Object object = ZERO_ENGINE_CLASS.newInstance();
            Method mMethod = object.getClass().getMethod("setEngineContext", Context.class);
            mMethod.invoke(object, mContext);
        } catch (Exception e) {
            e.printStackTrace();
            LogUtils.e(TAG, "setEngineContext error:" + e);
        }
    }

    public static void install(Handler mHandler) {
        try {
            Object object = ZERO_ENGINE_CLASS.newInstance();
            Method mMethod = object.getClass().getMethod("install", Handler.class);
            mMethod.invoke(object, mHandler);
        } catch (Exception e) {
            e.printStackTrace();
            LogUtils.e(TAG, "install error:" + e);
        }
    }

    public static String getVersionName() {
        try {
            Object object = ZERO_ENGINE_CLASS.newInstance();
            Method mMethod = object.getClass().getMethod("getVersionName", Context.class);
            return (String) mMethod.invoke(object, mContext);
        } catch (Exception e) {
            e.printStackTrace();
            LogUtils.e(TAG, "getVersionName error:" + e);
        }
        return "";
    }

    public static void setRunHandler(Handler mHandler) {
        try {
            Object object = ZERO_ENGINE_CLASS.newInstance();
            Method mMethod = object.getClass().getMethod("setRunHandler", Handler.class);
            mMethod.invoke(object, mHandler);
        } catch (Exception e) {
            e.printStackTrace();
            LogUtils.e(TAG, "setRunHandler error:" + e);
        }
    }

    public static void setKeyHandler(Handler mHandler) {
        try {
            Object object = ZERO_ENGINE_CLASS.newInstance();
            Method mMethod = object.getClass().getMethod("setKeyHandler", Handler.class);
            mMethod.invoke(object, mHandler);
        } catch (Exception e) {
            e.printStackTrace();
            LogUtils.e(TAG, "setKeyHandler error:" + e);
        }
    }


    public static void installFileBrowser(Handler mInstallHandler) {
        try {
            Object object = ZERO_ENGINE_CLASS.newInstance();
            Method mMethod = object.getClass().getMethod("installFileBrowser", Context.class, Context.class, Handler.class);
            mMethod.invoke(object, UUtils.getContext(), mContext, mInstallHandler);
        } catch (Exception e) {
            e.printStackTrace();
            LogUtils.e(TAG, "installFileBrowser error:" + e);
        }
    }


    public static void initKeyView() {
        try {
            Object object = ZERO_ENGINE_CLASS.newInstance();
            Method mMethod = object.getClass().getMethod("initKeyView");
            mMethod.invoke(object);
        } catch (Exception e) {
            e.printStackTrace();
            LogUtils.e(TAG, "initKeyView error:" + e);
        }
    }

    public static View getKeyView() {
        try {
            Object object = ZERO_ENGINE_CLASS.newInstance();
            Method mMethod = object.getClass().getMethod("getKeyView");
            return (View) mMethod.invoke(object);
        } catch (Exception e) {
            e.printStackTrace();
            LogUtils.e(TAG, "getKeyView error:" + e);
        }
        return null;
    }

    private static Object setMethod(Method[] methods, String method,  Object o, Object... var2) {
        if (methods == null || methods.length == 0) {
            LogUtils.e(TAG, "setMethod methods is empty");
            return null;
        }

        for (int i = 0; i < methods.length; i++) {
            LogUtils.d(TAG, "setMethod methods:" + methods[i].getName());
            if(methods[i].getName().equals(method)) {
                methods[i].setAccessible(true);
                try {
                   return methods[i].invoke(o, var2);
                } catch (Exception e) {
                    e.printStackTrace();
                    LogUtils.e(TAG, "setMethod error:" + e);
                }
            }
        }

        return null;
    }

}
