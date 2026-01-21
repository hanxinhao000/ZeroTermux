package com.termux.zerocore.config.other;

import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.blockchain.ub.utils.httputils.BaseHttpUtils;
import com.blockchain.ub.utils.httputils.HttpResponseListenerBase;
import com.example.xh_lib.utils.LogUtils;
import com.example.xh_lib.utils.UUtils;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.callback.Callback;
import com.lzy.okgo.callback.StringCallback;
import com.lzy.okgo.model.HttpHeaders;
import com.lzy.okgo.model.HttpParams;
import com.lzy.okgo.model.Progress;
import com.lzy.okgo.model.Response;
import com.lzy.okgo.request.base.Request;
import com.lzy.okgo.utils.HttpUtils;
import com.termux.BuildConfig;
import com.termux.zerocore.config.other.data.VersionBean;
import com.termux.zerocore.http.HTTPIP;

import java.util.HashMap;

public class ZTGitHubVersion {
    private static final String TAG = ZTGitHubVersion.class.getSimpleName();

    public static ZTGitHubVersion create() {
        return new ZTGitHubVersion();
    }

    public void initZtVersionVisible(TextView textView) {
        OkGo.<String>get(HTTPIP.GITHUB_VERSION).tag(UUtils.getContext()).headers(new HttpHeaders())
            .params(new HttpParams()).execute(new StringCallback() {

                @Override
                public void onSuccess(Response<String> response) {
                    LogUtils.i(TAG, "onSuccess response: " + response.code());
                    try {
                        if (response.code() == 200) {
                            String body = response.body();
                            LogUtils.i(TAG, "onSuccess body: " + body);
                            JsonElement rootElement = JsonParser.parseString(body);
                            JsonObject rootObject = rootElement.getAsJsonObject();
                            // 获取顶层的name字段
                            String gitName = rootObject.get("name").getAsString();
                            String locationName = "ZeroTermux-" + BuildConfig.VERSION_NAME;
                            LogUtils.i(TAG, "onSuccess gitName: " + gitName + " ,locationName: " + locationName);
                            if (!TextUtils.isEmpty(gitName) && gitName.trim().startsWith("ZeroTermux") &&  !TextUtils.equals(gitName, locationName)) {
                                UUtils.runOnUIThread(() -> textView.setVisibility(View.VISIBLE));
                            } else {
                                UUtils.runOnUIThread(() -> textView.setVisibility(View.GONE));
                            }
                        } else {
                            UUtils.runOnUIThread(() -> textView.setVisibility(View.GONE));
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        LogUtils.i(TAG, "onSuccess Exception: " + e);
                        UUtils.runOnUIThread(() -> textView.setVisibility(View.GONE));
                    }
                }

                @Override
                public void onError(Response<String> response) {
                    super.onError(response);
                    if (response == null) {
                        LogUtils.e(TAG, "onError response: null");
                    } else {
                        LogUtils.e(TAG, "onError response body: " + response.body() + " ,code: " + response.code());
                    }

                    UUtils.runOnUIThread(() -> textView.setVisibility(View.GONE));
                }
            });
    }
}
