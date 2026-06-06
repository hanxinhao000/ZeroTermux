package com.termux.zerocore.activity.utils;

import android.content.Context;
import android.content.res.AssetManager;

import com.example.xh_lib.utils.LogUtils;
import com.example.xh_lib.utils.UUtils;
import com.google.gson.Gson;
import com.termux.R;
import com.termux.app.TermuxInstaller;
import com.termux.shared.termux.TermuxConstants;
import com.termux.zerocore.bean.CreateSystemBean;
import com.termux.zerocore.bean.ReadSystemBean;
import com.termux.zerocore.shell.ExeCommand;
import com.termux.zerocore.url.FileUrl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 容器（多 Termux 环境）的创建、列表、切换、删除等核心逻辑。
 */
public final class CreateSystemUtils {

    private static final String TAG = CreateSystemUtils.class.getSimpleName();
    private static final String CONTAINER_DIR_PREFIX = "files";
    private static final String INFO_JSON_FILE = "xinhao_system.infoJson";
    private static final SimpleDateFormat DATE_FORMAT =
        new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    private static final Gson GSON = new Gson();

    private CreateSystemUtils() {
    }

    /** 加载容器列表的结果 */
    public static final class LoadResult {
        public final List<ReadSystemBean> containers;
        public final boolean configError;

        private LoadResult(List<ReadSystemBean> containers, boolean configError) {
            this.containers = containers;
            this.configError = configError;
        }

        public static LoadResult success(List<ReadSystemBean> containers) {
            return new LoadResult(containers, false);
        }

        public static LoadResult configError() {
            return new LoadResult(new ArrayList<>(), true);
        }
    }

    /** 删除容器的结果 */
    public static final class DeleteResult {
        public final boolean blockedAsMain;
        public final boolean deleted;
        public final boolean needsFallbackCleanup;

        private DeleteResult(boolean blockedAsMain, boolean deleted, boolean needsFallbackCleanup) {
            this.blockedAsMain = blockedAsMain;
            this.deleted = deleted;
            this.needsFallbackCleanup = needsFallbackCleanup;
        }

        public static DeleteResult blockedAsMain() {
            return new DeleteResult(true, false, false);
        }

        public static DeleteResult deleted() {
            return new DeleteResult(false, true, false);
        }

        public static DeleteResult needsFallbackCleanup() {
            return new DeleteResult(false, false, true);
        }
    }

    // -------------------------------------------------------------------------
    // 公开 API
    // -------------------------------------------------------------------------

    /** 若当前活跃容器配置文件不存在，则写入默认主系统配置。 */
    public static void ensureDefaultActiveConfig() {
        File configFile = getActiveConfigFile();
        if (configFile.exists()) {
            return;
        }
        try {
            configFile.getParentFile().mkdirs();
            configFile.createNewFile();
            CreateSystemBean bean = new CreateSystemBean();
            bean.systemName = UUtils.getString(R.string.item_containers_main_name);
            bean.dir = TermuxConstants.TERMUX_FILES_DIR_PATH;
            bean.time = DATE_FORMAT.format(new Date());
            writeJson(configFile, bean);
        } catch (IOException e) {
            LogUtils.e(TAG, "ensureDefaultActiveConfig failed: " + e.getMessage());
        }
    }

    /** 扫描并加载所有容器目录。 */
    public static LoadResult loadContainers() {
        File[] entries = listAppDataEntries();
        List<ReadSystemBean> containers = new ArrayList<>();
        for (File entry : entries) {
            if (!isContainerDir(entry)) {
                continue;
            }
            CreateSystemBean info = readContainerInfo(entry);
            if (info.systemName == null) {
                getContainerInfoFile(entry).delete();
                return LoadResult.configError();
            }
            ReadSystemBean item = new ReadSystemBean();
            item.dir = entry.getAbsolutePath();
            item.name = info.systemName;
            item.time = info.time;
            containers.add(item);
        }
        return LoadResult.success(containers);
    }

    /** 根据活跃配置标记列表中当前选中的容器。 */
    public static void markActiveContainer(List<ReadSystemBean> containers) {
        CreateSystemBean active = readActiveConfig();
        if (active == null || active.systemName == null) {
            return;
        }
        for (ReadSystemBean container : containers) {
            container.isCkeck = active.systemName.equals(container.name);
        }
    }

