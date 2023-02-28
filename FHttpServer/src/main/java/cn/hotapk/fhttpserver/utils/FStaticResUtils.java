package cn.hotapk.fhttpserver.utils;

import java.io.InputStream;
import java.util.Map;

/**
 * @author laijian
 * @version 2017/12/6
 * @Copyright (C)上午10:02 , www.hotapk.cn
 * 静态资源文件处理
 */
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
