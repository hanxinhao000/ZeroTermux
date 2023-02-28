package cn.hotapk.fhttpserver;


import android.content.Context;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import cn.hotapk.fhttpserver.annotation.RequestMapping;
import cn.hotapk.fhttpserver.utils.FHttpServerUtils;
import cn.hotapk.fhttpserver.utils.FStaticResUtils;

/**
 * @author laijian
 * @version 2017/12/1
 * @Copyright (C)下午11:20 , www.hotapk.cn
 * 管理类
 */
public class FHttpManager {
    private static FHttpManager fHttpManager;
    private FHttpServer fHttpServer;
    private Map<String, Method> methodMaps = new HashMap<>();//获取所有带RequestMapping注解的method
    private Map<String, String> fileMaps = new HashMap<>();//获取所有js，css等静态文件
    private int port = 8080;//端口号
    private String resdir = "";//静态资源目录，默认空 为assets根目录
    private String fileNameFilter = ".*xml";//文件过滤 默认过滤xml文件
    private String indexName = "index.html"; // 设置index名称
    private boolean allowCross = false;//是否允许跨站

    private FHttpManager(Context context, Class... servercls) {
        FHttpServerUtils.init(context.getApplicationContext());
        clsMethods(servercls);
    }

    public static FHttpManager init(Context context, Class... servercls) {
        if (fHttpManager == null) {
            synchronized (FHttpManager.class) {
                if (fHttpManager == null) {
                    fHttpManager = new FHttpManager(context, servercls);
                }
            }
        }
        return fHttpManager;
    }

    /**
     * 根据所提供的类，获取所有带RequestMapping注解的方法
     *
     * @param servercls
     */
    private void clsMethods(Class... servercls) {
        for (Class cls : servercls) {
            Method[] declaredMethods = cls.getDeclaredMethods();
            for (Method method : declaredMethods) {
                if (method.isAnnotationPresent(RequestMapping.class)) {
                    RequestMapping reqValue = method.getAnnotation(RequestMapping.class);
                    methodMaps.put(reqValue.value(), method);
                }
            }
        }
    }

    /**
     * 启动服务
     */

    public void startServer() {
        startServer(NanoHTTPD.SOCKET_READ_TIMEOUT);
    }

    public void startServer(int timeout) {
        if (fHttpServer == null) {
            fileMaps.putAll(FStaticResUtils.getFiles(resdir, fileNameFilter));
            try {
                fHttpServer = new FHttpServer(port);
                fHttpServer.start(timeout);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 关闭服务
     */
    public void stopServer() {
        if (fHttpServer != null) {
            if (fHttpServer.isAlive()) {
                fHttpServer.stop();
            }
        }
    }

    public boolean isAlive() {
        if (fHttpServer != null) {
               return fHttpServer.isAlive();
        }
        return false;
    }

    /**
     * 设置端口号
     *
     * @param port
     */
    public void setPort(int port) {
        this.port = port;
    }

    public int getPort() {
        return port;
    }

    /**
     * 设置静态资源目录
     *
     * @param resdir
     */
    public void setResdir(String resdir) {
        this.resdir = resdir;
    }

    public String getResdir() {
        return resdir;
    }

    /**
     * 获取所有资源文件
     *
     * @return
     */
    public Map<String, String> getFilels() {
        return fileMaps;
    }

    /**
     * 获取所有带RequestMapping注解的方法
     *
     * @return
     */
    public Map<String, Method> getMethods() {
        return methodMaps;
    }

    /**
     * 设置首页html
     *
     * @param indexName
     */
    public void setIndexName(String indexName) {
        this.indexName = indexName;
    }

    public String getIndexName() {
        return indexName;
    }

    /**
     * 设置文件过滤
     *
     * @param filterNames 文件名获取后缀名
     */
    public void setFilterName(String... filterNames) {
        StringBuilder sb = new StringBuilder();
        for (String filterName : filterNames) {
            sb.append(".*" + filterName + "|");
        }
        fileNameFilter = sb.toString().substring(0, sb.length() - 1);
    }

    /**
     * 设置是否允许ajax请求跨站
     *
     * @param allowCross
     */
    public void setAllowCross(boolean allowCross) {
        this.allowCross = allowCross;
    }

    public boolean isAllowCross() {
        return allowCross;
    }


    public static FHttpManager getFHttpManager() {
        return fHttpManager;
    }


}
