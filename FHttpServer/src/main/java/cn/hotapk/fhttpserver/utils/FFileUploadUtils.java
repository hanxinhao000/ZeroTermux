package cn.hotapk.fhttpserver.utils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import cn.hotapk.fhttpserver.NanoHTTPD;


public class FFileUploadUtils {

    /**
     * 文件上传
     *
     * @param session
     * @param fileDir 保存文件的目录
     * @param parm    上传文件的参数
     * @return
     */
    public static boolean uploadFile(NanoHTTPD.IHTTPSession session, String fileDir, String parm) {
        Map<String, String> files = new HashMap<>();
        try {
            session.parseBody(files);
            Map<String, String> parms = session.getParms();
            return FFileUtils.copyFileTo(files.get(parm), fileDir + "/" + parms.get(parm));
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NanoHTTPD.ResponseException e) {
            e.printStackTrace();
        }
        return false;
    }
}
