package com.example.xh_lib.utils;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.telephony.TelephonyManager;
import android.text.InputType;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.draggable.library.extension.ImageViewerHelper;
import com.example.xh_lib.R;
import com.example.xh_lib.statusBar.StatusBarCompat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.zip.GZIPInputStream;

import androidx.annotation.LayoutRes;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;
import okio.ByteString;

/**
 * @author ZEL
 * @create By ZEL on 2020/7/16 14:57
 **/
public class UUtils {

    /**
     * 常用变量
     */
    private static final SimpleDateFormat format = new SimpleDateFormat();
    public static final String DATE_FORMAT_YMD = "yyyy-MM-dd";
    public static final String DATE_FORMAT_YMDHM = "yyyy-MM-dd HH:mm";

    private static Context mContext;
    private static Handler mHandler;


    public static void initUUtils(Context mContextZ, Handler mHandlerZ) {

        mContext = mContextZ;
        mHandler = mHandlerZ;

    }

    public static Context getContext() {


        return mContext;

    }


    public static Handler getHandler() {

        return mHandler;

    }


    //强制主线程运行
    public static void runOnUIThread(Runnable r) {


        if (Looper.getMainLooper() == Looper.myLooper()) {
            //主线程
            r.run();

        } else {
            //子线程
            getHandler().post(r);

        }


    }

    //强制子线程运行
    public static void runOnThread(Runnable r) {


        if (Looper.getMainLooper() == Looper.myLooper()) {
            //主线程
            new Thread(r).start();

        } else {
            //子线程
            r.run();

        }

    }

    //获取字符串
    public static String getString(int id) {

        return getContext().getResources().getString(id);

    }

    //获取颜色
    public static int getColor(int id) {

        return getContext().getResources().getColor(id);

    }

    //去除重复集合
    public static ArrayList<String> removeDuplicate_1(ArrayList<String> list){
        for(int i =0;i<list.size()-1;i++){
            for(int j=list.size()-1;j>i;j--){
                if(list.get(i).equals(list.get(j)))
                    list.remove(j);
            }
        }

        return list;
    }

    //显示消息
    public static void showMsg(String msg){


        Toast.makeText(mContext, msg, Toast.LENGTH_SHORT).show();

    }

    //打印日志
    public static void showLog(String log){

        Log.e("ada:",  log );

    }




    /**
     * 日期转换为制定格式字符串
     */
    public static String formatDateToString(Date time, String pattern) {
        try {
            format.applyPattern(pattern);
            return format.format(time);
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * 计算两个时间的间隔
     */
    public static long getDiffBetween(Date startDate, Date endDate, long type) {
        if (null == startDate || null == endDate) {
            return 0;
        }
        return (int) ((endDate.getTime() - startDate.getTime()) / type);
    }


    /**
     * 去掉多余的0
     *
     * @param s
     * @return
     */
    public static String rvZeroAndDot(String s) {
        if (s.isEmpty()) {
            return null;
        }
        if (s.indexOf(".") > 0) {
            s = s.replaceAll("0+?$", "");//去掉多余的0
            s = s.replaceAll("[.]$", "");//如最后一位是.则去掉
        }
        return s;
    }

    //获取小数位
    public static int bigDecimalScale(String str) {
        BigDecimal bd = new BigDecimal(str);
        return bd.scale();
    }

    //保留小数位
    public static String bigDecimal(String str, int scale) {
        if (str.equals("")) {
            return "0";
        }
        BigDecimal bd = new BigDecimal(str);
        if (scale == 0) {
            return bd.toPlainString();
        } else {
            return bd.setScale(scale, BigDecimal.ROUND_DOWN).toPlainString();
        }
    }

    public static View getViewLay(@LayoutRes int mId) {


        return View.inflate(getContext(), mId, null);

    }

    public static View getViewLayViewGroup(@LayoutRes int mId, ViewGroup parent){

       return LayoutInflater.from(UUtils.getContext()).inflate(mId, parent, false);


    }


    //加法
    public static String bigDecimalAdd(String str, String str1, int scale) {
        BigDecimal bd = new BigDecimal(str);
        BigDecimal bd1 = new BigDecimal(str1);
        return rvZeroAndDot(bd.add(bd1).setScale(scale, BigDecimal.ROUND_DOWN).toPlainString());
    }

    //减法
    public static String bigDecimalSubtract(String str, String str1, int scale) {
        BigDecimal bd = new BigDecimal(str);
        BigDecimal bd1 = new BigDecimal(str1);
        return rvZeroAndDot(bd.subtract(bd1).setScale(scale, BigDecimal.ROUND_DOWN).toPlainString());
    }

    //乘法
    public static String bigDecimalMultiply(String str, String str1, int scale) {
        BigDecimal bd = new BigDecimal(str);
        BigDecimal bd1 = new BigDecimal(str1);
        if (scale == -1) {
            return rvZeroAndDot(bd.multiply(bd1).toPlainString());
        } else {
            return rvZeroAndDot(bd.multiply(bd1).setScale(scale, BigDecimal.ROUND_DOWN).toPlainString());
        }
    }

    //除法
    public static String bigDecimalDivide(String str, String str1, int scale) {
        if (bigDecimalCompareTo(str1, "0") <= 0 || str1.equals("")) {
            return "0";
        }
        BigDecimal bd = new BigDecimal(str);
        BigDecimal bd1 = new BigDecimal(str1);
        return rvZeroAndDot(bd.divide(bd1, scale, BigDecimal.ROUND_DOWN).toPlainString());
    }

    //比大小    -1小于  0 等于 1 大于
    public static int bigDecimalCompareTo(String str, String str1) {
        BigDecimal bd = new BigDecimal(str);
        BigDecimal bd1 = new BigDecimal(str1);
        return bd.compareTo(bd1);
    }

    /**
     * 时间转时间戳
     *
     * @param time
     * @return
     * @throws ParseException
     */
    public static String dateToStamp(String time) throws ParseException {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = simpleDateFormat.parse(time);
        long ts = date.getTime();
        return String.valueOf(ts);
    }


    /**
     * 时间戳转换为时间
     */
    public static String stampToDate(long timeMillis) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = new Date(timeMillis);
        return simpleDateFormat.format(date);
    }

    /**
     * 时间戳转换为时间
     */
    public static String stampToDate1(long timeMillis) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm:ss");
        Date date = new Date(timeMillis);
        return simpleDateFormat.format(date);
    }



