package com.termux.zerocore.activity;


import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.LinearLayout;

import com.example.xh_lib.activity.BaseActivity;
import com.example.xh_lib.activity.BaseThemeActivity;
import com.example.xh_lib.statusBar.StatusBarCompat;
import com.example.xh_lib.utils.UUtils;
import com.just.agentweb.AgentWeb;
import com.termux.R;

import androidx.activity.OnBackPressedCallback;

import org.jetbrains.annotations.NotNull;


public class WebViewActivity extends BaseThemeActivity {

    private LinearLayout ll;
    private AgentWeb agentWeb;

    private ValueCallback mValueCallback;
    private ValueCallback mFilePathCallback;
    private final static int RESULT_CODE = 10000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_view);
        StatusBarCompat.cancelLightStatusBar(this);
        setImgLiftAs();
        String title = getIntent().getStringExtra("title");
        if(title!= null) {
            setBaseTitleString(title);
        }
        getRightTv().setVisibility(View.GONE);

        String title_visible = getIntent().getStringExtra("title_visible");

        if(title_visible != null){
            getBaseTitle().setVisibility(View.GONE);
        }else{
            getBaseTitle().setVisibility(View.VISIBLE);
        }
        setting();

        UUtils.showLog("传入的连接:" + getIntent().getStringExtra("content"));
    }

    private void setting() {
        ll = findViewById(R.id.ll);
        agentWeb = AgentWeb.with(this)
                .setAgentWebParent(ll, new LinearLayout.LayoutParams(-1, -1))
                .useDefaultIndicator()
                .setWebChromeClient(mWebChromeClient)
                .createAgentWeb()
                .ready()


                .go(getIntent().getStringExtra("content"));
        agentWeb.getJsInterfaceHolder().addJavaObject("android",new AndroidInterface());
        agentWeb.getWebCreator().getWebView().getSettings().setUseWideViewPort(true);
        agentWeb.getWebCreator().getWebView().getSettings().setLoadWithOverviewMode(true);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (!agentWeb.back()) {
                    finish();
                }
            }
        });
    }

    @Override
    protected void onResume() {
        if (agentWeb != null) {
            agentWeb.getWebLifeCycle().onResume();
        }
        super.onResume();
    }

    @Override
    protected void onPause() {
        if (agentWeb != null) {
            agentWeb.getWebLifeCycle().onPause();
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (agentWeb != null) {
            agentWeb.getWebLifeCycle().onDestroy();
        }
        super.onDestroy();
    }

    public class AndroidInterface {

        @JavascriptInterface //一定要加
        public void finishFromJS() {
            runOnUiThread(() -> {
                if (!isFinishing() && !isDestroyed()) {
                    UUtils.showLog("执行销毁程序...");
                    finish();
                }
            });
        }

        @JavascriptInterface //一定要加
        public void closWordOrder() {
            runOnUiThread(() -> {
                if (!isFinishing() && !isDestroyed()) {
                    finish();
                }
            });
        }

        @JavascriptInterface //一定要加
        public void startWebUrl(String url) {
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;

                UUtils.showLog("传入连接:" + url);
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                intent.addCategory(Intent.CATEGORY_BROWSABLE);
                try {
                    startActivity(intent);
                } catch (android.content.ActivityNotFoundException e) {
                    UUtils.showLog("未找到处理该链接的应用: " + url);
                    e.printStackTrace();
                }
            });
        }

       // window.android.startWebUrl("https://www.baidu.com");
    }
    @Override
    public void refreshUI(@NotNull TypedValue mBackground, @NotNull TypedValue mTextColor) {

    }


    private WebChromeClient mWebChromeClient = new WebChromeClient() {
        // 3.0以下版本
        public void openFileChooser(ValueCallback valueCallback) {
            cancelValueCallback();
            mValueCallback = valueCallback;
            openImageActivity();
        }
        //3.0以上版本
        public void openFileChooser(ValueCallback valueCallback, String acceptType) {
            cancelValueCallback();
            mValueCallback = valueCallback;
            openImageActivity();
        }
        //4.1以上版本
        public void openFileChooser(ValueCallback valueCallback, String acceptType,  String capture) {
            cancelValueCallback();
            mValueCallback = valueCallback;
            openImageActivity();
        }
        // 5.0以上版本  主要版本
        @Override
        public boolean onShowFileChooser(WebView webView, ValueCallback filePathCallback, FileChooserParams fileChooserParams) {
            if (mFilePathCallback != null) {
                mFilePathCallback.onReceiveValue(null);
                mFilePathCallback = null;
            }
            mFilePathCallback = filePathCallback;
            openImageActivity();
            return true;
        }

        private void cancelValueCallback() {
            if (mValueCallback != null) {
                mValueCallback.onReceiveValue(null);
                mValueCallback = null;
            }
        }
    };
    private void openImageActivity() {
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("image/*");
        //调起手机图库
        startActivityForResult(Intent.createChooser(i, "Image Chooser"),RESULT_CODE);
    }

    //处理手机返回的图片
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RESULT_CODE) {
            if (null == mValueCallback && null == mFilePathCallback) return;
            Uri result = data == null || resultCode != RESULT_OK ? null : data.getData();
            if (mFilePathCallback != null) {
                onActivityResultAboveL(requestCode, resultCode, data);
            } else if (mValueCallback != null) {
                mValueCallback.onReceiveValue(result);
                mValueCallback = null;
            }
        }
    }



    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void onActivityResultAboveL(int requestCode, int resultCode, Intent intent) {
        if (requestCode != RESULT_CODE || mFilePathCallback == null)
            return;
        Uri[] results = null;
        if (resultCode == Activity.RESULT_OK) {
            if (intent != null) {
                String dataString = intent.getDataString();
                ClipData clipData = intent.getClipData();
                if (clipData != null) {
                    results = new Uri[clipData.getItemCount()];
                    for (int i = 0; i < clipData.getItemCount(); i++) {
                        ClipData.Item item = clipData.getItemAt(i);
                        results[i] = item.getUri();
                    }
                }
                if (dataString != null)
                    results = new Uri[]{Uri.parse(dataString)};
            }
        }
        mFilePathCallback.onReceiveValue(results);
        mFilePathCallback = null;
    }
}
