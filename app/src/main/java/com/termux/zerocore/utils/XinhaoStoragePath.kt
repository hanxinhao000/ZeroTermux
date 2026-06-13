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

    /**
     * 菜单 zip 备份目录 & 安装时 zip 选择来源。
     * 仅跟随当前「设置存储路径」：/sdcard/xinhao/menu 或 Android/data/.../xinhao/menu。
     * 两种存储路径下的 menu 互不影响，不会互相同步。
     */
    @JvmStatic @JvmOverloads
    fun getMenuBackupDir(context: Context = UUtils.getContext()): File {
        val dir = File(getRoot(context), "menu")
        try {
            ensureExists(dir)
        } catch (e: Exception) {
            android.util.Log.e("XinhaoStoragePath", "Failed to create menu backup dir: ${dir.absolutePath}", e)
        }
        return dir
    }

    /** @see getMenuBackupDir */
    @JvmStatic @JvmOverloads
    fun getMenuDir(context: Context = UUtils.getContext()): File = getMenuBackupDir(context)

    /**
     * 已安装菜单包（解压后）及切换状态文件，固定为应用私有目录。
     * /data/data/com.termux/files/menu
     */
    @JvmStatic @JvmOverloads
    fun getMenuInstallDir(context: Context = UUtils.getContext()): File {
        val dir = File(context.filesDir, "menu")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        migrateLegacyMenuInstallIfNeeded(context, dir)
        return dir
    }

    /** 与 [getMenuInstallDir] 相同，启动时读写无需存储权限。 */
    @JvmStatic @JvmOverloads
    fun getMenuStateDir(context: Context = UUtils.getContext()): File = getMenuInstallDir(context)

    @JvmStatic @JvmOverloads
    fun getMenuBackupDirDisplayPath(context: Context = UUtils.getContext()): String {
        return getMenuBackupDir(context).absolutePath.replace("/storage/emulated/0", "/sdcard")
    }

    /** @see getMenuBackupDirDisplayPath */
    @JvmStatic @JvmOverloads
    fun getMenuDirDisplayPath(context: Context = UUtils.getContext()): String =
        getMenuBackupDirDisplayPath(context)

    private const val LEGACY_MENU_INSTALL_MIGRATION_DONE = "legacy_install_migrated_v2.done"

    private val MENU_STATE_FILES = arrayOf(
        "active_package.txt",
        "active_label.txt",
        "program_default_v1.done",
        "menu_update_sources.json",
        "selected_update_source.txt",
    )

    private fun migrateLegacyMenuInstallIfNeeded(context: Context, installDir: File) {
        val marker = File(installDir, LEGACY_MENU_INSTALL_MIGRATION_DONE)
        if (marker.exists()) {
            return
        }
        try {
            for (legacyMenuDir in getLegacyExternalMenuDirs(context, installDir)) {
                for (fileName in MENU_STATE_FILES) {
                    copyFileIfMissing(File(legacyMenuDir, fileName), File(installDir, fileName))
                }
            }
            migrateInstalledPackageDirs(getMenuBackupDir(context), installDir)
            marker.writeText("1")
        } catch (e: Exception) {
            android.util.Log.e("XinhaoStoragePath", "migrateLegacyMenuInstallIfNeeded failed", e)
        }
    }

    private fun getLegacyExternalMenuDirs(context: Context, excludeDir: File): List<File> {
        val dirs = ArrayList<File>()
        val sdcardMenu = File(Environment.getExternalStorageDirectory(), "xinhao/menu")
        if (sdcardMenu.absolutePath != excludeDir.absolutePath) {
            dirs.add(sdcardMenu)
        }
        val externalFiles = context.getExternalFilesDir(null)
        if (externalFiles != null) {
            val androidDataMenu = File(externalFiles, "xinhao/menu")
            if (androidDataMenu.absolutePath != excludeDir.absolutePath) {
                dirs.add(androidDataMenu)
            }
        }
        return dirs
    }

    private fun migrateInstalledPackageDirs(sourceDir: File, destDir: File) {
        if (!sourceDir.exists() || !sourceDir.isDirectory) {
            return
        }
        sourceDir.listFiles()?.forEach { file ->
            if (file.name.startsWith(".")) {
                return@forEach
            }
            if (file.isDirectory && File(file, "zt_menu_config.xml").exists()) {
                val target = File(destDir, file.name)
                if (!target.exists()) {
                    copyMenuDirContents(file, target)
                }
            }
        }
    }

    private fun copyFileIfMissing(source: File, dest: File) {
        if (source.exists() && source.isFile && !dest.exists()) {
            source.copyTo(dest, overwrite = false)
        }
    }

    private fun copyMenuDirContents(sourceDir: File, destDir: File) {
        sourceDir.listFiles()?.forEach { file ->
            val target = File(destDir, file.name)
            if (file.isDirectory) {
                if (!target.exists()) {
                    target.mkdirs()
                }
                copyMenuDirContents(file, target)
            } else if (!target.exists()) {
                file.copyTo(target, overwrite = false)
            }
        }
    }

    /** 已安装菜单包解压目录（应用私有 storage）。 */
    @JvmStatic
    fun getMenuPackageDir(context: Context, packageName: String): File =
        File(getMenuInstallDir(context), packageName)

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
            getMenuDir(context),
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
            FileUrl.MAIN_XINHAO_MENU_PATH,
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
