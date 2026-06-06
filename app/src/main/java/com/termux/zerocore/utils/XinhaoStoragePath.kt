package com.termux.zerocore.utils

import android.content.Context
import android.os.Environment
import com.example.xh_lib.utils.UUtils
import com.termux.R
import com.termux.zerocore.ftp.utils.UserSetManage
import com.termux.zerocore.url.FileUrl
import com.termux.shared.termux.TermuxConstants
import java.io.File

/**
 * 统一管理 ZeroTermux 内部存储路径。
 *
 * 根据用户选择的存储模式返回对应目录：
 * - 内部存储：/storage/emulated/0/xinhao/...
 * - Android/data：/Android/data/com.termux/files/xinhao/...
 */
object XinhaoStoragePath {

    private const val CONFIG_DIR_NAME = "config"
    private const val LOG_DIR_NAME = "ZeroTermuxLog"

    @JvmStatic
    fun isAndroidDataMode(): Boolean {
        return UserSetManage.get().getZTUserBean().isCreateFolderForSdcardAndroid
    }

    /** 当前模式下的 xinhao 根目录（File）。 */
    @JvmStatic
    @JvmOverloads
    fun getRoot(context: Context = UUtils.getContext()): File {
        return getChild(context, FileUrl.MAIN_XINHAO_PATH)
    }

    /** 根据相对路径（如 /xinhao/data）解析为当前模式下的绝对路径。 */
    @JvmStatic
    fun getChild(context: Context, relativePath: String): File {
        val baseDir = if (isAndroidDataMode()) {
            context.getExternalFilesDir(null)
                ?: File(context.filesDir, FileUrl.MAIN_XINHAO_PATH.trimStart('/'))
        } else {
            Environment.getExternalStorageDirectory()
        }
        return File(baseDir, relativePath)
    }

    @JvmStatic @JvmOverloads
    fun getDataDir(context: Context = UUtils.getContext()) =
        getChild(context, FileUrl.MAIN_XINHAO_DATA_PATH)

    @JvmStatic @JvmOverloads
    fun getApkDir(context: Context = UUtils.getContext()) =
        getChild(context, FileUrl.MAIN_XINHAO_APK_PATH)

    @JvmStatic @JvmOverloads
    fun getWindowsDir(context: Context = UUtils.getContext()) =
        getChild(context, FileUrl.MAIN_XINHAO_WINDOWS_PATH)

    @JvmStatic @JvmOverloads
    fun getWindowsConfigDir(context: Context = UUtils.getContext()) =
        getChild(context, FileUrl.MAIN_XINHAO_WINDOWS_CONFIG_PATH)

    @JvmStatic
    fun getWindowsFile(context: Context, fileName: String): File {
        return File(getWindowsDir(context), fileName)
    }

    @JvmStatic @JvmOverloads
    fun getCommandDir(context: Context = UUtils.getContext()) =
        getChild(context, FileUrl.MAIN_XINHAO_COMMAND_PATH)

    @JvmStatic @JvmOverloads
    fun getFontDir(context: Context = UUtils.getContext()) =
        getChild(context, FileUrl.MAIN_XINHAO_FONT_PATH)

    @JvmStatic @JvmOverloads
    fun getIsoDir(context: Context = UUtils.getContext()) =
        getChild(context, FileUrl.MAIN_XINHAO_ISO_PATH)

    @JvmStatic @JvmOverloads
    fun getMysqlDir(context: Context = UUtils.getContext()) =
        getChild(context, FileUrl.MAIN_XINHAO_MYSQL_PATH)

    @JvmStatic @JvmOverloads
    fun getOnlineSystemDir(context: Context = UUtils.getContext()) =
        getChild(context, FileUrl.MAIN_XINHAO_ONLINE_SYSTEM_PATH)

    @JvmStatic @JvmOverloads
    fun getQemuDir(context: Context = UUtils.getContext()) =
        getChild(context, FileUrl.MAIN_XINHAO_QEMU_PATH)

    @JvmStatic @JvmOverloads
    fun getServerDir(context: Context = UUtils.getContext()) =
        getChild(context, FileUrl.MAIN_XINHAO_SERVER_PATH)

    @JvmStatic @JvmOverloads
    fun getShareDir(context: Context = UUtils.getContext()) =
        getChild(context, FileUrl.MAIN_XINHAO_SHARE_PATH)

    @JvmStatic @JvmOverloads
    fun getSystemDir(context: Context = UUtils.getContext()) =
        getChild(context, FileUrl.MAIN_XINHAO_SYSTEM_PATH)

    @JvmStatic @JvmOverloads
    fun getWebConfigDir(context: Context = UUtils.getContext()) =
        getChild(context, FileUrl.MAIN_XINHAO_WEB_CONFIG_PATH)

    @JvmStatic @JvmOverloads
    fun getModuleDir(context: Context = UUtils.getContext()) =
        getChild(context, FileUrl.MAIN_XINHAO_MODULE_PATH)

    @JvmStatic @JvmOverloads
    fun getTypeMarkerDir(context: Context = UUtils.getContext()): File {
        return if (isAndroidDataMode()) {
            getChild(context, FileUrl.MAIN_XINHAO_TYPE_ANDROID_PATH)
        } else {
            getChild(context, FileUrl.MAIN_XINHAO_TYPE_PATH)
        }
    }

    @JvmStatic @JvmOverloads
    fun getConfigDir(context: Context = UUtils.getContext()) =
        File(getRoot(context), CONFIG_DIR_NAME)

