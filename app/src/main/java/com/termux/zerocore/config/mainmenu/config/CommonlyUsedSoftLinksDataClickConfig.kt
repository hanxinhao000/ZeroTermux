package com.termux.zerocore.config.mainmenu.config

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.View
import com.example.xh_lib.utils.LogUtils
import com.example.xh_lib.utils.UUtils
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.termux.R
import com.termux.zerocore.dialog.SwitchDialog
import com.termux.zerocore.utils.FileIOUtils
import java.io.File

class CommonlyUsedSoftLinksDataClickConfig: BaseMenuClickConfig() {
    companion object {
        val TAG = CommonlyUsedSoftLinksDataClickConfig::class.simpleName
    }
    override fun getIcon(context: Context?): Drawable? {
        return context?.getDrawable(R.mipmap.link_ico)
    }

    override fun getString(context: Context?): String? {
        return UUtils.getString(R.string.commonly_used_soft_links)
    }

    override fun onClick(view: View?, context: Context?) {
        val switchDialog = SwitchDialog(mContext!!)
        switchDialog.createSwitchDialog(UUtils.getString(R.string.create_soft_links))
        switchDialog.ok?.setOnClickListener {
            switchDialog.dismiss()
            XXPermissions.with(mContext)
                .permission(Permission.WRITE_EXTERNAL_STORAGE)
                .permission(Permission.READ_EXTERNAL_STORAGE)
                .request(object : OnPermissionCallback {
                    override fun onGranted(permissions: List<String>, all: Boolean) {
                        val xinhaoLinkPath = FileIOUtils.getXinhaoLinkPath(UUtils.getContext())
                        val file = File(xinhaoLinkPath)
                        if (!file.exists()) {
                            if (!file.mkdirs()) {
                                UUtils.showMsg(UUtils.getString(R.string.create_soft_links_fail))
                                LogUtils.d(TAG, "commonShortcuts file mkdirs fail return")
                                return
                            }
                        }

                        val downLoadPath = FileIOUtils.getDownLoadPath()
                        val sdcardPath = FileIOUtils.getSdcardPath()
                        val qqDownloadPath = FileIOUtils.getQQAndroidDownloadPath()
                        val weiXinPath = FileIOUtils.getWeiXinPath()
                        val weiXinAndroidPath = FileIOUtils.getWeiXinAndroidPath()
                        FileIOUtils.setupFileSymlinks(downLoadPath, "$xinhaoLinkPath/download")
                        FileIOUtils.setupFileSymlinks(sdcardPath, "$xinhaoLinkPath/sdcard")
                        FileIOUtils.setupFileSymlinks(qqDownloadPath, "$xinhaoLinkPath/QQDownload")
                        FileIOUtils.setupFileSymlinks(weiXinPath, "$xinhaoLinkPath/WXDownload")
                        FileIOUtils.setupFileSymlinks(weiXinAndroidPath, "$xinhaoLinkPath/WXAndroidDownload")
                        UUtils.showMsg(UUtils.getString(R.string.成功))
                    }

                    override fun onDenied(permissions: List<String>, never: Boolean) {

                    }
                })
        }
        switchDialog.show()
    }
}
