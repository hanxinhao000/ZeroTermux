package com.zp.z_file.common

import android.content.Context
import android.content.Intent
import android.os.Parcelable
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import com.zp.z_file.content.*
import com.zp.z_file.dsl.result
import com.zp.z_file.listener.*
import com.zp.z_file.ui.ZFileListActivity2
import com.zp.z_file.ui.ZFileProxyFragment
import com.zp.z_file.ui.ZFileQWActivity
import com.zp.z_file.util.ZFileLog

class ZFileManageHelp {

    private object Builder {
        val MANAGER = ZFileManageHelp()
    }

    companion object {
        @JvmStatic
        fun getInstance() = Builder.MANAGER
    }

    /**
     * 图片类型和视频类型的显示方式，必须手动实现 并在调用前或Application中初始化
     */
    private var imageLoadeListener: ZFileImageListener? = null
    internal fun getImageLoadListener(): ZFileImageListener {
        if (imageLoadeListener == null) {
            ZFileLog.e("必须手动实现ZFileImageListener，并在调用前使用【ZFileManageHelp.init()】初始化！")
            throw ZFileException("ZFileImageListener is Null, You need call method \"ZFileManageHelp.init()\"")
        }
        return imageLoadeListener!!
    }

    fun init(imageLoadeListener: ZFileImageListener): ZFileManageHelp {
        this.imageLoadeListener = imageLoadeListener
        return this
    }

    /**
     * 文件数据获取
     */
    private var fileLoadListener: ZFileLoadListener = ZFileDefaultLoadListener()
    internal fun getFileLoadListener() = fileLoadListener
    fun setFileLoadListener(fileLoadListener: ZFileLoadListener): ZFileManageHelp {
        this.fileLoadListener = fileLoadListener
        return this
    }

    /**
     * QQ or WeChat 文件获取
     */
    private var qwLoadListener: ZQWFileLoadListener? = null
    internal fun getQWFileLoadListener() = qwLoadListener
    fun setQWFileLoadListener(qwLoadListener: ZQWFileLoadListener?): ZFileManageHelp {
        this.qwLoadListener = qwLoadListener
        return this
    }

    /**
     * 文件类型
     */
    private var fileTypeListener = ZFileTypeListener()
    internal fun getFileTypeListener() = fileTypeListener
    fun setFileTypeListener(fileTypeListener: ZFileTypeListener): ZFileManageHelp {
        this.fileTypeListener = fileTypeListener
        return this
    }

    /**
     * 文件点击
     */
    private var fileClickListener = ZFileClickListener()
    internal fun getFileClickListener() = fileClickListener
    fun setFileClickListener(fileClickListener: ZFileClickListener): ZFileManageHelp {
        this.fileClickListener = fileClickListener
        return this
    }

    /**
     * 文件操作
     */
    private var fileOperateListener = ZFileOperateListener()
    internal fun getFileOperateListener() = fileOperateListener
    fun setFileOperateListener(fileOperateListener: ZFileOperateListener): ZFileManageHelp {
        this.fileOperateListener = fileOperateListener
        return this
    }

    /**
     * 打开默认支持的文件
     */
    private var fileOpenListener = ZFileOpenListener()
    internal fun getFileOpenListener() = fileOpenListener
    fun setFileOpenListener(fileOpenListener: ZFileOpenListener): ZFileManageHelp {
        this.fileOpenListener = fileOpenListener
        return this
    }

    /**
     * 文件夹 标签/角标、说明文字 相关
     */
    private var fileBadgeHintListener = ZFileFolderBadgeHintListener()
    internal fun getFileBadgeHintListener() = fileBadgeHintListener
    fun setFileBadgeHintListener(fileBadgeHintListener: ZFileFolderBadgeHintListener): ZFileManageHelp {
        this.fileBadgeHintListener = fileBadgeHintListener
        return this
    }

    /**
     * 其他操作
     */
    private var otherFileListener: ZFileOtherListener? = null
    internal fun getOtherListener() = otherFileListener
    fun setOtherFileListener(otherListener: ZFileOtherListener?): ZFileManageHelp {
        this.otherFileListener = otherListener
        return this
    }

    /**
     * 文件的相关配置信息
     */
    private var config = ZFileConfiguration()
    fun getConfiguration() = config
    fun setConfiguration(config: ZFileConfiguration): ZFileManageHelp {
        this.config = config
        return this
    }

    /**
     * 获取返回的数据
     */
    fun getSelectData(requestCode: Int, resultCode: Int, data: Intent?): MutableList<ZFileBean>? {
        var list: MutableList<ZFileBean>? = arrayListOf()
        if (requestCode == ZFILE_REQUEST_CODE && resultCode == ZFILE_RESULT_CODE) {
            list = data?.getParcelableArrayListExtra<ZFileBean>(ZFILE_SELECT_DATA_KEY)
        }
        return list
    }

    /**
     * 将选中的数据 传递给调用页面 -> 前提是通过 [start] 或 [ZFileManageHelp.result] 扩展函数 调用
     * @param activity FragmentActivity
     * @param selectList MutableList<ZFileBean>?    选中的数据
     * @param finish Boolean    是否销毁页面 -> true：销毁页面；false：不销毁页面
     */
    @JvmOverloads
    fun setResult(
        activity: FragmentActivity,
        selectList: MutableList<ZFileBean>?,
        finish: Boolean = true
    ) {
        activity.setResult(ZFILE_RESULT_CODE, Intent().apply {
            putParcelableArrayListExtra(
                ZFILE_SELECT_DATA_KEY,
                selectList as java.util.ArrayList<out Parcelable>
            )
        })
        if (finish) {
            activity.finish()
        }
    }