    @JvmStatic @JvmOverloads
    fun getLogDir(context: Context = UUtils.getContext()) =
        File(getRoot(context), LOG_DIR_NAME)

    /** 内部存储模式下需要创建的全部目录。 */
    @JvmStatic
    fun getSdcardModeFolders(context: Context = UUtils.getContext()): List<File> {
        return listOf(
            getRoot(context),
            getDataDir(context),
            getApkDir(context),
            getWindowsDir(context),
            getCommandDir(context),
            getFontDir(context),
            getIsoDir(context),
            getMysqlDir(context),
            getOnlineSystemDir(context),
            getQemuDir(context),
            getServerDir(context),
            getShareDir(context),
            getSystemDir(context),
            getWebConfigDir(context),
            getModuleDir(context),
            getWindowsConfigDir(context),
            getChild(context, FileUrl.MAIN_XINHAO_TYPE_PATH)
        )
    }

    /** Android/data 模式下需要创建的子目录（相对 /xinhao）。 */
    @JvmStatic
    fun getAndroidDataSubfolderPaths(): List<String> {
        return listOf(
            FileUrl.MAIN_XINHAO_DATA_PATH,
            FileUrl.MAIN_XINHAO_APK_PATH,
            FileUrl.MAIN_XINHAO_WINDOWS_PATH,
            FileUrl.MAIN_XINHAO_COMMAND_PATH,
            FileUrl.MAIN_XINHAO_FONT_PATH,
            FileUrl.MAIN_XINHAO_ISO_PATH,
            FileUrl.MAIN_XINHAO_MYSQL_PATH,
            FileUrl.MAIN_XINHAO_ONLINE_SYSTEM_PATH,
            FileUrl.MAIN_XINHAO_QEMU_PATH,
            FileUrl.MAIN_XINHAO_SERVER_PATH,
            FileUrl.MAIN_XINHAO_SHARE_PATH,
            FileUrl.MAIN_XINHAO_SYSTEM_PATH,
            FileUrl.MAIN_XINHAO_WEB_CONFIG_PATH,
            FileUrl.MAIN_XINHAO_MODULE_PATH,
            FileUrl.MAIN_XINHAO_WINDOWS_CONFIG_PATH,
            FileUrl.MAIN_XINHAO_TYPE_ANDROID_PATH
        )
    }

    @JvmStatic
    fun ensureExists(file: File): Boolean {
        return file.exists() || file.mkdirs()
    }

    @JvmStatic
    fun createSdcardModeFolders(context: Context = UUtils.getContext()): Boolean {
        return getSdcardModeFolders(context).all { ensureExists(it) }
    }

    @JvmStatic
    fun createAndroidDataModeFolders(context: Context): Boolean {
        val home = context.getExternalFilesDir(null) ?: return false
        if (!home.exists() && !home.mkdirs()) {
            return false
        }
        return getAndroidDataSubfolderPaths().all { path ->
            ensureExists(File(home, path))
        }
    }

    /** Termux 终端内相对路径：./storage/shared/xinhao 或 android 变体。 */
    @JvmStatic
    fun getTerminalSharedBase(): String {
        return if (isAndroidDataMode()) {
            "./storage/shared/Android/data/com.termux/files/xinhao"
        } else {
            "./storage/shared/xinhao"
        }
    }

    @JvmStatic
    fun getTerminalDataPath(): String = "${getTerminalSharedBase()}/data"

    @JvmStatic
    fun getTerminalWindowsShellPath(fileName: String): String {
        return "${getTerminalSharedBase()}/windows/$fileName"
    }

    /** Termux 内 QEMU 等使用的绝对路径。 */
    @JvmStatic
    fun getTerminalWindowsAbsoluteDir(): String {
        val sharedBase = "${TermuxConstants.TERMUX_FILES_DIR_PATH}/home/storage/shared"
        val xinhaoBase = if (isAndroidDataMode()) {
            "$sharedBase/Android/data/com.termux/files/xinhao"
        } else {
            "$sharedBase/xinhao"
        }
        return "$xinhaoBase/windows"
    }

    @JvmStatic
    fun getTerminalWindowsAbsolutePath(fileName: String): String {
        return "${getTerminalWindowsAbsoluteDir()}/$fileName"
    }

    @JvmStatic
    fun getShellBackup(systemName: String, tarOption: String): String {
        val successRes = if (isAndroidDataMode()) {
            R.string.backup_success_android
        } else {
            R.string.backup_success
        }
        return "cd ~ && cd ~ && tar -$tarOption ${getTerminalDataPath()}/$systemName " +
            "/data/data/com.termux/files && echo \"${UUtils.getString(successRes)}\" \n"
    }

    @JvmStatic
    fun getShellRestore(command: String, archiveName: String, containerDirName: String): String {
        val archive = archiveName.replace(" ", "")
        return "cd ~ && cd ~ && tar -v -$command ${getTerminalDataPath()}/$archive " +
            "-C ../../$containerDirName && mv ../../$containerDirName/data/data/com.termux/files/home " +
            "../../$containerDirName && mv ../../$containerDirName/data/data/com.termux/files/usr " +
            "../../$containerDirName && rm -rf ../../$containerDirName/data && " +
            "echo \"${UUtils.getString(R.string.system_restore_success)}\" \n"
    }

    @JvmStatic
    fun getModuleEmptyMessageRes(): Int {
        return if (isAndroidDataMode()) {
            R.string.install_empty_android
        } else {
            R.string.install_empty
        }
    }
}
