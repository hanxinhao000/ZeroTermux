package com.termux.zerocore.utils

import android.content.res.AssetManager
import com.hzy.lib7z.IExtractCallback
import com.hzy.lib7z.Z7Extractor
import java.io.File

object Z7ExtracatUtils {

    public var mUnZipCallBack: UnZipCallBack? = null

    public  fun unZipFile(mInputFile: File, mOutputPath: File) {
        Z7Extractor.extractFile(mInputFile.absolutePath, mOutputPath.absolutePath, object: IExtractCallback {
            override fun onStart() {
                mUnZipCallBack?.onStart()
            }

            override fun onGetFileNum(fileNum: Int) {
                mUnZipCallBack?.onGetFileNum(fileNum)
            }

            override fun onProgress(name: String?, size: Long) {
                mUnZipCallBack?.onProgress(name, size)
            }

            override fun onError(errorCode: Int, message: String?) {
                mUnZipCallBack?.onError(errorCode, message)
            }

            override fun onSucceed() {
                mUnZipCallBack?.onSucceed()
            }
        })
    }

    public fun setUnZipCallBack(mUnZipCallBack: UnZipCallBack) {
        this.mUnZipCallBack = mUnZipCallBack
    }

    public interface UnZipCallBack {
        fun onStart()
        fun onGetFileNum(fileNum: Int)
        fun onProgress(name: String?, size: Long)
        fun onError(errorCode: Int, message: String?)
        fun onSucceed()
    }

    public  fun unZipAssets(mAssetManager: AssetManager, name:String, mOutputPath: File) {
        Z7Extractor.extractAsset(mAssetManager, name, mOutputPath.absolutePath, object : IExtractCallback {
            override fun onStart() {
                mUnZipCallBack?.onStart()
            }

            override fun onGetFileNum(fileNum: Int) {
                mUnZipCallBack?.onGetFileNum(fileNum)
            }

            override fun onProgress(name: String?, size: Long) {
                mUnZipCallBack?.onProgress(name, size)
            }

            override fun onError(errorCode: Int, message: String?) {
                mUnZipCallBack?.onError(errorCode, message)
            }

            override fun onSucceed() {
                mUnZipCallBack?.onSucceed()
            }
        })
    }
}
