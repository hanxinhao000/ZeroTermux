package com.termux.view.zerotermux;

import android.content.Context;
import android.content.SharedPreferences;
public class SaveData {
    public final static String TOOL = "toolbox_state";
    public static void saveData(String key, String values, Context mContext) {
        SharedPreferences xinhao = mContext.getSharedPreferences("xinhao", Context.MODE_PRIVATE);
        xinhao.edit().putString(key, values).apply();
    }

    public static String getData(String key , Context mContext) {
        SharedPreferences xinhao = mContext.getSharedPreferences("xinhao", Context.MODE_PRIVATE);
        String def = xinhao.getString(key, "def");
        return def;
    }
}
