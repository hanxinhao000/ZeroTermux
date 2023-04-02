package com.zp.z_file.ui

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import com.zp.z_file.content.getZFileHelp
import com.zp.z_file.listener.ZFileSelectResultListener

internal class ZFileProxyFragment : Fragment() {

    companion object {
        internal const val TAG = "ZFileProxyFragment"
    }

    private var resultListener: ZFileSelectResultListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
    }

    fun jump(requestCode: Int, data: Intent, resultListener: ZFileSelectResultListener) {
        this.resultListener = resultListener
        startActivityForResult(data, requestCode)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val list = getZFileHelp().getSelectData(requestCode, resultCode, data)
        resultListener?.selectResult(list)
    }

}