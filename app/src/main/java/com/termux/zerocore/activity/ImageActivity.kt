package com.termux.zerocore.activity

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.xh_lib.utils.UUtils
import com.termux.R
import com.termux.shared.logger.Logger
import com.termux.zerocore.data.UsbFileData
import com.zzti.fengyongge.imagepicker.ImagePickerInstance
import java.io.File


class ImageActivity : AppCompatActivity() {
    private val LOG_TAG = "Termux--Apk:ImageActivity"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image)
        ImagePickerInstance.getInstance().photoSelect(this,1,true,5558);
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            5558 -> {

                //IMG_1641139954166.jpg
                if (data != null) {
                    try {
                        val paths = data.extras!!
                            .getSerializable("photos") as List<String>?
                        if(paths == null || paths.size == 0){
                            UUtils.showMsg("image get error,try again")
                            return
                        }
                        val imageFileCheckListener = UsbFileData.get().getImageFileCheckListener()
                        imageFileCheckListener?.imageFile(File(paths[0]))
                        finish()

                        Logger.logDebug(LOG_TAG, "change:$paths")
                    //处理代码
                    }catch (e:Exception){
                        e.printStackTrace()
                        UUtils.showMsg("image get error,try again")
                    }

                }

            }

            else -> {
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }
}
