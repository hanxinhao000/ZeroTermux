package com.termux.zerocore.deepseek.model;


import android.util.Log;

import com.example.xh_lib.utils.LogUtils;
import com.example.xh_lib.utils.UUtils;
import com.termux.R;
import com.termux.zerocore.ftp.utils.UserSetManage;

import okhttp3.*;

import okio.BufferedSource;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import java.util.List;

public class DeepSeekClient {
    private static final String TAG = DeepSeekClient.class.getSimpleName();
    private boolean isStream = false;

    public interface Lis {
        void error();

        void msg(String msg, boolean isError);

        void end();
    }

    public DeepSeekClient() {

    }

    // 向DeepSeek提问
    public void ask(List<RequestMessageItem> messageItemList, boolean stream, Lis lis) {
        try {
            //  创建OkHttp客户端
            OkHttpClient client = new OkHttpClient();
            isStream = stream;

            // 把用户提问添加到请求中
            String requestBody = new RequestBodyParameter("deepseek-chat",
                messageItemList, stream).toString();
            RequestBody body = RequestBody.create(requestBody,
                MediaType.parse("application/json; charset=utf-8"));

            Request request = new Request.Builder()
                .url(Config.DEEP_SEEK_URL)
                .addHeader("Authorization", "Bearer " + UserSetManage.Companion.get().getZTUserBean().getDeepSeekApiKey())
                .post(body)
                .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NotNull Call call, @NotNull IOException e) {
                    LogUtils.e(TAG, "onFailure call: " + call + " ,e: " + e);
                    e.printStackTrace();
                    lis.msg("```call: " + call + "\n\nException: " + e, true);
                    lis.end();
                }

                @Override
                public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                    LogUtils.e(TAG, "onResponse call: " + call + " ,response: " + response);
                    if (response.isSuccessful()) {
                        try {
                            BufferedSource source = response.body().source();
                            String line;
                            while ((line = source.readUtf8Line()) != null) {
                                String processChunk = processChunk(line);
                                if (processChunk != null && processChunk.length() > 0) {
                                    lis.msg(processChunk, false);
                                }
                            }
                            lis.end();

                        } catch (Exception e) {
                            LogUtils.e(TAG, "onResponse data error: " + e);
                            lis.msg("Data Error Exception: " + e, true);
                            lis.end();
                        }
                    } else {
                        lis.msg(UUtils.getString(R.string.deepseek_input_key_error_start_info) + "\n\n```" + response
                            + "```\n\n" + (response.code() == 401 ? UUtils.getString(R.string.deepseek_input_key_error_info) : ""), true);
                        lis.end();
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            UUtils.getHandler().postDelayed(() -> {
                lis.msg(UUtils.getString(R.string.deepseek_input_key_error_start_info) + " \n\n```" + e + "```"
                    + "\n\n" + UUtils.getString(R.string.deepseek_input_key_error_info_1), true);
                lis.end();
            }, 100);
        }
    }


    // 获取DeepSeek返回的内容
    public String getMsg(String msg) {
        try {
            JSONObject jsonObject = new JSONObject(msg);
            JSONArray choices = jsonObject.getJSONArray("choices");
            if (!isStream) {
                return choices.getJSONObject(0).getJSONObject("message").getString("content");
            } else {
                return choices.getJSONObject(0).getJSONObject("delta").getString("content");
            }
        } catch (JSONException e) {
            LogUtils.e(TAG, "getMsg error: " + e);
        }
        return msg;
    }


    // 解析DeepSeek流式数据
    private String processChunk(String chunk) {
        // 去除"data: "前缀
        String cleanChunk = chunk.replaceFirst("^data: ", "");
        // 如果cleanChunk为空或不以'{'开头，则可能是换行符或其他非JSON数据，跳过
        if (cleanChunk.isEmpty() || cleanChunk.charAt(0) != '{') {
            return null;
        }
        return cleanChunk;
    }

}
