package com.termux.zerocore.data

import com.github.mjdev.libaums.fs.UsbFile
import java.io.File

public class UsbFileData {

    companion object {
        private lateinit var instance: UsbFileData

        @Synchronized
        fun get(): UsbFileData {
            if (!this::instance.isInitialized) instance = UsbFileData()
            return instance
        }
    }


    public var mUsbFile: UsbFile? = null


    public var mRefFileList:RefFileList? = null

    public var mImageFileCheckListener:ImageFileCheckListener? = null

    public interface RefFileList{
        fun ref()
    }

    public fun setImageFileCheckListener(mImageFileCheckListener:ImageFileCheckListener){
        this.mImageFileCheckListener = mImageFileCheckListener
    }

    public fun getImageFileCheckListener():ImageFileCheckListener?{
        return mImageFileCheckListener
    }

    public interface ImageFileCheckListener{
        fun imageFile(file:File)
    }

}