    /**
     * 重置所有配置信息
     * @param imageLoadReset Boolean  是否重置 [ZFileImageListener]
     */
    @JvmOverloads
    fun resetAll(imageLoadReset: Boolean = false) {
        if (imageLoadReset) imageLoadeListener = null
        fileLoadListener = ZFileDefaultLoadListener()
        qwLoadListener = null
        fileTypeListener = ZFileTypeListener()
        fileOperateListener = ZFileOperateListener()
        fileOpenListener = ZFileOpenListener()
        fileClickListener = ZFileClickListener()
        fileBadgeHintListener = ZFileFolderBadgeHintListener()
        otherFileListener = null
        config = ZFileConfiguration()
    }

    /**
     * 跳转至文件管理页面  推荐使用 [ZFileManageHelp.result] 扩展函数
     * @param fragmentOrActivity  [FragmentActivity] or [Fragment]
     * @param resultListener [ZFileSelectResultListener] 文件选择回调
     */
    fun start(fragmentOrActivity: Any, resultListener: ZFileSelectResultListener) {
        when (getConfiguration().filePath) {
            ZFileConfiguration.QQ -> startByQQ(fragmentOrActivity, resultListener)
            ZFileConfiguration.WECHAT -> startByWechat(fragmentOrActivity, resultListener)
            else -> startByFileManager(
                fragmentOrActivity,
                getConfiguration().filePath,
                resultListener
            )
        }
    }

    private fun startByQQ(fragmentOrActivity: Any, resultListener: ZFileSelectResultListener) {
        when (fragmentOrActivity) {
            is FragmentActivity -> {
                if (fragmentOrActivity.isDestroyed || fragmentOrActivity.isFinishing) return
                addFragment(
                    fragmentOrActivity.supportFragmentManager, fragmentOrActivity,
                    ZFileQWActivity::class.java, ZFileConfiguration.QQ, resultListener
                )
            }
            is Fragment -> {
                if (fragmentOrActivity.isRemoving || fragmentOrActivity.isDetached) return
                addFragment(
                    fragmentOrActivity.childFragmentManager, fragmentOrActivity.context!!,
                    ZFileQWActivity::class.java, ZFileConfiguration.QQ, resultListener
                )
            }
            else -> throw ZFileException(ERROR_MSG)
        }
    }

    private fun startByWechat(fragmentOrActivity: Any, resultListener: ZFileSelectResultListener) {
        when (fragmentOrActivity) {
            is FragmentActivity -> {
                if (fragmentOrActivity.isDestroyed || fragmentOrActivity.isFinishing) return
                addFragment(
                    fragmentOrActivity.supportFragmentManager, fragmentOrActivity,
                    ZFileQWActivity::class.java, ZFileConfiguration.WECHAT, resultListener
                )
            }
            is Fragment -> {
                if (fragmentOrActivity.isRemoving || fragmentOrActivity.isDetached) return
                addFragment(
                    fragmentOrActivity.childFragmentManager, fragmentOrActivity.context!!,
                    ZFileQWActivity::class.java, ZFileConfiguration.WECHAT, resultListener
                )
            }
            else -> throw ZFileException(ERROR_MSG)
        }
    }

    private fun startByFileManager(
        fragmentOrActivity: Any,
        path: String? = null,
        resultListener: ZFileSelectResultListener
    ) {
        val newPath = if (path.isNullOrEmpty()) SD_ROOT else path
        if (!newPath.toFile().exists()) {
            throw ZFileException("$newPath not exist")
        }
        when (fragmentOrActivity) {
            is FragmentActivity -> {
                if (fragmentOrActivity.isDestroyed || fragmentOrActivity.isFinishing) {
                    ZFileLog.e("FragmentActivity 已被销毁或回收，请确保调用前你的FragmentActivity状态正常！")
                    throw ZFileException("FragmentActivity isDestroyed or isFinishing！")
                }
                addFragment(
                    fragmentOrActivity.supportFragmentManager, fragmentOrActivity,
                    ZFileListActivity2::class.java, path, resultListener
                )
            }
            is Fragment -> {
                if (fragmentOrActivity.isRemoving || fragmentOrActivity.isDetached) {
                    ZFileLog.e("Fragment 已被销毁或回收，请确保调用前你的Fragment状态正常！")
                    throw ZFileException("Fragment isRemoving or isDetached！")
                }
                addFragment(
                    fragmentOrActivity.childFragmentManager, fragmentOrActivity.requireContext(),
                    ZFileListActivity2::class.java, path, resultListener
                )
            }
            else -> throw ZFileException(ERROR_MSG)
        }
    }

    private fun addFragment(
        fragmentManager: FragmentManager,
        context: Context,
        clazz: Class<*>,
        path: String?,
        resultListener: ZFileSelectResultListener
    ) {
        var fragment =
            fragmentManager.findFragmentByTag(ZFileProxyFragment.TAG) as? ZFileProxyFragment
        if (fragment == null) {
            fragment = ZFileProxyFragment()
            fragmentManager.beginTransaction().add(fragment, ZFileProxyFragment.TAG).commitNow()
        }
        fragment.jump(ZFILE_REQUEST_CODE, Intent(context, clazz).apply {
            putExtra(FILE_START_PATH_KEY, path)
        }, resultListener)
    }

}