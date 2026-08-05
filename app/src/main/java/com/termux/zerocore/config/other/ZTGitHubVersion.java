package com.termux.zerocore.config.other;

import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import com.example.xh_lib.utils.LogUtils;
import com.example.xh_lib.utils.UUtils;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.callback.StringCallback;
import com.lzy.okgo.model.Response;
import com.termux.BuildConfig;
import com.termux.zerocore.http.HTTPIP;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 从 GitHub latest release 拉取版本，仅当远端版本号大于本地时显示「新版本」。
 * 任何失败（网络、限流、解析、空数据、异常）都不显示。
 */
public class ZTGitHubVersion {
    private static final String TAG = ZTGitHubVersion.class.getSimpleName();
    private static final Pattern VERSION_PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)+)");
    /** GitHub 无 User-Agent 会 403；必须显式带上。 */
    private static final String USER_AGENT = "ZeroTermux/" + BuildConfig.VERSION_NAME;

    public static ZTGitHubVersion create() {
        return new ZTGitHubVersion();
    }

    public void initZtVersionVisible(TextView textView) {
        hide(textView);
        System.out.println();
        if (textView == null) {
            return;
        }
        requestLatest(textView, 0);
    }

    private void requestLatest(TextView textView, int urlIndex) {
        // 可在此追加镜像地址；全部失败则不显示 NEW
        String[] urls = new String[] {
            HTTPIP.GITHUB_VERSION
        };
        if (urlIndex < 0 || urlIndex >= urls.length) {
            hide(textView);
            return;
        }
        final String url = urls[urlIndex];
        try {
            OkGo.<String>get(url)
                .tag(UUtils.getContext())
                .headers("User-Agent", USER_AGENT)
                .headers("Accept", "application/vnd.github+json")
                .headers("X-GitHub-Api-Version", "2022-11-28")
                .execute(new StringCallback() {
                    @Override
                    public void onSuccess(Response<String> response) {
                        try {
                            if (applyLatestRelease(textView, response)) {
                                return;
                            }
                        } catch (Throwable t) {
                            LogUtils.e(TAG, "onSuccess parse failed url=" + url + " err=" + t);
                        }
                        // 当前源失败则试下一个
                        requestLatest(textView, urlIndex + 1);
                    }

                    @Override
                    public void onError(Response<String> response) {
                        try {
                            super.onError(response);
                        } catch (Throwable ignored) {
                        }
                        LogUtils.e(TAG, "onError url=" + url
                            + " code=" + (response == null ? "null" : response.code())
                            + " body=" + safeBody(response));
                        requestLatest(textView, urlIndex + 1);
                    }
                });
        } catch (Throwable t) {
            LogUtils.e(TAG, "requestLatest failed url=" + url + " err=" + t);
            requestLatest(textView, urlIndex + 1);
        }
    }

    /**
     * @return true 已根据有效远端版本设置可见性；false 表示本响应无效，可换源重试
     */
    private static boolean applyLatestRelease(TextView textView, Response<String> response) {
        if (response == null) {
            return false;
        }
        // 不强制 code==200：部分环境下 OkGo 成功回调里 code 可能异常，以可解析 body 为准
        int code = response.code();
        if (code > 0 && (code < 200 || code >= 300)) {
            LogUtils.i(TAG, "applyLatestRelease http code=" + code);
            return false;
        }
        String body = response.body();
        if (TextUtils.isEmpty(body)) {
            LogUtils.i(TAG, "applyLatestRelease empty body");
            return false;
        }
        // 限流/错误页常是 HTML
        String trimmed = body.trim();
        if (!trimmed.startsWith("{")) {
            LogUtils.i(TAG, "applyLatestRelease body not json object");
            return false;
        }
        JsonElement rootElement = JsonParser.parseString(trimmed);
        if (rootElement == null || !rootElement.isJsonObject()) {
            return false;
        }
        JsonObject rootObject = rootElement.getAsJsonObject();
        // draft release 不提示
        if (rootObject.has("draft") && rootObject.get("draft").isJsonPrimitive()
            && rootObject.get("draft").getAsBoolean()) {
            LogUtils.i(TAG, "applyLatestRelease draft release ignored");
            setVisible(textView, false);
            return true;
        }
        String remoteRaw = firstNonEmpty(
            jsonString(rootObject, "tag_name"),
            jsonString(rootObject, "name")
        );
        String localRaw = BuildConfig.VERSION_NAME;
        String remoteVersion = normalizeVersion(remoteRaw);
        String localVersion = normalizeVersion(localRaw);
        LogUtils.i(TAG, "applyLatestRelease remoteRaw=" + remoteRaw
            + " localRaw=" + localRaw
            + " remoteVersion=" + remoteVersion
            + " localVersion=" + localVersion);

        if (TextUtils.isEmpty(remoteVersion) || TextUtils.isEmpty(localVersion)) {
            return false;
        }
        boolean hasNewer = compareVersions(remoteVersion, localVersion) > 0;
        LogUtils.i(TAG, "applyLatestRelease hasNewer=" + hasNewer);
        setVisible(textView, hasNewer);
        return true;
    }

    private static String safeBody(Response<String> response) {
        try {
            String body = response == null ? null : response.body();
            if (body == null) {
                return "<null>";
            }
            return body.length() > 200 ? body.substring(0, 200) + "…" : body;
        } catch (Throwable t) {
            return "<unreadable>";
        }
    }

    private static void hide(TextView textView) {
        setVisible(textView, false);
    }

    private static void setVisible(TextView textView, boolean visible) {
        if (textView == null) {
            return;
        }
        try {
            UUtils.runOnUIThread(() -> {
                try {
                    textView.setVisibility(visible ? View.VISIBLE : View.GONE);
                } catch (Throwable t) {
                    LogUtils.e(TAG, "setVisible ui failed: " + t);
                }
            });
        } catch (Throwable t) {
            LogUtils.e(TAG, "setVisible post failed: " + t);
            try {
                textView.post(() -> textView.setVisibility(visible ? View.VISIBLE : View.GONE));
            } catch (Throwable ignored) {
                try {
                    textView.setVisibility(View.GONE);
                } catch (Throwable ignored2) {
                }
            }
        }
    }

    private static String jsonString(JsonObject obj, String key) {
        try {
            if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) {
                return null;
            }
            JsonElement el = obj.get(key);
            return el.isJsonPrimitive() ? el.getAsString() : null;
        } catch (Throwable t) {
            return null;
        }
    }

    private static String firstNonEmpty(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (!TextUtils.isEmpty(value)) {
                return value.trim();
            }
        }
        return null;
    }

    /**
     * 抽出可比较的数字版本：
     * ZeroTermux-0.118.3.64 / v0.118.3.64 / 0.118.3.64 → 0.118.3.64
     */
    static String normalizeVersion(String raw) {
        if (TextUtils.isEmpty(raw)) {
            return "";
        }
        try {
            Matcher matcher = VERSION_PATTERN.matcher(raw.trim());
            if (matcher.find()) {
                return matcher.group(1);
            }
            return "";
        } catch (Throwable t) {
            return "";
        }
    }

    /**
     * @return &gt;0 若 a&gt;b；0 相等；&lt;0 若 a&lt;b
     */
    static int compareVersions(String a, String b) {
        try {
            String[] pa = a.split("\\.");
            String[] pb = b.split("\\.");
            int n = Math.max(pa.length, pb.length);
            for (int i = 0; i < n; i++) {
                long va = i < pa.length ? parsePart(pa[i]) : 0L;
                long vb = i < pb.length ? parsePart(pb[i]) : 0L;
                if (va != vb) {
                    return Long.compare(va, vb);
                }
            }
            return 0;
        } catch (Throwable t) {
            return 0;
        }
    }

    private static long parsePart(String part) {
        if (TextUtils.isEmpty(part)) {
            return 0L;
        }
        try {
            return Long.parseLong(part.trim());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}
