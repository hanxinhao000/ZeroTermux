package com.termux.zerocore.config.ztcommand.config;

import android.content.Context;

import com.termux.zerocore.config.ztcommand.ZTSocketService;

public interface ZTConfig {
    // 获取相对应的命令执行
    String getCommand(Context context, String command);
    // 当前的对应ID
    int getId();
    // 是否需要转发
    boolean isForWard();
    // 转发页面执行
    String getCommandForWard(Context context, String command);

    // 手动发送消息,需要在 getCommand return null 也就是返回空
    void sendSocketMessage(ZTSocketService.ClientHandler clientHandler, Context context);
}
