package com.termux.zerocore.utils;

import com.example.xh_lib.utils.LogUtils;

public class SingletonCommunicationUtils {
    private static final String TAG = SingletonCommunicationUtils.class.getSimpleName();
    private static SingletonCommunicationUtils singletonCommunicationUtils;
    public static boolean isSingletonCommunicationListenerNull = true;
    private SingletonCommunicationListener mSingletonCommunicationListener;
    public static SingletonCommunicationUtils getInstance() {
        if (singletonCommunicationUtils == null) {
            synchronized (SingletonCommunicationUtils.class) {
                if (singletonCommunicationUtils == null) {
                    singletonCommunicationUtils = new SingletonCommunicationUtils();
                }
                return singletonCommunicationUtils;
            }
        } else {
            return singletonCommunicationUtils;
        }
    }
    public void setSingletonCommunicationListener(SingletonCommunicationListener singletonCommunicationListener) {
        LogUtils.e(TAG, "setSingletonCommunicationListener: " + singletonCommunicationListener);
        this.mSingletonCommunicationListener = singletonCommunicationListener;
    }

    public SingletonCommunicationListener getmSingletonCommunicationListener() {
        return mSingletonCommunicationListener;
    }

    public static interface SingletonCommunicationListener {
        void sendTextToTerminal(String command);
        void sendTextToTerminalAlt(String command, boolean isAlt);
        void sendTextToTerminalCtrl(String command, boolean isCtrl);
        void onTerminalExtraKeyButtonClick(String key);
        String getTextToTerminal();
    }
}
