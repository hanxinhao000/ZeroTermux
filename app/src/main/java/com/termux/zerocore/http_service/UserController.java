package com.termux.zerocore.http_service;

import cn.hotapk.fastandrutils.utils.FFileUtils;
import cn.hotapk.fastandrutils.utils.FLogUtils;
import cn.hotapk.fhttpserver.NanoHTTPD;
import cn.hotapk.fhttpserver.annotation.RequestBody;
import cn.hotapk.fhttpserver.annotation.RequestMapping;
import cn.hotapk.fhttpserver.annotation.RequestParam;
import cn.hotapk.fhttpserver.annotation.ResponseBody;
import cn.hotapk.fhttpserver.utils.FFileUploadUtils;

/**
 * @author laijian
 * @version 2017/12/3
 * @Copyright (C)上午12:29 , www.hotapk.cn
 */
public class UserController {

    @RequestMapping("userls")
    public NanoHTTPD.Response getUserLs() {
        return setResponse("user列表");
    }

    @ResponseBody
    @RequestMapping("getuser")
    public UserBean getUser() {
        return new UserBean("admin", "admin");
    }

    @RequestMapping("gethtml")
    public String getHtml() {
        return "index1.html";
    }

    @RequestMapping("upload")
    public String upload(NanoHTTPD.IHTTPSession session) {
        FFileUploadUtils.uploadFile(session, FFileUtils.getRootDir(), "file");
        return "上传成功";
    }

    @RequestMapping("adduser")
    public NanoHTTPD.Response addUser(@RequestBody UserBean userBean) {
        FLogUtils.getInstance().e(userBean);
        return setResponse("添加成功");
    }

    @RequestMapping("edituser")
    public NanoHTTPD.Response editUser(@RequestParam("userName") String userName, @RequestParam("id") int id) {
        return setResponse("修改成功");
    }

    public static NanoHTTPD.Response setResponse(String res) {
        return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.OK, "application/octet-stream", res);
    }

}
