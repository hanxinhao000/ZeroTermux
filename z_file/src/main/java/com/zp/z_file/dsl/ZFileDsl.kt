package com.zp.z_file.dsl

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.zp.z_file.common.ZFileManageHelp
import com.zp.z_file.content.ZFileBean
import com.zp.z_file.content.ZFileConfiguration
import com.zp.z_file.content.getZFileHelp
import com.zp.z_file.listener.*

/**
 * 在 [FragmentActivity] 使用 ZFileManager DSL
 * @receiver FragmentActivity
 * @param block [@kotlin.ExtensionFunctionType] Function1<ZFileDsl, Unit>
 */
fun FragmentActivity.zfile(block: ZFileDsl.() -> Unit) {
    zfile(this, block)
}

/**
 * 在 [Fragment] 使用 ZFileManager DSL
 * @receiver Fragment
 * @param block [@kotlin.ExtensionFunctionType] Function1<ZFileDsl, Unit>
 */
fun Fragment.zfile(block: ZFileDsl.() -> Unit) {
    zfile(this, block)
}

/**
 * 重置 所有配置
 * @param imageLoadReset Boolean    是否重置 [imageLoade]
 */
fun ZFileDsl.resetAll(imageLoadReset: Boolean = false) {
    resetAll(imageLoadReset)
}

/**
 * 设置 [ZFileImageListener]  配置 图片类型和视频类型的显示方式
 */
fun ZFileDsl.imageLoade(block: () -> ZFileImageListener) {
    setImageLoade(block)
}

/**
 * 设置 [ZFileLoadListener]  配置 自定义文件数据获取
 */
fun ZFileDsl.fileLoade(block: () -> ZFileLoadListener) {
    setFileLoade(block)
}

/**
 * 设置 [ZQWFileLoadListener] 配置 自定义 QQ or WeChat 文件获取
 */
fun ZFileDsl.qwLoade(block: () -> ZQWFileLoadListener) {
    setQwLoad(block)
}

/**
 * 设置 [ZFileTypeListener] 配置 自定义 文件类型
 */
fun ZFileDsl.fileType(block: () -> ZFileTypeListener) {
    setFileType(block)
}

/**
 * 设置 [ZFileOperateListener] 配置 自定义 文件操作
 */
fun ZFileDsl.fileOperate(block: () -> ZFileOperateListener) {
    setfileOperate(block)
}

/**
 * 设置 [ZFileOpenListener] 配置 自定义 打开默认支持的文件
 */
fun ZFileDsl.fileOpen(block: () -> ZFileOpenListener) {
    setFileOpen(block)
}

/**
 * 设置 [ZFileClickListener] 配置 自定义 文件点击事件
 */
fun ZFileDsl.fileClick(block: () -> ZFileClickListener) {
    setFileClick(block)
}

/**
 * 设置 [ZFileFolderBadgeHintListener] 配置 文件夹 标签/角标、说明文字 相关
 */
fun ZFileDsl.fileBadgeHint(block: () -> ZFileFolderBadgeHintListener) {
    setBadgeHint(block)
}

/**
 * 设置 [ZFileOtherListener] 配置 其他
 */
fun ZFileDsl.fileOther(block: () -> ZFileOtherListener) {
    setOther(block)
}

/**
 * 设置 [ZFileConfiguration] 文件的相关配置信息
 */
fun ZFileDsl.config(block: () -> ZFileConfiguration) {
    setConfig(block)
}

/**
 * 获取返回的数据
 */
fun ZFileDsl.result(block: MutableList<ZFileBean>?.() -> Unit) {
    startManger(block)
}

/**
 * 跳转至文件管理页面并获取文件选择后返回的数据，是 [ZFileManageHelp.start] 的扩展函数
 */
fun ZFileManageHelp.result(fragmentOrActivity: Any, block: MutableList<ZFileBean>?.() -> Unit) {
    start(fragmentOrActivity, object : ZFileSelectResultListener {
        override fun selectResult(selectList: MutableList<ZFileBean>?) {
            block.invoke(selectList)
        }
    })
}

// inner ===========================================================================================

internal fun zfile(fragmentOrActivity: Any, block: ZFileDsl.() -> Unit) {
    val zFileDsl = ZFileDsl(fragmentOrActivity)
    block.invoke(zFileDsl)
}

class ZFileDsl internal constructor(private var fragmentOrActivity: Any) {

    internal fun resetAll(imageLoadReset: Boolean = false) {
        getZFileHelp().resetAll(imageLoadReset)
    }

    internal fun setImageLoade(initBlock: () -> ZFileImageListener) {
        getZFileHelp().init(initBlock())
    }

    internal fun setFileLoade(fileLoadBlock: () -> ZFileLoadListener) {
        getZFileHelp().setFileLoadListener(fileLoadBlock())
    }

    internal fun setQwLoad(qwLoadBlock: () -> ZQWFileLoadListener) {
        getZFileHelp().setQWFileLoadListener(qwLoadBlock())
    }

    internal fun setFileType(fileTypeBlock: () -> ZFileTypeListener) {
        getZFileHelp().setFileTypeListener(fileTypeBlock())
    }

    internal fun setfileOperate(fileOperateBlock: () -> ZFileOperateListener) {
        getZFileHelp().setFileOperateListener(fileOperateBlock())
    }

    internal fun setFileOpen(fileOpenBlock: () -> ZFileOpenListener) {
        getZFileHelp().setFileOpenListener(fileOpenBlock())
    }

    internal fun setFileClick(fileClickBlock: () -> ZFileClickListener) {
        getZFileHelp().setFileClickListener(fileClickBlock())
    }

    internal fun setBadgeHint(hintBlock: () -> ZFileFolderBadgeHintListener) {
        getZFileHelp().setFileBadgeHintListener(hintBlock())
    }

    internal fun setOther(otherBlock: () -> ZFileOtherListener) {
        getZFileHelp().setOtherFileListener(otherBlock())
    }

    internal fun setConfig(configBlock: () -> ZFileConfiguration) {
        getZFileHelp().setConfiguration(configBlock())
    }

    internal fun startManger(startBlock: MutableList<ZFileBean>?.() -> Unit) {
        getZFileHelp().result(fragmentOrActivity, startBlock)
    }

}

