package cn.hotapk.fhttpserver.utils;

import java.io.InputStream;
import java.util.Map;


public class FStaticResUtils {

    /**
     * 获取静态文件列表
     *
     * @param resdir
     * @param fileNameFilter
     * @return
     */
    public static Map<String, String> getFiles(String resdir, String fileNameFilter) {
        if (resdir.startsWith("/")) {
            return FFileUtils.getFileLs(resdir, fileNameFilter);
        } else {
            return FAssetsUtils.getAssetsLs(resdir, fileNameFilter);
        }
    }

    /**
     * 获取文件流
     *
     * @param filePath
     * @return
     */
    public  static InputStream getFileInp(String filePath) {

        if (filePath.startsWith("/")) {
            return FFileUtils.file2Inp(filePath);
        } else {
            return FAssetsUtils.getAssetsToInp(filePath);
        }
    }

}
