package com.example.xh_lib.utils

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.widget.ImageView
import cn.bingoogolapple.qrcode.core.BGAQRCodeUtil
import cn.bingoogolapple.qrcode.zxing.QRCodeEncoder
import com.bumptech.glide.Glide
import com.draggable.library.extension.ImageViewerHelper
import com.example.xh_lib.R
import com.example.xh_lib.activity.city.data.LoginBean
import com.example.xh_lib.glide_utils.CornerTransform
import com.example.xh_lib.utils.UUtils.getContext
import com.example.xh_lib.utils.UUtils.showMsg
import com.google.gson.Gson
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * @author ZEL
 * @create By ZEL on 2020/7/17 17:41
 **/
object UUtils2 {
    //放大图片
    fun imgZoom(url: String) {

        val arrayList = ArrayList<String>()

        arrayList.add(url)

        ImageViewerHelper.showImages(UUtils.getContext(), arrayList, 0, true)

    }

    fun imgZoomMore(imgs: ArrayList<String>) {

        ImageViewerHelper.showImages(UUtils.getContext(), imgs, 0, true)

    }


    //获取版本号
    fun getVersionName(context: Context): String? {
        var versionName = ""
        try {
            val packageInfo = context.applicationContext
                .packageManager.getPackageInfo(context.packageName, 0)
            versionName = packageInfo.versionName
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        return versionName
    }


    fun codeStringReturn2(msg: String){


        try {


            val fromJson = Gson().fromJson<LoginBean>(msg, LoginBean::class.java)

            showMsg(fromJson.msg)
        }catch (e:java.lang.Exception){
            showMsg(UUtils.getString(R.string.服务器繁忙))

        }


    }

    //图片圆角
    fun imageRoundGlied(mActivity: Activity,url:String,mImageView:ImageView){


        val cornerTransform = CornerTransform(mActivity, 15f)
        cornerTransform.setExceptCorner(false,false,false,false)


        Glide
                .with(mActivity)
                .asBitmap()
                .skipMemoryCache(true)
                .load(url)
                .transform(cornerTransform)
                .into(mImageView)


    }



    //保存图片到本地

    fun saveImageToGallery(context: Context, bitmap: Bitmap, fileName: String): Boolean {
        // 保存图片至指定路径
        val storePath: String = Environment.getExternalStorageDirectory().getAbsolutePath().toString() + File.separator + "qrcode"
        val appDir = File(storePath)
        if (!appDir.exists()) {
            appDir.mkdir()
        }
        val file = File(appDir, fileName)
        try {
            val fos = FileOutputStream(file)
            //通过io流的方式来压缩保存图片(80代表压缩20%)
            val isSuccess = bitmap.compress(Bitmap.CompressFormat.JPEG, 80, fos)
            fos.flush()
            fos.close()

            //发送广播通知系统图库刷新数据
            val uri: Uri = Uri.fromFile(file)
            context.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri))
            UUtils.showMsg(UUtils.getString(R.string.baocun))
            return isSuccess
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return false
    }

    //复制到剪贴板
    fun copyToClip(msg: String): String {

        try {
            val cm = UUtils.getContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            // 将文本内容放到系统剪贴板里。
            // 将文本内容放到系统剪贴板里。
            cm.text = msg
            return UUtils.getString(R.string.fuzhichenggong)
        } catch (e: Exception) {
            return UUtils.getString(R.string.fuzhichenggongshibai)
        }


    }



    /**
     * 复制
     *
     * @param context
     * @param str
     */
    fun onClickCopy(context: Context, str: String?) {
        //获取剪贴板管理器：
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        // 创建普通字符型ClipData
        val mClipData = ClipData.newPlainText("Label", str)
        // 将ClipData内容放到系统剪贴板里。
        cm.setPrimaryClip(mClipData)
        UUtils.showMsg(UUtils.getString(R.string.fuzhichenggong))
    }

    //    public static File getSaveFile1(Context context, Bitmap bmp) {
    //        String storePath = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + R.string.app_name;
    //        File appDir = new File(storePath);
    //        if (!appDir.exists()) {
    //            appDir.mkdir();
    //        }
    //        String fileName = System.currentTimeMillis() + ".jpg";
    //        File file = new File(appDir, fileName);
    //        try {
    //            FileOutputStream fos = new FileOutputStream(file);
    //            //通过io流的方式来压缩保存图片
    //            bmp.compress(Bitmap.CompressFormat.JPEG, 60, fos);
    //            fos.flush();
    //            fos.close();
    //
    //            //把文件插入到系统图库
    //            //MediaStore.Images.Media.insertImage(context.getContentResolver(), file.getAbsolutePath(), fileName, null);
    //
    //            //保存图片后发送广播通知更新数据库
    //            Uri uri = Uri.fromFile(file);
    //            context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri));
    //
    //            return file;
    //        } catch (IOException e) {
    //            e.printStackTrace();
    //        }
    //        return file;
    //    }


    fun doPaste():String{


        var mClipData:ClipData
        var mClipboardManager:ClipboardManager = UUtils.getContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        mClipData = mClipboardManager.getPrimaryClip()!!;

        val item = mClipData.getItemAt(0)

        return item.text.toString()

    }

    public fun setTextViewStringColor(str: String, color: Int, startIndex: Int, endIndex: Int): SpannableString {

        val spannableString = SpannableString(str)
        spannableString.setSpan(ForegroundColorSpan(color), startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        //tv5.setText(spannableString)

        return spannableString
    }


    //生成二维码
    fun stringToImg(content: String, mActivity: Activity): Bitmap {


        return QRCodeEncoder.syncEncodeQRCode(content, BGAQRCodeUtil.dp2px(mActivity, 150f));


    }
}
