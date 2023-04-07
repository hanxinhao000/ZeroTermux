package com.zp.z_file.listener

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Environment
import android.system.Os
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.collection.ArrayMap
import androidx.fragment.app.FragmentActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.zp.z_file.R
import com.zp.z_file.bean.DataBean
import com.zp.z_file.common.ZFileType
import com.zp.z_file.content.*
import com.zp.z_file.type.*
import com.zp.z_file.ui.ZFileListFragment
import com.zp.z_file.ui.ZFilePicActivity
import com.zp.z_file.ui.ZFileVideoPlayActivity
import com.zp.z_file.ui.dialog.*
import com.zp.z_file.util.*
import com.zp.z_file.zerotermux.CallBackListener
import com.zp.z_file.zerotermux.Z7Listener
import com.zp.z_file.zerotermux.ZTConfig
import java.io.File
import java.io.IOException

/*
本库内置丰富的api、内置丰富的配置属性，足以胜任开发者的个性化需求！
极高自定义（文件获取、文件操作、文件类型扩展、UI展示、主题、提示语句等），简单配置即可满足需求
 */

/**
 * 图片或视频 显示
 */
abstract class ZFileImageListener {

    /**
     * 图片类型加载
     */
    abstract fun loadImage(imageView: ImageView, file: File)

    /**
     * 视频类型加载
     */
    open fun loadVideo(imageView: ImageView, file: File) {
        loadImage(imageView, file)
    }
}

/**
 * 文件选取 后 的监听
 */
interface ZFileSelectResultListener {

    fun selectResult(selectList: MutableList<ZFileBean>?)

}

/**
 * 完全自定义 获取文件数据
 */
interface ZFileLoadListener {

    /**
     * 获取手机里的文件List
     * @param filePath String           指定的文件目录访问，空为SD卡根目录
     * @return MutableList<ZFileBean>?  list
     */
    fun getFileList(context: Context?, filePath: String?): MutableList<ZFileBean>?
}

/**
 * 嵌套在 Fragment 中 使用
 * [FragmentActivity] 中 对于 [ZFileListFragment] 操作
 */
abstract class ZFragmentListener {

    /**
     * 文件选择
     */
    abstract fun selectResult(selectList: MutableList<ZFileBean>?)

    /**
     * [Activity] 中直接调用 [Activity.finish] 即可，如有需要，重写即可
     */
    open fun onActivityBackPressed(activity: FragmentActivity) {
        activity.finish()
    }

    /**
     * 获取 [Manifest.permission.WRITE_EXTERNAL_STORAGE] 权限失败
     * @param activity [FragmentActivity]
     */
    open fun onSDPermissionsFiled(activity: FragmentActivity) {
        activity.toast(activity getStringById R.string.zfile_permission_bad)
    }

    /**
     * 获取 [Environment.isExternalStorageManager] (所有的文件管理) 权限 失败
     * 请注意：Android 11 及以上版本 才有
     */
    open fun onExternalStorageManagerFiled(activity: FragmentActivity) {
        activity.toast(activity getStringById R.string.zfile_11_bad)
    }
}

/**
 * 完全自定义 QQ、WeChat 获取
 */
abstract class ZQWFileLoadListener {

    /**
     * 获取标题
     * @return Array<String>
     */
    open fun getTitles(): Array<String>? = null

    /**
     * 获取过滤规则
     * @param fileType Int      文件类型 see [ZFILE_QW_PIC]、[ZFILE_QW_MEDIA]、[ZFILE_QW_DOCUMENT]、[ZFILE_QW_OTHER]
     */
    abstract fun getFilterArray(fileType: Int): Array<String>

    /**
     * 获取 QQ 或 WeChat 文件路径
     * @param qwType String         QQ 或 WeChat  see [ZFileConfiguration.QQ]、[ZFileConfiguration.WECHAT]
     * @param fileType Int          文件类型 see [ZFILE_QW_PIC]、[ZFILE_QW_MEDIA]、[ZFILE_QW_DOCUMENT]、[ZFILE_QW_OTHER]
     * @return MutableList<String>  文件路径集合（因为QQ或WeChat保存的文件可能存在多个路径）
     */
    abstract fun getQWFilePathArray(qwType: String, fileType: Int): MutableList<String>

