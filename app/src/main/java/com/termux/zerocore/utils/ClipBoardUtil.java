package com.termux.zerocore.utils;

import static android.content.Context.CLIPBOARD_SERVICE;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.example.xh_lib.utils.LogUtils;
import com.example.xh_lib.utils.UUtils;
import com.termux.R;

public class ClipBoardUtil {
     private static final String TAG = "ClipBoardUtil";
     private static long timeMillis = 0;
     private  ClipboardManager mClipboardManager;
     private  ClipboardManager.OnPrimaryClipChangedListener mOnPrimaryClipChangedListener;

    /**
     * 注册剪切板复制、剪切事件监听
     */
    public void registerClipEvents() {
        mClipboardManager = (ClipboardManager) UUtils.getContext().getSystemService(CLIPBOARD_SERVICE);
        mOnPrimaryClipChangedListener = new ClipboardManager.OnPrimaryClipChangedListener() {
            @Override
            public void onPrimaryClipChanged() {
                try {
                    if (mClipboardManager.hasPrimaryClip()
                        && mClipboardManager.getPrimaryClip().getItemCount() > 0) {
                        // 获取复制、剪切的文本内容
                        CharSequence content =
                            mClipboardManager.getPrimaryClip().getItemAt(0).getText();
                        LogUtils.d(TAG, "registerClipEvents Clipboard text is:" + content);

                        if (timeMillis != 0) {
                            long timeTemp = System.currentTimeMillis();
                            if (timeTemp - timeMillis < 500) {
                                LogUtils.d(TAG, "registerClipEvents onPrimaryClipChanged is more");
                                return;
                            }
                        }
                        timeMillis = System.currentTimeMillis();
                        if (TextUtils.isEmpty(content)){
                            UUtils.showMsg(UUtils.getString(R.string.clipboard_empty_copy));
                            return;
                        }
                        FileIOUtils.INSTANCE.setClipBoardString(content.toString());

                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        };
        mClipboardManager.addPrimaryClipChangedListener(mOnPrimaryClipChangedListener);
    }

    /**
     * 注销监听，避免内存泄漏。
     */
    public void onDestroy() {
        if (mClipboardManager != null && mOnPrimaryClipChangedListener != null) {
            mClipboardManager.removePrimaryClipChangedListener(mOnPrimaryClipChangedListener);
        }
    }

    /**
     * 控制在500ms内不允许在调用
     */

    private boolean is500Ms() {

        return false;
    }
}
