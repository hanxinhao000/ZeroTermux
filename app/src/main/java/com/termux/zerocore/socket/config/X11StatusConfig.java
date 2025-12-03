package com.termux.zerocore.socket.config;

import static com.termux.zerocore.socket.config.ZTKeyConstants.ZT_ID_X11_STATUS;

import android.content.Context;

import com.example.xh_lib.utils.UUtils;
import com.termux.R;
import com.termux.x11.MainActivity;

public class X11StatusConfig extends BaseOkJsonConfig {
    @Override
    public String getCommand(Context context, String command) {
        // x11status - 输出当前x11状态(只有内部通道显示) \n
        // 暂时不处理，后续更新2.0处理
        // 需要线程转换
        return getJson(MainActivity.isConnected() ? 0 : 1,
            MainActivity.isConnected() ? UUtils.getString(R.string.连接) : UUtils.getString(R.string.未连接), "");
    }

    @Override
    public int getId() {
        return ZT_ID_X11_STATUS;
    }
}