    /** 创建新容器目录及其元数据文件。 */
    public static boolean createContainer(String name) {
        int nextIndex = getNextContainerIndex();
        File containerDir = new File(getAppDataDir(), CONTAINER_DIR_PREFIX + nextIndex);
        if (!containerDir.mkdirs() && !containerDir.exists()) {
            return false;
        }
        return writeContainerInfo(containerDir, name);
    }

    /**
     * 切换到目标容器：更新元数据并通过目录重命名交换 files 与目标目录的内容。
     *
     * @return 切换是否成功
     */
    public static boolean switchContainer(ReadSystemBean target) {
        CreateSystemBean activeConfig = readActiveConfig();
        if (activeConfig == null) {
            return false;
        }

        File targetDir = new File(target.dir);
        File activeDir = new File(activeConfig.dir);
        File tempDir = new File(FileUrl.INSTANCE.getMainHomeTemp());

        try {
            // 更新活跃目录（files）中的配置，指向即将激活的容器
            CreateSystemBean newActiveInfo = copyOf(activeConfig);
            newActiveInfo.dir = target.dir;
            newActiveInfo.time = DATE_FORMAT.format(new Date());
            writeJson(getContainerInfoFile(activeDir), newActiveInfo);
            writeJson(getActiveConfigFile(), newActiveInfo);

            // 更新目标目录中的配置，记录被换出的旧容器信息
            CreateSystemBean swappedInfo = copyOf(activeConfig);
            swappedInfo.dir = TermuxConstants.TERMUX_FILES_DIR_PATH;
            swappedInfo.systemName = target.name;
            writeJson(getContainerInfoFile(targetDir), swappedInfo);

            // 三目录轮换：target -> temp, active -> target, temp -> active
            if (!targetDir.renameTo(tempDir)) {
                return false;
            }
            if (!activeDir.renameTo(targetDir)) {
                tempDir.renameTo(targetDir);
                return false;
            }
            if (!tempDir.renameTo(activeDir)) {
                return false;
            }
            return true;
        } catch (IOException e) {
            LogUtils.e(TAG, "switchContainer failed: " + e.getMessage());
            return false;
        }
    }

