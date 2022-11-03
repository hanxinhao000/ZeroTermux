package com.termux.zerocore.activity

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.xh_lib.utils.LogUtils
import com.example.xh_lib.utils.UUtils
import com.lcw.library.imagepicker.ImagePicker
import com.termux.R
import com.termux.shared.logger.Logger
import com.termux.zerocore.data.UsbFileData
import com.termux.zerocore.loader.ZeroTermuxImageLoader
import com.termux.zerocore.utils.FileIOUtils
import java.io.File


class ImageActivity : AppCompatActivity() {
    object ImageActivityFlgh{
        public val IMAGE_FLGH = "IMAGE"
        public val VIDEO_FLGH = "VIDEO"
        public val REQUEST_SELECT_IMAGE_CODE = 8889
        public val REQUEST_SELECT_VIDEO_CODE = 8888
    }
    private val TAG = "ImageActivity"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image)
        intent?.let {
            LogUtils.d(TAG, "onCreate intent action is:${it.action}")
            when (it.action) {
                ImageActivityFlgh.IMAGE_FLGH ->{
                    ImagePicker.getInstance()
                        .setTitle("select Image") //设置标题
                        .showCamera(true) //设置是否显示拍照按钮
                        .showImage(true) //设置是否展示图片
                        .showVideo(false) //设置是否展示视频
                        .setSingleType(true) //设置图片视频不能同时选择
                        .setImageLoader( ZeroTermuxImageLoader())//设置自定义图片加载器
                        .setMaxCount(1) //设置最大选择图片数目(默认为1，单选)
                        .start(
                            this@ImageActivity,
                            ImageActivityFlgh.REQUEST_SELECT_IMAGE_CODE
                        ) //REQEST_SELECT_IMAGES_CODE为Intent调用的requestCode
                }
                ImageActivityFlgh.VIDEO_FLGH ->{
                    ImagePicker.getInstance()
                        .setTitle("select video") //设置标题
                        .showCamera(false) //设置是否显示拍照按钮
                        .showImage(false) //设置是否展示图片
                        .showVideo(true) //设置是否展示视频
                        .setImageLoader( ZeroTermuxImageLoader())
                        .setMaxCount(1) //设置最大选择图片数目(默认为1，单选)
                        .start(
                            this@ImageActivity,
                            ImageActivityFlgh.REQUEST_SELECT_VIDEO_CODE
                        ) //REQEST_SELECT_IMAGES_CODE为Intent调用的requestCode
                }
            }
        }

    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            ImageActivityFlgh.REQUEST_SELECT_IMAGE_CODE -> {
                LogUtils.d(TAG, "onActivityResult data is:${data}")
                data?.let {
                    val imagePaths: List<String>? =
                        it.getStringArrayListExtra(ImagePicker.EXTRA_SELECT_IMAGES)
                    LogUtils.d(TAG, "onActivityResult imagePath is:${imagePaths}")
                    if (imagePaths == null || imagePaths.isEmpty()) {
                        LogUtils.d(TAG, "onActivityResult imagePath is null or Empty")
                        return
                    }
                    val path: String = imagePaths[0]
                    val file = File(path)
                    LogUtils.d(TAG, "onActivityResult file size = ${file.length()}")
                    if (FileIOUtils.isFileSize5Mb(file)) {
                        LogUtils.d(TAG, "onActivityResult file size > 5MB")
                        UUtils.showMsg(UUtils.getString(R.string.image_size_error))
                        finish()
                        return
                    }
                    val imageFileCheckListener = UsbFileData.get().getImageFileCheckListener()
                    imageFileCheckListener?.imageFile(file)
                    finish()
                }
            }
            ImageActivityFlgh.REQUEST_SELECT_VIDEO_CODE -> {
                LogUtils.d(TAG, "onActivityResult data is:${data}")
                data?.let {
                    val imagePaths: List<String>? =
                        it.getStringArrayListExtra(ImagePicker.EXTRA_SELECT_IMAGES)
                    LogUtils.d(TAG, "onActivityResult videoPath is:${imagePaths}")
                    if (imagePaths == null || imagePaths.isEmpty()) {
                        LogUtils.d(TAG, "onActivityResult imagePath is null or Empty")
                        return
                    }
                    val path: String = imagePaths[0]
                    val file = File(path)
                    if (FileIOUtils.isFileSize100Mb(file)) {
                        LogUtils.d(TAG, "onActivityResult file size > 100MB")
                        UUtils.showMsg(UUtils.getString(R.string.image_size_error))
                        finish()
                        return
                    }
                    val imageFileCheckListener = UsbFileData.get().getImageFileCheckListener()
                    imageFileCheckListener?.imageFile(file)
                    finish()
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }
}