    /**
     * 获取数据
     * @param fileType Int                          文件类型 see [ZFILE_QW_PIC]、[ZFILE_QW_MEDIA]、[ZFILE_QW_DOCUMENT]、[ZFILE_QW_OTHER]
     * @param qwFilePathArray MutableList<String>   QQ 或 WeChat 文件路径集合
     * @param filterArray Array<String>             过滤规则
     */
    abstract fun getQWFileDatas(fileType: Int, qwFilePathArray: MutableList<String>, filterArray: Array<String>): MutableList<ZFileBean>

}

/**
 * 文件类型
 */
open class ZFileTypeListener {

    open fun getFileType(filePath: String): ZFileType {
        return when (ZFileHelp.getFileTypeBySuffix(filePath)) {
            PNG, JPG, JPEG, GIF -> ZFileImageType()
            MP3, AAC, WAV, M4A -> ZFileAudioType()
            MP4, _3GP -> ZFileVideoType()
            TXT, XML, JSON, EMPTY, SH-> ZFileTxtType()
            ZIP -> ZFileZipType()
            DOC, DOCX -> ZFileWordType()
            XLS, XLSX -> ZFileXlsType()
            PPT, PPTX -> ZFilePptType()
            PDF -> ZFilePdfType()
            TAGGZ, TAGXZ, TAGBZ2 -> ZFileTarGzType()
            Z7 -> ZFile7ZType()
            DEB -> ZFileDEBType()
            else -> ZFileOtherType()
        }
    }
}

/**
 * 打开文件
 */
open class ZFileOpenListener {

    private val textArrays by lazy {
        arrayOf(
            "编辑(Edit)",
            "使用VIM打开(open with vim)",
            "在终端运行(run in terminal)",
            "设置此文件可执行权限(Set executable permissions for this file)",
            "使用其它应用程序打开(Open with another application)",
        )
    }
    private val otherArrays by lazy {
        arrayOf(
            "编辑(Edit)",
            "使用VIM打开(open with vim)",
            "尝试在终端运行(run in terminal try)",
            "设置此文件可执行权限(Set executable permissions for this file)",
            "使用其它应用程序打开(Open with another application)",
        )
    }
    /**
     * 打开音频
     * @param filePath String   文件路径
     * @param view View         RecyclerView itemView
     */
    open fun openAudio(filePath: String, view: View) {
        (view.context as? AppCompatActivity)?.apply {
            val tag = "ZFileAudioPlayDialog"
            checkFragmentByTag(tag)
            ZFileAudioPlayDialog.getInstance(filePath).show(supportFragmentManager, tag)
        }
    }

    /**
     * 打开图片，如果只需要自定义视图，请实现 [ZFileOtherListener.getImgInfoView] 即可
     * @param filePath String   文件路径
     * @param view View         RecyclerView itemView
     */
    open fun openImage(filePath: String, view: View) {
        (view.context as? Activity)?.let {
            ZFilePicActivity.show(it, filePath)
        }
    }

    /**
     * 打开视频
     * @param filePath String   文件路径
     * @param view View         RecyclerView itemView
     */
    open fun openVideo(filePath: String, view: View) {
        (view.context as? Activity)?.let {
            ZFileVideoPlayActivity.show(it, filePath)
        }
    }

    /**
     * 打开Txt
     * @param filePath String   文件路径
     * @param view View         RecyclerView itemView
     */
    open fun openTXT(filePath: String, view: View) {
        view.context?.let {
            AlertDialog.Builder(it).apply {
                setItems(textArrays) { dialog, which ->
                    when (which) {
                        0 -> {
                            val apply = Intent().apply {
                                action = "com.termux.zerocore.activity.edittextactivity"
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                putExtra("edit_path", filePath)
                            }
                            view.context!!.startActivity(apply)
                        }
                        1 -> {
                            LocalBroadcastManager.getInstance(view.context).apply {
                                val intent = Intent()
                                intent.action = "localbroadcast"
                                val sendText = "pkg install vim -y && vim $filePath"
                                intent.putExtra("broadcastString", sendText)
                                sendBroadcast(intent)
                            }
                        }
                        2 -> {
                            LocalBroadcastManager.getInstance(view.context).apply {
                                val intent = Intent()
                                intent.action = "localbroadcast"
                                val sendText = "bash $filePath"
                                intent.putExtra("broadcastString", sendText)
                                sendBroadcast(intent)
                            }
                        }
                        3 -> {
                            LocalBroadcastManager.getInstance(view.context).apply {
                                val intent = Intent()
                                intent.action = "localbroadcast"
                                val file = File(filePath)
                                val sendText = "cd ${file.parentFile?.absolutePath} && chmod 777 ${file.name}"
                                intent.putExtra("broadcastString", sendText)
                                sendBroadcast(intent)
                            }
                        }
                        4 -> {
                            ZFileOpenUtil.openOtherFile(filePath, "text/plain", view)
                        }
                    }
                    dialog.dismiss()
                }

            }.show()
        }
    }