    /**
     * 时间戳转换为时间
     */
    public static String stampToDate3(long timeMillis) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm");
        Date date = new Date(timeMillis);
        return simpleDateFormat.format(date);
    }

    /**
     * 时间戳转换为时间
     */
    public static String stampToDate4(long timeMillis) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("mm分ss秒");
        Date date = new Date(timeMillis);
        return simpleDateFormat.format(date);
    }


    /**
     * 时间戳转换为时间
     */
    public static String stampToDate2(long timeMillis) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Date date = new Date(timeMillis);
        return simpleDateFormat.format(date);
    }


    //gzip 解压
    public static String uncompressGzip(Object message) {
        ByteString msg = (ByteString) message;
        byte[] b = Base64.decode(msg.toByteArray(), Base64.DEFAULT);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayInputStream in = new ByteArrayInputStream(b);
        try {
            GZIPInputStream ungzip = new GZIPInputStream(in);
            byte[] buffer = new byte[256];
            int n;
            while ((n = ungzip.read(buffer)) >= 0) {
                out.write(buffer, 0, n);
            }
            Log.i("wxx", out.toString("UTF-8"));
            return out.toString("UTF-8");
        } catch (IOException e) {
            Log.e("gzip compress error.", e.getMessage());
        }
        return null;
    }


    /**
     * 手机号用****号隐藏中间数字
     *
     * @param phone
     * @return
     */
    public static String settingphone(String phone) {
        String phone_s = phone.replaceAll("(\\d{3})\\d{4}(\\d{4})", "$1****$2");
        return phone_s;
    }


    /**
     * 邮箱用****号隐藏前面的字母
     *
     * @return
     */
    public static String settingemail(String email) {
        String emails = email.replaceAll("(\\w?)(\\w+)(\\w)(@\\w+\\.[a-z]+(\\.[a-z]+)?)", "$1****$3$4");
        return emails;
    }


    /**
     * view生成bitmap
     *
     * @return
     */
    public static Bitmap ViewToBitmap(View view, int color) {
        int w1 = view.getWidth();
        int h1 = view.getHeight();
        Bitmap bitmap = Bitmap.createBitmap(w1, h1, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(color);
        view.layout(0, 0, w1, h1);
        view.draw(canvas);
        return bitmap;
    }


    /**
     * 多个bitmap  合成
     *
     * @return
     */
    public static Bitmap DrawMulti(Bitmap first, Bitmap second) {
        int width = Math.max(first.getWidth(), second.getWidth());
        int height = first.getHeight() + second.getHeight();
        Bitmap result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);
        canvas.drawBitmap(first, 0, 0, null);
        canvas.drawBitmap(second, 0, first.getHeight(), null);
        return result;
    }



    //设置状态栏为透明
    public static void topUITransparent(Activity mActivity){

        StatusBarCompat.translucentStatusBar(mActivity, true);
    }

    //取消设置状态栏为透明
    public static void topCancelUITransparent(Activity mActivity){

        StatusBarCompat.translucentStatusBar(mActivity, false);
    }

    //设置状态栏字体颜色为黑色
    public static void topUIColorBack(Activity mActivity){

        StatusBarCompat.changeToLightStatusBar(mActivity);

    }




    //设置状态栏字体颜色为白色
    public static void topUIColorWhite(Activity mActivity){

        StatusBarCompat.cancelLightStatusBar(mActivity);

    }



    //设置状态栏字体颜色
    public static void topUIColor(Activity mActivity,int color){

        StatusBarCompat.setStatusBarColor(mActivity,color);

    }

    //延迟启动
    public static void sleepSetRun(Runnable r){

        getHandler().postDelayed(r, 3000);


    }

    //移除掉
    public static void sleepRemoveRun(Runnable r){

        getHandler().removeCallbacks(r);
    }

    //获取内部files文件夹
    public static File getAppFilesCache(){

        return getContext().getFilesDir();

    }

    //获取内部cache文件夹
    public static File getAppCacheFile(){

        return getContext().getCacheDir();

    }


    //获取外部cache文件夹
    public static File getAppCacheFileSd(){

        return getContext().getExternalCacheDir();

    }

    //editText 隐藏显示
    public static void editTextPwdGV(EditText editText, ImageView imageView,int goneId,int viId){

        Object tag = editText.getTag();
        if(tag == null){
            tag = true;
        }

        if((Boolean)tag){
            //隐藏
            tag = false;

            editText.setTag(tag);

            editText.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);

            imageView.setImageResource(goneId);

        }else{


            //显示
            tag = true;



            editText.setTag(tag);

            editText.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD | InputType.TYPE_CLASS_TEXT);




            imageView.setImageResource(viId);

        }



    }


    //editText 隐藏显示
    public static void editTextPwdGVNumber(EditText editText, ImageView imageView,int goneId,int viId){

        Object tag = editText.getTag();
        if(tag == null){
            tag = true;
        }

        if((Boolean)tag){
            //隐藏
            tag = false;
            editText.setTag(tag);

            editText.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD | InputType.TYPE_CLASS_NUMBER);


            imageView.setImageResource(goneId);
        }else{


            //显示
            tag = true;
            editText.setTag(tag);




            editText.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD | InputType.TYPE_CLASS_TEXT);



            imageView.setImageResource(viId);

        }



    }

    //获取外部files文件夹
    public static File getAppFilesCacheSd(String path){

        return getContext().getExternalFilesDir(path);

    }

    /**
     * 获取当前手机系统语言。
     *
     * @return 返回当前系统语言。例如：当前设置的是“中文-中国”，则返回“zh-CN”
     */
    public static String getSystemLanguage() {
        return Locale.getDefault().getLanguage();
    }

    /**
     * 获取当前系统上的语言列表(Locale列表)
     *
     * @return  语言列表
     */
    public static Locale[] getSystemLanguageList() {
        return Locale.getAvailableLocales();
    }

    /**
     * 获取当前手机系统版本号
     *
     * @return  系统版本号
     */
    public static String getSystemVersion() {
        return android.os.Build.VERSION.RELEASE;
    }

    /**
     * 获取手机型号
     *
     * @return  手机型号
     */
    public static String getSystemModel() {
        return android.os.Build.MODEL;
    }

    /**
     * 获取手机厂商
     *
     * @return  手机厂商
     */
    public static String getDeviceBrand() {
        return android.os.Build.BRAND;
    }

    /**
     * 获取手机IMEI(需要“android.permission.READ_PHONE_STATE”权限)
     *
     * @return  手机IMEI
     */
    public static String getIMEI(Context ctx) {
        TelephonyManager tm = (TelephonyManager) ctx.getSystemService(Activity.TELEPHONY_SERVICE);
        if (tm != null) {
            return tm.getDeviceId();
        }
        return null;
    }

    /***
     *
     *
     *   使用指纹  如
     *
     *    FingerPrintUtils fingerPrintUtils = new FingerPrintUtils(mActivity, false);
     *
     *
     *                         fingerPrintUtils.setStartFingerListener(new FingerPrintUtils.StartFingerListener() {
     *                             @Override
     *                             public void onUsePassword() {
     *                                 UUtils.showMsg("使用密码");
     *                             }
     *
     *                             @Override
     *                             public void onNonsupport() {
     *                                 UUtils.showMsg("验证失败");
     *                             }
     *
     *                             @Override
     *                             public void onSucceeded() {
     *                                 UUtils.showMsg("验证成功");
     *                             }
     *
     *                             @Override
     *                             public void onFailed() {
     *                                 UUtils.showMsg("验证失败");
     *                             }
     *
     *                             @Override
     *                             public void onError(int code, String reason) {
     *                                 UUtils.showMsg("错误");
     *                             }
     *
     *                             @Override
     *                             public void onCancel() {
     *                                 UUtils.showMsg("返回");
     *                             }
     *                         });
     *
     *
     */

}
