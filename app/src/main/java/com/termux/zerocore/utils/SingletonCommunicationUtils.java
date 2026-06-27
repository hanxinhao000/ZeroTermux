package com.termux.zerocore.utils;

import com.example.xh_lib.utils.LogUtils;
import com.example.xh_lib.utils.UUtils;
import com.termux.R;

public class SingletonCommunicationUtils {
    private static final String TAG = SingletonCommunicationUtils.class.getSimpleName();
    private static SingletonCommunicationUtils singletonCommunicationUtils;
    public static boolean isSingletonCommunicationListenerNull = true;
    private static final SingletonCommunicationListener EMPTY_SINGLETON_COMMUNICATION_LISTENER = new SingletonCommunicationListener() {
        @Override
        public void sendTextToTerminal(String command) {
            notifyTerminalUnavailable("sendTextToTerminal", command, R.string.zt_terminal_unavailable_command_not_sent);
        }

        @Override
        public void sendTextToTerminalAlt(String command, boolean isAlt) {
            notifyTerminalUnavailable("sendTextToTerminalAlt", command + ", isAlt: " + isAlt, R.string.zt_terminal_unavailable_command_not_sent);
        }

        @Override
        public void sendTextToTerminalCtrl(String command, boolean isCtrl) {
            notifyTerminalUnavailable("sendTextToTerminalCtrl", command + ", isCtrl: " + isCtrl, R.string.zt_terminal_unavailable_command_not_sent);
        }

        @Override
        public void onTerminalExtraKeyButtonClick(String key) {
            notifyTerminalUnavailable("onTerminalExtraKeyButtonClick", key, R.string.zt_terminal_unavailable_key_not_sent);
        }

        @Override
        public String getTextToTerminal() {
            notifyTerminalUnavailable("getTextToTerminal", null, R.string.zt_terminal_unavailable_text_not_read);
            return "";
        }

        @Override
        public String getVisibleTerminalText() {
            notifyTerminalUnavailable("getVisibleTerminalText", null, R.string.zt_terminal_unavailable_text_not_read);
            return "";
        }
    };
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
        isSingletonCommunicationListenerNull = singletonCommunicationListener == null;
    }

    public boolean hasTerminalListener() {
        return mSingletonCommunicationListener != null;
    }

    public SingletonCommunicationListener getmSingletonCommunicationListener() {
        if (mSingletonCommunicationListener == null) {
            return EMPTY_SINGLETON_COMMUNICATION_LISTENER;
        }
        return mSingletonCommunicationListener;
    }

    private static void notifyTerminalUnavailable(String action, String content, int toastMessageResId) {
        LogUtils.e(TAG, "terminal communication listener is null, action: " + action + ", content: " + content);
        UUtils.showMsg(UUtils.getString(toastMessageResId));
    }

    public static interface SingletonCommunicationListener {
        void sendTextToTerminal(String command);
        void sendTextToTerminalAlt(String command, boolean isAlt);
        void sendTextToTerminalCtrl(String command, boolean isCtrl);
        void onTerminalExtraKeyButtonClick(String key);
        String getTextToTerminal();
        String getVisibleTerminalText();
    }
}
