package com.termux.zerocore.http_service;

import cn.hotapk.fhttpserver.NanoHTTPD;
import cn.hotapk.fhttpserver.annotation.RequestMapping;

/**
 * @author laijian
 * @version 2017/12/3
 * @Copyright (C)上午12:29 , www.hotapk.cn
 */
public class AppController {

    @RequestMapping("appls")
    public NanoHTTPD.Response getAppLs() {
        return setResponse("appfdd");
    }


    public static NanoHTTPD.Response setResponse(String res) {
        return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.OK, "application/octet-stream", res);
    }
}
