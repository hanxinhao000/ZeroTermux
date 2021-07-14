package com.termux.zerocore.data

import com.github.mjdev.libaums.fs.UsbFile

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

    public interface RefFileList{

        fun ref()


    }


}