    /**
     * 打开zip
     * @param filePath String   文件路径
     * @param view View         RecyclerView itemView
     */
    open fun openZIP(filePath: String, view: View) {
        view.context?.let {
            AlertDialog.Builder(it).apply {
                setTitle("请选择(select)")
                setItems(arrayOf("打开(open)", "解压(decompress)")) { dialog, which ->
                    if (which == 0) {
                        ZFileOpenUtil.openZIP(filePath, view)
                    } else {
                        zipSelect(filePath, it)
                    }
                    dialog.dismiss()
                }
                setPositiveButton("取消(cancel)") { dialog, _ -> dialog.dismiss() }
                show()
            }
        }
    }

    open fun openTar(filePath: String, view: View) {
        view.context?.let {
            AlertDialog.Builder(it).apply {
                setTitle("请选择(select)")
                setItems(arrayOf("安装此恢复包(Install this recovery package)", "解压(decompress)")) { dialog, which ->
                    if (which == 0) {
                        InstallTarData.installTar(view.context!!, filePath)
                    } else if (which == 1) {
                        tarSelect(filePath, it)
                    }
                    dialog.dismiss()
                }
                setPositiveButton("取消") { dialog, _ -> dialog.dismiss() }
                show()
            }
        }
    }

    open fun openDeb(filePath: String, view: View) {
        view.context?.let {
            AlertDialog.Builder(it).apply {
                setTitle("请选择(select)")
                setItems(arrayOf("安装(Install)", "解压(decompress)")) { dialog, which ->
                    if (which == 0) {
                        LocalBroadcastManager.getInstance(view.context).apply {
                            val intent = Intent()
                            intent.action = "localbroadcast"
                            val file = File(filePath)
                            val sendText = "cd ${file.parentFile?.absolutePath} && dpkg -i ${file.name}"
                            intent.putExtra("broadcastString", sendText)
                            sendBroadcast(intent)
                        }
                    } else if (which == 1) {
                        debSelect(filePath, view.context)
                    }
                    dialog.dismiss()
                }
                setPositiveButton("取消") { dialog, _ -> dialog.dismiss() }
                show()
            }
        }
    }

    open fun open7Z(filePath: String, view: View) {
        view.context?.let {
            AlertDialog.Builder(it).apply {
                setTitle("请选择(select)")
                setItems(arrayOf("安装此模块包(Install this modpack)", "解压(decompress)")) { dialog, which ->
                    if (which == 0) {
                        val switchDialog = SwitchDialog(view.context)
                        switchDialog.createSwitchDialog(ZFileUUtils.getString(R.string.install_module_switch))
                        switchDialog.show()
                        switchDialog.cancel?.setOnClickListener {
                            switchDialog.dismiss()

                        }
                        switchDialog.ok?.setOnClickListener {
                            switchDialog.dismiss()
                            val installModuleDialog = InstallModuleDialog(view.context)
                            installModuleDialog.show()
                            installModuleDialog.setCancelable(false)
                            val dataBean = DataBean()
                            dataBean.mFile = File(filePath)
                            installModuleDialog.installModule(dataBean)
                        }

                    } else if (which == 1){
                        z7Select(filePath, it)
                    }
                    dialog.dismiss()
                }
                setPositiveButton("取消") { dialog, _ -> dialog.dismiss() }
                show()
            }
        }
    }

    private fun debSelect(filePath: String, context: Context) {
        Log.e(TAG, "tarSelect: filePath:" + filePath )
        if (context is AppCompatActivity) {
            context.checkFragmentByTag("ZFileSelectFolderDialog")
            val dialog = ZFileSelectFolderDialog.newInstance("解压")
            dialog.selectFolder = {
                val targetFile = this
                LocalBroadcastManager.getInstance(context).apply {
                    val intent = Intent()
                    intent.action = "localbroadcast"
                    val file = File(filePath)
                    val sendText = "cd ${file.parentFile?.absolutePath} && mkdir $targetFile/${file.name}_folder && dpkg -X ${file.name} $targetFile/${file.name}_folder"
                    intent.putExtra("broadcastString", sendText)
                    sendBroadcast(intent)
                }
            }
            dialog.show(context.supportFragmentManager, "ZFileSelectFolderDialog")
        } else {
            ZFileLog.e("文件解压 showDialog 失败")
        }
    }


