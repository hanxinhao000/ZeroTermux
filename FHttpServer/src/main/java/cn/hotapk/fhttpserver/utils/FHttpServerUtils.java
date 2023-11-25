package cn.hotapk.fhttpserver.utils;

import android.content.Context;

public class FHttpServerUtils {

    private Context context;

    private volatile static FHttpServerUtils fUtils;

    private FHttpServerUtils(Context context) {
        this.context = context;
    }

    public static void init(Context context) {
        if (fUtils == null) {
            synchronized (FHttpServerUtils.class) {
                if (fUtils == null) {
                    fUtils = new FHttpServerUtils(context);
                }
            }
        }
    }

    public static Context getAppContext() {
        if (fUtils != null) return fUtils.context.getApplicationContext();
        throw new NullPointerException("To initialize first");
    }

}