    /** 删除非主容器目录。需在后台线程调用。 */
    public static DeleteResult deleteContainer(Context context, String containerDir) {
        if (isMainContainer(containerDir)) {
            return DeleteResult.blockedAsMain();
        }

        prepareBusybox(context);
        ExeCommand cmd = new ExeCommand(false).run(
            FileUrl.INSTANCE.getBusyboxPath() + " rm -rf " + containerDir, 60000, false);
        while (cmd.isRunning()) {
            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        if (new File(containerDir).exists()) {
            return DeleteResult.needsFallbackCleanup();
        }
        return DeleteResult.deleted();
    }

    public static boolean isMainContainer(String containerDir) {
        return TermuxConstants.TERMUX_FILES_DIR_PATH.equals(containerDir);
    }

    public static void clearActiveMarks(List<ReadSystemBean> containers) {
        for (ReadSystemBean container : containers) {
            container.isCkeck = false;
        }
    }

    // -------------------------------------------------------------------------
    // 内部实现
    // -------------------------------------------------------------------------

    private static File getAppDataDir() {
        return new File(TermuxConstants.TERMUX_INTERNAL_PRIVATE_APP_DATA_DIR_PATH);
    }

    private static File getActiveConfigFile() {
        return new File(FileUrl.INSTANCE.getXinhaoSystemPath());
    }

    private static File getContainerInfoFile(File containerDir) {
        return new File(containerDir, INFO_JSON_FILE);
    }

    private static File[] listAppDataEntries() {
        File[] entries = getAppDataDir().listFiles();
        return entries != null ? entries : new File[0];
    }

    private static boolean isContainerDir(File file) {
        return file.isDirectory() && file.getName().startsWith(CONTAINER_DIR_PREFIX);
    }

    private static int getNextContainerIndex() {
        int maxIndex = 0;
        boolean found = false;
        for (File entry : listAppDataEntries()) {
            if (!isContainerDir(entry)) {
                continue;
            }
            int index = parseContainerIndex(entry.getName());
            if (!found || index > maxIndex) {
                maxIndex = index;
                found = true;
            }
        }
        return found ? maxIndex + 1 : 1;
    }

    private static int parseContainerIndex(String dirName) {
        String suffix = dirName.substring(CONTAINER_DIR_PREFIX.length());
        if (suffix.isEmpty()) {
            return 0;
        }
        return Integer.parseInt(suffix);
    }

    private static CreateSystemBean readActiveConfig() {
        File configFile = getActiveConfigFile();
        if (!configFile.exists()) {
            return null;
        }
        try {
            CreateSystemBean bean = readJson(configFile, CreateSystemBean.class);
            if (bean == null) {
                bean = defaultActiveConfig();
            }
            return bean;
        } catch (IOException e) {
            LogUtils.e(TAG, "readActiveConfig failed: " + e.getMessage());
            return null;
        }
    }

    private static CreateSystemBean defaultActiveConfig() {
        CreateSystemBean bean = new CreateSystemBean();
        bean.dir = TermuxConstants.TERMUX_FILES_DIR_PATH;
        bean.time = DATE_FORMAT.format(new Date());
        bean.systemName = UUtils.getString(R.string.item_containers_toast_def_system);
        return bean;
    }

    private static CreateSystemBean readContainerInfo(File containerDir) {
        File infoFile = getContainerInfoFile(containerDir);
        if (!infoFile.exists()) {
            CreateSystemBean fallback = new CreateSystemBean();
            fallback.systemName = UUtils.getString(R.string.item_containers_toast_def_system);
            return fallback;
        }
        try {
            CreateSystemBean bean = readJson(infoFile, CreateSystemBean.class);
            if (bean == null) {
                bean = new CreateSystemBean();
                bean.systemName = UUtils.getString(R.string.item_containers_error_system);
            }
            return bean;
        } catch (IOException e) {
            LogUtils.e(TAG, "readContainerInfo failed: " + containerDir + ", " + e.getMessage());
            CreateSystemBean fallback = new CreateSystemBean();
            fallback.systemName = UUtils.getString(R.string.item_containers_toast_def_system);
            return fallback;
        }
    }

    private static boolean writeContainerInfo(File containerDir, String name) {
        CreateSystemBean bean = new CreateSystemBean();
        bean.dir = containerDir.getAbsolutePath();
        bean.systemName = name;
        bean.time = DATE_FORMAT.format(new Date());
        try {
            File infoFile = getContainerInfoFile(containerDir);
            infoFile.getParentFile().mkdirs();
            writeJson(infoFile, bean);
            return true;
        } catch (IOException e) {
            LogUtils.e(TAG, "writeContainerInfo failed: " + e.getMessage());
            return false;
        }
    }

    private static CreateSystemBean copyOf(CreateSystemBean source) {
        CreateSystemBean copy = new CreateSystemBean();
        copy.systemName = source.systemName;
        copy.dir = source.dir;
        copy.time = source.time;
        return copy;
    }

    private static <T> T readJson(File file, Class<T> clazz) throws IOException {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(new FileInputStream(file)))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line);
            }
        }
        return GSON.fromJson(content.toString(), clazz);
    }

    private static void writeJson(File file, Object data) throws IOException {
        file.getParentFile().mkdirs();
        try (PrintWriter writer = new PrintWriter(
            new OutputStreamWriter(new FileOutputStream(file)))) {
            writer.print(GSON.toJson(data));
            writer.flush();
        }
    }

    private static void prepareBusybox(Context context) {
        String arch = TermuxInstaller.determineTermuxArchName();
        File busybox = new File(FileUrl.INSTANCE.getBusyboxPath());
        File busyboxStatic = new File(FileUrl.INSTANCE.getBusyboxStaticPath());

        switch (arch) {
            case "aarch64":
                copyAsset(context.getAssets(), "arm_64/busybox", busybox);
                copyAsset(context.getAssets(), "arm_64/busybox_static", busyboxStatic);
                break;
            case "arm":
                copyAsset(context.getAssets(), "arm/busybox", busybox);
                break;
            case "x86_64":
                copyAsset(context.getAssets(), "x86/busybox", busybox);
                break;
            default:
                break;
        }

        chmod(busybox);
        chmod(busyboxStatic);
    }

    private static void copyAsset(AssetManager assets, String assetPath, File dest) {
        try (InputStream in = assets.open(assetPath);
             FileOutputStream out = new FileOutputStream(dest)) {
            if (!dest.exists()) {
                dest.createNewFile();
            }
            byte[] buffer = new byte[1024];
            int len;
            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }
            out.flush();
        } catch (IOException e) {
            LogUtils.e(TAG, "copyAsset failed: " + assetPath + ", " + e.getMessage());
        }
    }

    private static void chmod(File file) {
        try {
            Runtime.getRuntime().exec("chmod 777 " + file.getAbsolutePath());
        } catch (IOException e) {
            LogUtils.e(TAG, "chmod failed: " + file + ", " + e.getMessage());
        }
    }
}
