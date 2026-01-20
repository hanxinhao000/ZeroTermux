package com.termux.zerocore.deepseek.markdown;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import io.noties.markwon.LinkResolver;

public class CustomLinkResolver implements LinkResolver {

    private final Context context;

    public CustomLinkResolver(@NonNull Context context) {
        this.context = context;
    }

    @Override
    public void resolve(@NonNull View view, @NonNull String link) {
// 处理 URL
        if (link.startsWith("http://") || link.startsWith("https://")) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));

            // 检查上下文是否是 Activity
            if (context instanceof Activity) {
                context.startActivity(intent);
            } else {
                // 非 Activity 上下文需要添加 FLAG_ACTIVITY_NEW_TASK
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            }
        }
        // 可以添加其他链接类型的处理，如 mailto:、tel: 等
    }
}
