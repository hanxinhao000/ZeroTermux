package com.example.xh_lib.zw;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;


public class SPUtils {

    private static final String FILE_NAME = "biometric_demo";

    public static final String KEY_BIOMETRIC_SWITCH_ENABLE = "key_biometric_switch_enable";

    private static SharedPreferences sPreferences;

    private SPUtils() {
        // nothing
    }

    public static void initialize(Context context) {
        sPreferences = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);
    }

    public static void put(Context context, String key, @NonNull Object object) {
        SharedPreferences.Editor editor = getSharedPreferences(context).edit();
        if (object instanceof String) {
            editor.putString(key, (String) object);
        } else if (object instanceof Integer) {
            editor.putInt(key, (Integer) object);
        } else if (object instanceof Boolean) {
            editor.putBoolean(key, (Boolean) object);
        } else if (object instanceof Float) {
            editor.putFloat(key, (Float) object);
        } else if (object instanceof Long) {
            editor.putLong(key, (Long) object);
        } else {
            editor.putString(key, object.toString());
        }
        editor.apply();
    }

    public static String getString(Context context, String key, String defaultValue) {
        return getSharedPreferences(context).getString(key, defaultValue);
    }

    public static boolean getBoolean(Context context, String key, boolean defaultValue) {
        return getSharedPreferences(context).getBoolean(key, defaultValue);
    }

    public static int getInt(Context context, String key, int defaultValue) {
        return getSharedPreferences(context).getInt(key, defaultValue);
    }

    public static long getLong(Context context, String key, long defaultValue) {
        return getSharedPreferences(context).getLong(key, defaultValue);
    }

    public static void remove(Context context, String key) {
        getSharedPreferences(context).edit().remove(key).apply();
    }

    public static boolean contains(Context context, String key) {
        return getSharedPreferences(context).contains(key);
    }

    private static SharedPreferences getSharedPreferences(Context context) {
        return sPreferences == null ? context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE) : sPreferences;
    }
}