    private fun z7Select(filePath: String, context: Context) {
        Log.e(TAG, "tarSelect: filePath:" + filePath )
        if (context is AppCompatActivity) {
            context.checkFragmentByTag("ZFileSelectFolderDialog")
            val dialog = ZFileSelectFolderDialog.newInstance("解压")
            dialog.selectFolder = {
                val targetFile = this
                val loadingDialog = LoadingDialog(context)
                loadingDialog.show()
                ZTConfig.setZ7Listener(object: Z7Listener{
                    override fun decompress(text: String, runs: Boolean, error: Boolean) {
                        loadingDialog.msg?.text = text
                        if (error) {
                            loadingDialog.dismiss()
                            ZFileUUtils.showMsg(text)
                            return
                        }
                        if (runs) {
                            loadingDialog.dismiss()
                            ZFileUUtils.showMsg("解压完成\nSuccessfullyDecompressed")
                        }

                    }

                })
                LocalBroadcastManager.getInstance(context).apply {
                    val intent = Intent()
                    intent.action = "localbroadcast"
                    val sendText = "$filePath,$targetFile"
                    intent.putExtra("broadcastString7Z", sendText)
                    sendBroadcast(intent)
                }
               // LoadingDialog
            }
            dialog.show(context.supportFragmentManager, "ZFileSelectFolderDialog")
        } else {
            ZFileLog.e("文件解压 showDialog 失败")
        }
    }

    private fun tarSelect(filePath: String, context: Context) {
        Log.e(TAG, "tarSelect: filePath:" + filePath )
        if (context is AppCompatActivity) {
            context.checkFragmentByTag("Z7ZFileSelectFolderDialog")
            val dialog = ZFileSelectFolderDialog.newInstance("解压")
            dialog.selectFolder = {
                val targetFile = this
                ZTConfig.setCallBackListener(object: CallBackListener {
                    override fun call() {
                        val fragment =
                            context.supportFragmentManager.findFragmentByTag(getZFileConfig().fragmentTag)
                        if (fragment is ZFileListFragment) {
                            fragment.observer(true)
                        } else {
                            ZFileLog.e("文件解压成功，但是无法立刻刷新界面！")
                        }
                    }

                })
                LocalBroadcastManager.getInstance(context).apply {
                    val intent = Intent()
                    intent.action = "localbroadcast"
                    val sendText = "$filePath,$targetFile"
                    intent.putExtra("broadcastStringTar", sendText)
                    sendBroadcast(intent)
                }
            }
            dialog.show(context.supportFragmentManager, "Z7ZFileSelectFolderDialog")
        } else {
            ZFileLog.e("文件解压 showDialog 失败")
        }
    }

    private fun zipSelect(filePath: String, context: Context) {
        if (context is AppCompatActivity) {
            context.checkFragmentByTag("ZFileSelectFolderDialog")
            val dialog = ZFileSelectFolderDialog.newInstance("解压")
            dialog.selectFolder = {
                getZFileHelp().getFileOperateListener().zipFile(filePath, this, context) {
                    ZFileLog.i(if (this) "解压成功" else "解压失败")
                    val fragment =
                        context.supportFragmentManager.findFragmentByTag(getZFileConfig().fragmentTag)
                    if (fragment is ZFileListFragment) {
                        fragment.observer(this)
                    } else {
                        ZFileLog.e("文件解压成功，但是无法立刻刷新界面！")
                    }
                }
            }
            dialog.show(context.supportFragmentManager, "ZFileSelectFolderDialog")
        } else {
            ZFileLog.e("文件解压 showDialog 失败")
        }
    }

    /**
     * 打开word
     * @param filePath String   文件路径
     * @param view View         RecyclerView itemView
     */
    open fun openDOC(filePath: String, view: View) {
        ZFileOpenUtil.openDOC(filePath, view)
    }

    /**
     * 打开xls
     * @param filePath String   文件路径
     * @param view View         RecyclerView itemView
     */
    open fun openXLS(filePath: String, view: View) {
        ZFileOpenUtil.openXLS(filePath, view)
    }

    /**
     * 打开PPT
     * @param filePath String   文件路径
     * @param view View         RecyclerView itemView
     */
    open fun openPPT(filePath: String, view: View) {
        ZFileOpenUtil.openPPT(filePath, view)
    }

