package com.termux.zerocore.keybord;

import android.os.Handler;
import android.view.View;

import com.example.xh_lib.utils.UUtils;
import com.termux.zerocore.zero.engine.ZeroCoreManage;

public class KeyBordManage {

    public static final int KEY_DEF = 60000;
    public static final int KEY_ALT = 60001;
    public static final int KEY_CTRL = 60002;
    public static final int KEY_OTHER = 60003;

    private View mKeyBordView;
    public static KeyBordManage mKeyBordManage;

    private KeyBordManage(){}

    public static KeyBordManage getInstance() {
        if (mKeyBordManage == null) {
            synchronized (KeyBordManage.class) {
                if (mKeyBordManage == null) {
                    mKeyBordManage = new KeyBordManage();
                    return mKeyBordManage;
                } else {
                    return mKeyBordManage;
                }
            }
        } else {
            return mKeyBordManage;
        }
    }

    public View getKeyBordView() {
        if (mKeyBordView == null) {
            mKeyBordView = ZeroCoreManage.getKeyView();
        }
        return mKeyBordView;
    }

    public void initKeyBord(Handler mHandler) {
        ZeroCoreManage.setRunHandler(UUtils.getHandler());
        ZeroCoreManage.setKeyHandler(mHandler);
        ZeroCoreManage.setEngineContext();
        ZeroCoreManage.initKeyView();
    }

}
