package com.termux.zerocore.config.mainmenu

import android.content.Context
import com.example.xh_lib.utils.UUtils
import com.termux.R
import com.termux.zerocore.utils.FileIOUtils
import com.termux.zerocore.utils.XinhaoStoragePath
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/** Java 可调用的网络更新回调。 */
fun interface NetworkFetchCallback {
    fun onResult(success: Boolean, message: String)
}

/**
 * 左侧栏菜单包：安装、切换、备份、网络更新。
 */
object MainMenuPackageManager {

    private const val MENU_XML_FILE = "zt_menu_config.xml"
    private const val ICON_DIR_NAME = "icon"
    private const val ACTIVE_PACKAGE_FILE = "active_package.txt"
    private const val ACTIVE_LABEL_FILE = "active_label.txt"
    private const val NETWORK_PACKAGE_NAME = "network_latest"
    private val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    @JvmStatic
    fun ensureMenuDir(context: Context): File {
        val dir = XinhaoStoragePath.getMenuDir(context)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /** 构建列表：网络更新 + 历史包 + 安装。 */
    @JvmStatic
    fun buildListItems(context: Context): List<MainMenuPackageInfo> {
        ensureMenuDir(context)
        val activeId = getActivePackageId(context)
        val items = ArrayList<MainMenuPackageInfo>()

        val latestNetworkDir = getLatestNetworkPackageDir(context)
        items.add(
            MainMenuPackageInfo(
                id = latestNetworkDir?.name ?: MainMenuPackageInfo.ID_NETWORK,
                label = UUtils.getString(R.string.menu_package_default_label),
                installTime = latestNetworkDir?.lastModified() ?: 0L,
                packageDir = latestNetworkDir,
                type = MainMenuPackageInfo.TYPE_NETWORK,
                isActive = isDefaultMenuActive(activeId)
            )
        )

        val menuDir = XinhaoStoragePath.getMenuDir(context)
        menuDir.listFiles()?.filter { it.isDirectory }?.sortedByDescending { it.lastModified() }
            ?.forEach { dir ->
                if (!hasMenuXml(dir) || isNetworkPackageDir(dir)) {
                    return@forEach
                }
                items.add(
                    MainMenuPackageInfo(
                        id = dir.name,
                        label = dir.name,
                        installTime = dir.lastModified(),
                        packageDir = dir,
                        type = MainMenuPackageInfo.TYPE_INSTALLED,
                        isActive = dir.name == activeId
                    )
                )
            }

        items.add(
            MainMenuPackageInfo(
                id = MainMenuPackageInfo.ID_INSTALL,
                label = UUtils.getString(R.string.menu_package_install),
                type = MainMenuPackageInfo.TYPE_INSTALL
            )
        )
        return items
    }

    @JvmStatic
    fun fetchFromNetwork(context: Context, callback: NetworkFetchCallback) {
        Thread {
            try {
                val menuDir = ensureMenuDir(context)
                val tempZip = File(menuDir, "menu_download_temp.zip")
                val updateUrl = MenuUpdateSourceManager.getSelectedUrl(context)
                if (!downloadFile(updateUrl, tempZip)) {
                    val applied = applyDefaultFromAssets(context)
                    UUtils.runOnUIThread {
                        callback.onResult(applied, UUtils.getString(R.string.menu_package_network_fallback))
                    }
                    return@Thread
                }
                val packageName = NETWORK_PACKAGE_NAME + "_" + System.currentTimeMillis()
                val installed = installFromZip(context, tempZip, packageName)
                tempZip.delete()
                if (installed && applyPackage(context, packageName)) {
                    UUtils.runOnUIThread {
                        callback.onResult(true, UUtils.getString(R.string.menu_package_network_success))
                    }
                } else {
                    val applied = applyDefaultFromAssets(context)
                    UUtils.runOnUIThread {
                        callback.onResult(applied, UUtils.getString(R.string.menu_package_network_fallback))
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                val applied = applyDefaultFromAssets(context)
                UUtils.runOnUIThread {
                    callback.onResult(applied, UUtils.getString(R.string.menu_package_network_fallback))
                }
            }
        }.start()
    }

    @JvmStatic
    fun installFromZip(context: Context, zipFile: File, packageName: String? = null): Boolean {
        if (!zipFile.exists() || !zipFile.isFile) {
            return false
        }
        val name = packageName ?: zipFile.nameWithoutExtension
        val targetDir = XinhaoStoragePath.getMenuPackageDir(context, name)
        if (targetDir.exists()) {
            deleteRecursive(targetDir)
        }
        targetDir.mkdirs()
        return try {
            unzipToDirectory(zipFile, targetDir) && hasMenuXml(targetDir)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    @JvmStatic
    fun getActivePackageLabel(context: Context): String {
        val labelFile = File(ensureMenuDir(context), ACTIVE_LABEL_FILE)
        if (labelFile.exists()) {
            val label = labelFile.readText().trim()
            if (label.isNotEmpty()) {
                return label
            }
        }
        val packageId = getActivePackageId(context)
        if (packageId.isEmpty()) {
            return UUtils.getString(R.string.menu_package_default_label)
        }
        return resolveLabelFromPackageId(packageId)
    }

    @JvmStatic
    fun listMenuZipFiles(context: Context): List<File> {
        val menuDir = ensureMenuDir(context)
        return menuDir.listFiles()
            ?.filter { it.isFile && it.name.endsWith(".zip", ignoreCase = true) }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }

    @JvmStatic
    fun installAndApplyFromMenuZip(context: Context, zipFile: File): Boolean {
        if (!zipFile.exists() || !zipFile.isFile) {
            return false
        }
        val packageName = sanitizeFileName(zipFile.nameWithoutExtension)
        if (packageName.isEmpty()) {
            return false
        }
        if (!installFromZip(context, zipFile, packageName)) {
            return false
        }
        return applyPackage(context, packageName, packageName)
    }

    @JvmStatic
    fun applyMenuPackageInfo(context: Context, info: MainMenuPackageInfo): Boolean {
        if (info.type == MainMenuPackageInfo.TYPE_NETWORK) {
            return applyLatestNetworkPackage(context)
        }
        return applyPackage(context, info.id, info.label)
    }

    @JvmStatic
    fun applyLatestNetworkPackage(context: Context): Boolean {
        val latestId = getLatestNetworkPackageId(context)
        if (latestId != null) {
            return applyPackage(context, latestId, UUtils.getString(R.string.menu_package_default_label))
        }
        return applyDefaultFromAssets(context)
    }

    @JvmStatic
    fun getLatestNetworkPackageId(context: Context): String? {
        return getLatestNetworkPackageDir(context)?.name
    }

    @JvmStatic
    @JvmOverloads
    fun applyPackage(context: Context, packageId: String, displayLabel: String? = null): Boolean {
        val packageDir = XinhaoStoragePath.getMenuPackageDir(context, packageId)
        if (!hasMenuXml(packageDir)) {
            return false
        }
        val menuXml = File(packageDir, MENU_XML_FILE)
        val activeXml = FileIOUtils.getMainMenuXmlPathFile()
        activeXml.parentFile?.mkdirs()
        copyFile(menuXml, activeXml)

        val iconDir = File(packageDir, ICON_DIR_NAME)
        if (iconDir.exists() && iconDir.isDirectory) {
            val ztInfoDir = File(FileIOUtils.getHomePath(context), FileIOUtils.DATA_MESSAGE_PATH_FOLDER)
            ztInfoDir.mkdirs()
            iconDir.listFiles()?.forEach { icon ->
                if (icon.isFile) {
                    copyFile(icon, File(ztInfoDir, icon.name))
                }
            }
            val editIcon = File(iconDir, "edit_menu.png")
            if (editIcon.exists()) {
                copyFile(editIcon, FileIOUtils.getMainEditMenuIconPathFile())
            }
        }
        saveActivePackageId(context, packageId)
        saveActivePackageLabel(context, displayLabel ?: resolveLabelFromPackageId(packageId))
        return true
    }

    @JvmStatic
    fun applyDefaultFromAssets(context: Context): Boolean {
        val locale = context.resources.configuration.locale
        val lang = if (locale.language == "en") "en" else "cn"
        val assetPath = "mainmenu/$lang/$MENU_XML_FILE"
        val activeXml = FileIOUtils.getMainMenuXmlPathFile()
        activeXml.parentFile?.mkdirs()
        UUtils.writerFile(assetPath, activeXml)
        saveActivePackageId(context, "assets_default_$lang")
        saveActivePackageLabel(context, UUtils.getString(R.string.menu_package_default_label))
        return activeXml.exists()
    }

    @JvmStatic
    fun backupCurrentMenu(context: Context, backupName: String): File? {
        return try {
            val menuDir = ensureMenuDir(context)
            val stagingDir = File(menuDir, ".backup_staging")
            if (stagingDir.exists()) {
                deleteRecursive(stagingDir)
            }
            stagingDir.mkdirs()

            val activeXml = FileIOUtils.getMainMenuXmlPathFile()
            if (!activeXml.exists()) {
                return null
            }
            copyFile(activeXml, File(stagingDir, MENU_XML_FILE))

            val iconDir = File(stagingDir, ICON_DIR_NAME)
            iconDir.mkdirs()
            val editIcon = FileIOUtils.getMainEditMenuIconPathFile()
            if (editIcon.exists()) {
                copyFile(editIcon, File(iconDir, editIcon.name))
            }

            val safeName = sanitizeFileName(backupName)
            if (safeName.isEmpty()) {
                return null
            }
            val zipFile = File(menuDir, "$safeName.zip")
            if (zipFile.exists()) {
                zipFile.delete()
            }
            zipDirectory(stagingDir, zipFile)
            deleteRecursive(stagingDir)
            zipFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    @JvmStatic
    fun formatInstallTime(time: Long): String {
        if (time <= 0L) {
            return ""
        }
        return DATE_FORMAT.format(Date(time))
    }

    @JvmStatic
    fun getActivePackageId(context: Context): String {
        val file = File(ensureMenuDir(context), ACTIVE_PACKAGE_FILE)
        if (!file.exists()) {
            return ""
        }
        return file.readText().trim()
    }

    private fun saveActivePackageId(context: Context, packageId: String) {
        File(ensureMenuDir(context), ACTIVE_PACKAGE_FILE).writeText(packageId)
    }

    private fun saveActivePackageLabel(context: Context, label: String) {
        File(ensureMenuDir(context), ACTIVE_LABEL_FILE).writeText(label)
    }

    private fun resolveLabelFromPackageId(packageId: String): String {
        if (packageId.startsWith("assets_default")) {
            return UUtils.getString(R.string.menu_package_default_label)
        }
        if (packageId.startsWith(NETWORK_PACKAGE_NAME)) {
            return UUtils.getString(R.string.menu_package_default_label)
        }
        if (packageId.startsWith("local_")) {
            return UUtils.getString(R.string.menu_package_local_label)
        }
        if (packageId.startsWith("zip_")) {
            val parts = packageId.removePrefix("zip_").split("_")
            if (parts.size >= 2) {
                return parts.dropLast(1).joinToString("_")
            }
        }
        return packageId
    }

    private fun sanitizeFileName(name: String): String {
        return name.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim()
    }

    private fun hasMenuXml(dir: File): Boolean {
        return File(dir, MENU_XML_FILE).exists()
    }

    private fun isNetworkPackageDir(dir: File): Boolean {
        return dir.name.startsWith("${NETWORK_PACKAGE_NAME}_")
    }

    private fun isDefaultMenuActive(activeId: String): Boolean {
        return activeId.isEmpty()
            || activeId.startsWith("assets_default")
            || activeId.startsWith(NETWORK_PACKAGE_NAME)
    }

    private fun getLatestNetworkPackageDir(context: Context): File? {
        val menuDir = ensureMenuDir(context)
        return menuDir.listFiles()
            ?.filter { it.isDirectory && isNetworkPackageDir(it) && hasMenuXml(it) }
            ?.maxByOrNull { it.lastModified() }
    }

    private fun downloadFile(urlStr: String, dest: File): Boolean {
        var connection: HttpURLConnection? = null
        return try {
            val url = URL(urlStr)
            connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 15000
            connection.readTimeout = 30000
            connection.requestMethod = "GET"
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                return false
            }
            connection.inputStream.use { input ->
                FileOutputStream(dest).use { output ->
                    val buffer = ByteArray(8192)
                    var len: Int
                    while (input.read(buffer).also { len = it } != -1) {
                        output.write(buffer, 0, len)
                    }
                    output.flush()
                }
            }
            dest.exists() && dest.length() > 0
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            connection?.disconnect()
        }
    }

    private fun unzipToDirectory(zipFile: File, destDir: File): Boolean {
        if (!destDir.exists()) {
            destDir.mkdirs()
        }
        ZipFile(zipFile).use { zip ->
            val entries = zip.entries()
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement() as ZipEntry
                val outFile = File(destDir, entry.name)
                if (entry.isDirectory) {
                    outFile.mkdirs()
                } else {
                    outFile.parentFile?.mkdirs()
                    zip.getInputStream(entry).use { input ->
                        FileOutputStream(outFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                }
            }
        }
        return true
    }

    private fun zipDirectory(sourceDir: File, zipFile: File) {
        ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
            zipDirectoryRecursive(sourceDir, sourceDir, zos)
        }
    }

    private fun zipDirectoryRecursive(rootDir: File, current: File, zos: ZipOutputStream) {
        current.listFiles()?.forEach { file ->
            val entryName = file.relativeTo(rootDir).path.replace('\\', '/')
            if (file.isDirectory) {
                zipDirectoryRecursive(rootDir, file, zos)
            } else {
                val entry = ZipEntry(entryName)
                zos.putNextEntry(entry)
                FileInputStream(file).use { it.copyTo(zos) }
                zos.closeEntry()
            }
        }
    }

    private fun copyFile(source: File, dest: File) {
        dest.parentFile?.mkdirs()
        FileInputStream(source).use { input ->
            FileOutputStream(dest).use { output ->
                input.copyTo(output)
            }
        }
    }

    private fun deleteRecursive(file: File) {
        if (file.isDirectory) {
            file.listFiles()?.forEach { deleteRecursive(it) }
        }
        file.delete()
    }
}