    /**
     * 打开PDF
     * @param filePath String   文件路径
     * @param view View         RecyclerView itemView
     */
    open fun openPDF(filePath: String, view: View) {
        ZFileOpenUtil.openPDF(filePath, view)
    }

    /**
     * 打开其他文件类型
     * @param filePath String   文件路径
     * @param view View         RecyclerView itemView
     */
    open fun openOther(filePath: String, view: View) {

        view.context?.let {
            AlertDialog.Builder(it).apply {
                setItems(otherArrays) { dialog, which ->
                    when (which) {
                        0 -> {
                            val apply = Intent().apply {
                                action = "com.termux.zerocore.activity.edittextactivity"
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                putExtra("edit_path", filePath)
                            }
                            view.context!!.startActivity(apply)
                        }
                        1 -> {
                            LocalBroadcastManager.getInstance(view.context).apply {
                                val intent = Intent()
                                intent.action = "localbroadcast"
                                val sendText = "pkg install vim -y && vim $filePath"
                                intent.putExtra("broadcastString", sendText)
                                sendBroadcast(intent)
                            }
                        }
                        2 -> {
                            LocalBroadcastManager.getInstance(view.context).apply {
                                val intent = Intent()
                                intent.action = "localbroadcast"
                                val file = File(filePath)
                                val sendText = "cd ${file.parentFile?.absolutePath} && ./${file.name}"
                                intent.putExtra("broadcastString", sendText)
                                sendBroadcast(intent)
                            }
                        }
                        3 -> {
                            LocalBroadcastManager.getInstance(view.context).apply {
                                val intent = Intent()
                                intent.action = "localbroadcast"
                                val file = File(filePath)
                                val sendText = "cd ${file.parentFile?.absolutePath} && chmod 777 ${file.name}"
                                intent.putExtra("broadcastString", sendText)
                                sendBroadcast(intent)
                            }
                        }
                        4 -> {
                            ZFileOpenUtil.openOtherFile(filePath, "text/plain", view)
                        }
                    }
                    dialog.dismiss()
                }

            }.show()
        }
    }
}

/**
 * 点击事件
 */
open class ZFileClickListener {

    /**
     * 文件 点击
     * @param fileBean ZFileBean    文件实体
     * @param view View             RecyclerView itemView
     */
    open fun itemFileClick(fileBean: ZFileBean, view: View) = Unit

    /**
     * 文件夹 点击
     * @param fileBean ZFileBean    文件实体
     * @param view View             RecyclerView itemView
     */
    open fun itemFoldClick(fileBean: ZFileBean, view: View) = Unit

    /**
     * 未选中数据时Toolbar完成 点击
     */
    open fun emptyDataDownClick() = Unit

    /**
     * 重新申请 权限 按钮 点击
     * @param view View             Button
     */
    open fun permissionBtnApplyClick(view: View) = Unit

}

/**
 * 文件操作（默认不支持对于文件夹的操作，如果需要对于文件夹的操作，请重写该类的所有方法）！
 * 耗时的文件操作建议放在 非 UI线程中
 */
open class ZFileOperateListener {

    /**
     * 文件重命名（该方式需要先弹出重命名弹窗或其他页面，再执行重命名逻辑）
     * @param filePath String   文件路径
     * @param context Context   Context
     * @param block Function2<Boolean, String, Unit> Boolean：成功或失败；String：新名字
     */
    open fun renameFile(
        filePath: String,
        context: Context,
        block: (Boolean, String) -> Unit
    ) {
        (context as? AppCompatActivity)?.let {
            it.checkFragmentByTag("ZFileRenameDialog")
            ZFileRenameDialog.newInstance(filePath.getFileNameOnly()).apply {
                reanameDown = {
                    renameFile(filePath, this, context, block)
                }
            }.show(it.supportFragmentManager, "ZFileRenameDialog")
        }
    }

    /**
     * 文件重命名（该方式只需要实现重命名逻辑即可）
     * @param filePath String       文件路径
     * @param fileNewName String    新名字
     * @param context Context       Context
     * @param block Function2<Boolean, String, Unit> Boolean：成功或失败；String：新名字
     */
    open fun renameFile(
        filePath: String,
        fileNewName: String,
        context: Context,
        block: (Boolean, String) -> Unit
    ) {
        ZFileUtil.renameFile(filePath, fileNewName, context, block)
    }

    /**
     * 复制文件
     * @param sourceFile String     源文件地址
     * @param targetFile String     目标文件地址
     * @param context Context       Context
     */
    open fun copyFile(
        sourceFile: String,
        targetFile: String,
        context: Context,
        block: Boolean.() -> Unit
    ) {
        ZFileUtil.copyFile(sourceFile, targetFile, context, block)
    }

