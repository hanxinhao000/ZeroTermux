package cn.hotapk.fhttpserver.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * @author laijian
 * @version 2017/9/12
 * @Copyright (C)上午11:18 , www.hotapk.cn
 * assets 相关操作类
 */
public final class FAssetsUtils {


    private FAssetsUtils() {

    }

    /**
     * 读取assets文件转InputStream
     *
     * @param assetsName
     * @return
     */
    public static InputStream getAssetsToInp(String assetsName) {
        try {
            return FHttpServerUtils.getAppContext().getAssets().open(assetsName);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


    /**
     * 读取assets文件内容
     *
     * @param assetsName
     * @return
     */
    public static String getAssetsToString(String assetsName) {
        try {
            return FFileUtils.readInp(FHttpServerUtils.getAppContext().getAssets().open(assetsName));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 获取assets下所有文件路径，assets下的文件目录不能有.(点) 符号
     *
     * @param assetsPath
     * @param fileNameFilter
     * @return
     */
    public static Map<String, String> getAssetsLs(String assetsPath, String fileNameFilter) {
        Map<String, String> maps = new HashMap<>();
        try {
            String[] assetsList = FHttpServerUtils.getAppContext().getAssets().list(assetsPath);
            for (String fileName : assetsList) {
                if (!fileName.contains(".")) {
                    getFiles(fileName, maps, fileNameFilter);
                } else {
                    if (!fileName.matches(fileNameFilter)) {
                        maps.put(fileName, fileName);
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return maps;
    }

    private static void getFiles(String file, Map<String, String> maps, String fileNameFilter) {
        try {
            String[] assetsList = FHttpServerUtils.getAppContext().getAssets().list(file);
            for (String fileName : assetsList) {
                if (!fileName.contains(".")) {
                    getFiles(file + "/" + fileName, maps, fileNameFilter);
                } else {
                    if (!fileName.matches(fileNameFilter)) {
                        maps.put(fileName, file + "/" + fileName);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
