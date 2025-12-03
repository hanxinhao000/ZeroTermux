package com.termux.zerocore.socket.config;

import android.content.Context;

public interface ZTConfig {
    // 获取相对应的命令执行
    String getCommand(Context context, String command);
    // 当前的对应ID
    int getId();
    // 是否需要转发
    boolean isForWard();
    // 转发页面执行
    String getCommandForWard(Context context, String command);
}
