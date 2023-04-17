package com.termux.zerocore.ftp.new_ftp;

import androidx.appcompat.app.AppCompatDelegate;

import com.termux.zerocore.ftp.new_ftp.utils.StorageUtil;


public class Constants {
    public static class SQLConsts {
        public static final String SQL_USERS_FILENAME = "ftp_accounts.db";
        public static final int SQL_VERSION = 1;
        public static final String TABLE_NAME = "ftp_account_table";
        public static final String COLUMN_ID = "_id";
        public static final String COLUMN_ACCOUNT_NAME = "name";
        public static final String COLUMN_PASSWORD = "password";
        public static final String COLUMN_PATH = "path";
        public static final String COLUMN_WRITABLE = "writable";
    }

    public static class FTPConsts {
        public static final String NAME_ANONYMOUS = "anonymous";
    }

    public static class PreferenceConsts {
        public static final String FILE_NAME = "settings";
        /**
         * this stands for a boolean value
         */
        public static final String ANONYMOUS_MODE = "anonymous_mode";
        public static final boolean ANONYMOUS_MODE_DEFAULT = true;
        /**
         * this stands for a string value
         */
        public static final String ANONYMOUS_MODE_PATH = "anonymous_mode_path";
        public static final String ANONYMOUS_MODE_PATH_DEFAULT = StorageUtil.getMainStoragePath();
        /**
         * this stands for a boolean value
         */
        public static final String ANONYMOUS_MODE_WRITABLE = "anonymous_mode_writable";
        public static final boolean ANONYMOUS_MODE_WRITABLE_DEFAULT = true;

        /**
         * this stands for a boolean value
         */
        public static final String WAKE_LOCK = "wake_lock";
        public static final boolean WAKE_LOCK_DEFAULT = false;
        /**
         * this stands for a int value
         */
        public static final String PORT_NUMBER = "port_number";
        public static int PORT_NUMBER_DEFAULT = 2121;
        /**
         * this stands for a string value
         */
        public static final String CHARSET_TYPE = "charset_type";
        public static final String CHARSET_TYPE_DEFAULT = "UTF-8";
        public static final String MAX_ANONYMOUS_NUM = "max_anonymous_logins";
        public static final String MAX_LOGIN_NUM = "max_logins";
        /**
         * int value
         */
        public static final String NIGHT_MODE = "night_mode";
        public static final int NIGHT_MODE_DEFAULT = AppCompatDelegate.MODE_NIGHT_NO;
        /**
         * int value
         */
        public static final String LANGUAGE_SETTING = "language_setting";
        public static final int LANGUAGE_FOLLOW_SYSTEM = 0;
        public static final int LANGUAGE_SIMPLIFIED_CHINESE = 1;
        public static final int LANGUAGE_ENGLISH = 2;
        public static final int LANGUAGE_SETTING_DEFAULT = LANGUAGE_FOLLOW_SYSTEM;
        /**
         * int value
         */
        public static final String AUTO_STOP = "auto_stop";
        public static final int AUTO_STOP_NONE = -1;
        public static final int AUTO_STOP_WIFI_DISCONNECTED = 0;
        public static final int AUTO_STOP_AP_DISCONNECTED = 1;
        public static final int AUTO_STOP_TIME_COUNT = 2;
        public static final int AUTO_STOP_DEFAULT = AUTO_STOP_NONE;
        /**
         * int value
         */
        public static final String AUTO_STOP_VALUE = "auto_stop_value";
        public static final int AUTO_STOP_VALUE_DEFAULT = 600;
        /**
         * boolean value
         */
        public static final String START_AFTER_BOOT = "start_after_boot";
        public static final boolean START_AFTER_BOOT_DEFAULT = false;
    }

    public static class Charset {
        public static final String CHAR_UTF = "UTF-8";
        public static final String CHAR_GBK = "GBK";
    }

}
