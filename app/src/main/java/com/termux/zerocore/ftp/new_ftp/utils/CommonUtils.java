package com.termux.zerocore.ftp.new_ftp.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.View;

import androidx.annotation.NonNull;


import com.google.android.material.snackbar.Snackbar;
import com.termux.R;
import com.termux.zerocore.ftp.new_ftp.Constants;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class CommonUtils {

    /**
     * 拿到SP实例
     */
    public static SharedPreferences getSettingSharedPreferences(@NonNull Context context) {
        return context.getSharedPreferences(Constants.PreferenceConsts.FILE_NAME, Context.MODE_PRIVATE);
    }

    /**
     * 编码展示值
     *
     * @return GBK(简体中文)或者UTF-8(默认)
     */
    public static String getDisplayCharsetValue(@NonNull Context context) {
        return context.getResources().getString(
                Constants.Charset.CHAR_GBK.equals(getSettingSharedPreferences(context).getString(Constants.PreferenceConsts.CHARSET_TYPE, Constants.PreferenceConsts.CHARSET_TYPE_DEFAULT)) ?
                        R.string.item_charset_gbk : R.string.item_charset_utf
        );
    }

    /**
     * Ftp服务正在运行的SnackBar提示
     *
     * @param activity 要显示的activity
     */
    public static void showSnackBarOfFtpServiceIsRunning(@NonNull Activity activity) {
        try {
            Snackbar.make(activity.findViewById(android.R.id.content), activity.getResources().getString(R.string.attention_ftp_is_running), Snackbar.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 向activity发送请求读写权限的snackBar
     */
    public static void showSnackBarOfRequestingWritingPermission(@NonNull final Activity activity) {
        Snackbar snackbar = Snackbar.make(activity.findViewById(android.R.id.content),
                activity.getResources().getString(R.string.permission_write_external),
                Snackbar.LENGTH_SHORT);
        snackbar.setAction(activity.getResources().getString(R.string.snackbar_action_goto), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setAction(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.fromParts("package", activity.getApplication().getPackageName(), null));
                activity.startActivity(intent);
            }
        });
        snackbar.show();
    }

    /**
     * 判断是否为匿名模式
     *
     * @return true-为匿名模式
     */
    public static boolean isAnonymousMode(@NonNull Context context) {
        return getSettingSharedPreferences(context).getBoolean(Constants.PreferenceConsts.ANONYMOUS_MODE, Constants.PreferenceConsts.ANONYMOUS_MODE_DEFAULT);
    }

    /**
     * judge if it is a child path of parent
     */
    public static boolean isChildPathOfCertainPath(File child, File parent) {
        try {
            return child.getAbsolutePath().toLowerCase().startsWith(parent.getAbsolutePath().toLowerCase());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private static String getIPAddressForFTPService(Context context) {
        if (NetworkEnvironmentUtil.isWifiConnected(context)) {
            final String wifiIp = NetworkEnvironmentUtil.getWifiIp(context);
            if (!TextUtils.isEmpty(wifiIp)) return wifiIp;
        }
        ArrayList<String> ips = NetworkEnvironmentUtil.getLocalIpv4Addresses();
        if (ips.size() > 0) return ips.get(0);
        return "127.0.0.1";
    }

    public static String getFTPServiceDisplayAddress(@NonNull Context context) {
        return getFtpServiceAddress(context, CommonUtils.getIPAddressForFTPService(context));
    }

    public static String getFtpServiceAddress(@NonNull Context context, @NonNull String ip) {
        return "ftp://" + ip + ":"
                + context.getSharedPreferences(Constants.PreferenceConsts.FILE_NAME, Context.MODE_PRIVATE)
                .getInt(Constants.PreferenceConsts.PORT_NUMBER, Constants.PreferenceConsts.PORT_NUMBER_DEFAULT);
    }
    /*public static Bitmap getQrCodeBitmapOfString(String content,int w,int h){
        try{
            Hashtable<EncodeHintType,String> hints=new Hashtable<>();
            hints.put(EncodeHintType.CHARACTER_SET,"utf-8");
            BitMatrix bitMatrix=new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE,w,h,hints);
            int []pixels=new int[w*h];
            for (int y = 0; y < h; y++){
                for (int x = 0; x < w; x++){
                    if (bitMatrix.get(x, y)){
                        pixels[y * w + x] = 0xff000000;
                    }
                    else{
                        pixels[y * w + x] = 0xffffffff;
                    }
                }
            }
            Bitmap bitmap= Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            bitmap.setPixels(pixels,0,w,0,0,w,h);
            return bitmap;
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }*/

    public static int dip2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    /**
     * 获取本应用名称
     */
    public static @NonNull
    String getAppName(@NonNull Context context) {
        try {
            PackageManager packageManager = context.getPackageManager();
            ApplicationInfo applicationInfo = packageManager.getApplicationInfo(context.getPackageName(), 0);
            return String.valueOf(packageManager.getApplicationLabel(applicationInfo));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * 获取本应用版本名
     */
    public static @NonNull
    String getAppVersionName(@NonNull Context context) {
        try {
            PackageManager packageManager = context.getPackageManager();
            return String.valueOf(packageManager.getPackageInfo(context.getPackageName(), 0).versionName);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * 将秒数转换为按时分秒的格式的字符串
     */
    public static String getDisplayTimeOfSeconds(int seconds) {
        int hour = seconds / 3600;
        int minute = seconds % 3600 / 60;
        int second = seconds % 60;
        return getNumberWithZero(hour) + ":"
                + getNumberWithZero(minute) + ":"
                + getNumberWithZero(second);
    }

    /**
     * 将0~9之间的数值转换为00,01的数值字符串
     */
    private static String getNumberWithZero(int value) {
        if (value >= 0 && value < 10) {
            return "0" + value;
        }
        return String.valueOf(value);
    }


    public static String getDisplayTimeOfMillis(long millis) {
        Date date = new Date(millis);
        return date.toLocaleString();
    }

    /**
     * 计算出来的位置，y方向就在anchorView的上面和下面对齐显示，x方向就是与屏幕右边对齐显示
     * 如果anchorView的位置有变化，就可以适当自己额外加入偏移来修正
     *
     * @param anchorView  呼出window的view
     * @param contentView window的内容布局
     * @return window显示的左上角的xOff, yOff坐标
     */
    public static int[] calculatePopWindowPos(final View anchorView, final View contentView) {
        final int windowPos[] = new int[2];
        final int anchorLoc[] = new int[2];
        // 获取锚点View在屏幕上的左上角坐标位置
        anchorView.getLocationOnScreen(anchorLoc);
        final int anchorHeight = anchorView.getHeight();
        // 获取屏幕的高宽
        final int screenHeight = anchorView.getContext().getResources().getDisplayMetrics().heightPixels;//ScreenUtils.getScreenHeight(anchorView.getContext());
        final int screenWidth = anchorView.getResources().getDisplayMetrics().widthPixels;
        contentView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        // 计算contentView的高宽
        final int windowHeight = contentView.getMeasuredHeight();
        final int windowWidth = contentView.getMeasuredWidth();
        // 判断需要向上弹出还是向下弹出显示
        final boolean isNeedShowUp = (screenHeight - anchorLoc[1] - anchorHeight < windowHeight);
        if (isNeedShowUp) {
            windowPos[0] = screenWidth - windowWidth;
            windowPos[1] = anchorLoc[1] - windowHeight;
        } else {
            windowPos[0] = screenWidth - windowWidth;
            windowPos[1] = anchorLoc[1] + anchorHeight;
        }
        return windowPos;
    }

    public static boolean isALegalFileName(@NonNull String name) {
        try {
            if (name.trim().isEmpty()) return false;
            if (name.contains("?") || name.contains("\\") || name.contains("/") || name.contains(":") || name.contains("*") || name.contains("\"")
                    || name.contains("<") || name.contains(">") || name.contains("|")) return false;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    /**
     * 更新上下文的资源Resources(更新语言)
     */
    public static void updateResourcesOfContext(@NonNull Context context) {
        // 获得res资源对象
        Resources resources = context.getResources();
        // 获得屏幕参数：主要是分辨率，像素等。
        DisplayMetrics metrics = resources.getDisplayMetrics();
        // 获得配置对象
        Configuration config = resources.getConfiguration();
        //区别17版本（其实在17以上版本通过 config.locale设置也是有效的，不知道为什么还要区别）
        //在这里设置需要转换成的语言，也就是选择用哪个values目录下的strings.xml文件
        int value = CommonUtils.getSettingSharedPreferences(context).getInt(Constants.PreferenceConsts.LANGUAGE_SETTING, Constants.PreferenceConsts.LANGUAGE_FOLLOW_SYSTEM);
        Locale locale = null;
        switch (value) {
            default:
                break;
            case Constants.PreferenceConsts.LANGUAGE_FOLLOW_SYSTEM:
                locale = Locale.getDefault();
                break;
            case Constants.PreferenceConsts.LANGUAGE_SIMPLIFIED_CHINESE:
                locale = Locale.SIMPLIFIED_CHINESE;
                break;
            case Constants.PreferenceConsts.LANGUAGE_ENGLISH:
                locale = Locale.ENGLISH;
                break;
        }
        if (locale == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            config.setLocale(locale);
        } else {
            config.locale = locale;
        }
        resources.updateConfiguration(config, metrics);
    }
}