    /**
     * 移动文件
     * @param sourceFile String     源文件地址
     * @param targetFile String     目标文件地址
     * @param context Context       Context
     */
    open fun moveFile(
        sourceFile: String,
        targetFile: String,
        context: Context,
        block: Boolean.() -> Unit
    ) {
        ZFileUtil.cutFile(sourceFile, targetFile, context, block)
    }

    /**
     * 删除文件
     * @param filePath String   源文件地址
     */
    open fun deleteFile(filePath: String, context: Context, block: Boolean.() -> Unit) {
        AlertDialog.Builder(context).apply {
            setTitle("注意[Notice]")
            setMessage("请谨慎执行此操作,目录的软连接删除可能会清除你手机存储所有的文件(软连接文件夹右下角会显示上下箭头的图标)!\nPlease perform this operation carefully, the soft link deletion of the directory may clear all the files stored on your phone (the icon of up and down arrows will be displayed in the lower right corner of the soft link folder)")
            setPositiveButton("删除[delete]") { _, _ ->
                ZFileUtil.deleteFile(filePath, context, block)
            }
            setNegativeButton("取消[no]") { dialog, _ ->
                dialog.dismiss()
            }
            show()
        }
    }

    /**
     * 解压文件
     * @param sourceFile String     源文件地址
     * @param targetFile String     目标文件地址
     */
    open fun zipFile(
        sourceFile: String,
        targetFile: String,
        context: Context,
        block: Boolean.() -> Unit
    ) {
        ZFileUtil.zipFile(sourceFile, targetFile, context, block)
    }

    /**
     * 文件详情
     */
    open fun fileInfo(bean: ZFileBean, context: Context) {
        val tag = ZFileInfoDialog::class.java.simpleName
        (context as? AppCompatActivity)?.let {
            it.checkFragmentByTag(tag)
            ZFileInfoDialog.newInstance(bean).show(it.supportFragmentManager, tag)
        }

    }
}

/**
 * 文件夹 标签/角标、说明文字 相关
 */
open class ZFileFolderBadgeHintListener {

    /**
     * 说明文字大小
     */
    open fun hintTextSize(): Float {
        return 12f
    }

    /**
     * 说明文字颜色
     */
    open fun hintTextColor(): Int {
        return R.color.zfile_bbbbb9
    }

    /**
     * 匹配方式
     * true：等价于 equals，在[doingWork] 中 ArrayMap key 值必须为文件夹全路径：如 /storage/emulated/0/DICM
     * false：等价于 indexOf，在[doingWork] 中 ArrayMap key 值 只需要是文件夹名称：如 DCIM/Camera
     */
    open fun isEquals(): Boolean {
        return true
    }

    /**
     * 具体的配置 信息
     */
    open fun doingWork(context: Context): ArrayMap<String, ZFileFolderBadgeHintBean>? {
        return ZFileFBHUtil.doingWork(context)
    }
}

/**
 * 其他操作相关
 */
open class ZFileOtherListener {

    protected val MATCH_PARENT = FrameLayout.LayoutParams.MATCH_PARENT
    protected val WRAP_CONTENT = FrameLayout.LayoutParams.WRAP_CONTENT

    /**
     * 耗时的文件操作（如复制、移动文件等） 展示的 Dialog
     * @param context Context   Context
     * @param title String?     标题
     */
    open fun getLoadingDialog(
        context: Context,
        title: String? = context getStringById R.string.zfile_loading
    ): Dialog {
        return ZFileLoadingDialog(context, title)
    }

    /**
     * 获取 权限失败 时的 布局
     * 请注意：布局中必须包含控件 id：zfile_permission_againBtn
     * 该id对应视图功能：用户点击后再次申请权限
     */
    open fun getPermissionFailedLayoutId(): Int {
        return ZFILE_DEFAULT
    }

    /**
     * 获取 当前目录没有文件时（为空） 的布局
     */
    open fun getFileListEmptyLayoutId(): Int {
        return ZFILE_DEFAULT
    }

    /**
     * 获取 查看图片 展示 View（LayoutParams 为 FrameLayout.LayoutParams）
     * @param context Context   Context
     * @param imgPath String    图片路径
     * @return View?     空表示使用默认值
     */
    open fun getImgInfoView(context: Context, imgPath: String): View? {
        return null
    }

}
