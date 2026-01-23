package com.termux.zerocore.deepseek.utils;

import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.CharacterStyle;
import android.text.style.LeadingMarginSpan;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.TypefaceSpan;
import android.graphics.Typeface;
import android.view.View;
import android.widget.TextView;
import android.content.Context;
import android.widget.Toast;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.PluralsRes;

import com.example.xh_lib.utils.LogUtils;
import com.example.xh_lib.utils.UUtils;
import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.zerocore.bean.ZTUserBean;
import com.termux.zerocore.deepseek.model.Config;
import com.termux.zerocore.ftp.utils.UserSetManage;

public class SpannableTextUtil {

    private static final String TAG = SpannableTextUtil.class.getSimpleName();
    /**
     * 检测并标记Markwon生成的代码块
     */
    public static Spanned createClickableSpannableString(Spanned spanned, Context context) {
        SpannableStringBuilder builder = new SpannableStringBuilder(spanned);
        String fullText = spanned.toString();

        Log.d("SpannableTextUtil", "开始检测代码块，文本长度: " + fullText.length());

        // 方法1：检测可能的代码块样式
        detectAndMarkCodeBlocks(builder, spanned, context);

        // 方法2：检测连续多行的等宽字体（备用方法）
        detectMonospaceBlocks(builder, spanned, context);

        return builder;
    }

    /**
     * 方法1：通过检测特定Span组合来识别代码块
     */
    private static void detectAndMarkCodeBlocks(SpannableStringBuilder builder, Spanned spanned, Context context) {
        String text = spanned.toString();

        // 可能的代码块特征：
        // 1. 有背景色的区域
        // 2. 使用等宽字体
        // 3. 有固定边距

        // 检测背景色Span（代码块通常有背景色）
        BackgroundColorSpan[] bgSpans = spanned.getSpans(0, spanned.length(), BackgroundColorSpan.class);
        for (BackgroundColorSpan bgSpan : bgSpans) {
            int start = spanned.getSpanStart(bgSpan);
            int end = spanned.getSpanEnd(bgSpan);

            // 检测这个区域是否还有等宽字体
            TypefaceSpan[] typefaceSpans = spanned.getSpans(start, end, TypefaceSpan.class);
            for (TypefaceSpan typefaceSpan : typefaceSpans) {
                if ("monospace".equals(typefaceSpan.getFamily())) {
                    // 很可能是代码块
                    addClickableSpanToRange(builder, start, end, text, context);
                    break;
                }
            }
        }

        // 检测LeadingMarginSpan（代码块通常有左边距）
        LeadingMarginSpan[] marginSpans = spanned.getSpans(0, spanned.length(), LeadingMarginSpan.class);
        for (LeadingMarginSpan marginSpan : marginSpans) {
            int start = spanned.getSpanStart(marginSpan);
            int end = spanned.getSpanEnd(marginSpan);

            // 如果这个区域足够长，可能是代码块
            if (end - start > 10) { // 长度阈值
                String content = text.substring(start, Math.min(end, text.length()));
                // 检查是否包含典型代码特征
                if (containsCodeIndicators(content)) {
                    addClickableSpanToRange(builder, start, end, text, context);
                }
            }
        }
    }

    /**
     * 方法2：检测等宽字体区域
     */
    private static void detectMonospaceBlocks(SpannableStringBuilder builder, Spanned spanned, Context context) {
        String text = spanned.toString();

        // 查找所有等宽字体区域
        TypefaceSpan[] monospaceSpans = spanned.getSpans(0, spanned.length(), TypefaceSpan.class);

        for (TypefaceSpan span : monospaceSpans) {
            if ("monospace".equals(span.getFamily())) {
                int start = spanned.getSpanStart(span);
                int end = spanned.getSpanEnd(span);

                // 检查这是否是一个独立的代码块（不是行内代码）
                String spanText = text.substring(start, Math.min(end, text.length()));
                if (spanText.contains("\n") || spanText.length() > 20) {
                    // 多行或较长文本，更可能是代码块而不是行内代码
                    addClickableSpanToRange(builder, start, end, text, context);
                }
            }
        }
    }

    /**
     * 添加点击事件到指定范围
     */
    private static void addClickableSpanToRange(SpannableStringBuilder builder, int start, int end,
                                                String fullText, Context context) {
        final String codeContent = fullText.substring(start, Math.min(end, fullText.length()));

        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                String replace = codeContent.strip().replace("\n", "")
                    .replace("\u00A0", "") // 专门处理不换行空格
                    .replace("\n", "") // 去除换行
                    .replace("\r", ""); // 去除回车;
                LogUtils.e(TAG, "addClickableSpanToRange replace:" + replace);
                if (context instanceof TermuxActivity) {
                    com.termux.zerocore.utils.SingletonCommunicationUtils.getInstance().getmSingletonCommunicationListener().sendTextToTerminal(replace);
                    ((TermuxActivity) context).getDrawer().smoothClose();
                }
            }

            @Override
            public void updateDrawState(@NonNull TextPaint ds) {
                super.updateDrawState(ds);
                // 改变鼠标指针样式或添加其他视觉反馈
                //ds.setColor(ds.linkColor);
                // 可以添加下划线或其他指示
                ds.setUnderlineText(false);
            }
        };

        // 避免重复添加
        ClickableSpan[] existing = builder.getSpans(start, end, ClickableSpan.class);
        if (existing.length == 0) {
            builder.setSpan(clickableSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            Log.d("SpannableTextUtil", "添加点击事件: " + start + "-" + end + ", 内容: " +
                    (codeContent.length() > 50 ? codeContent.substring(0, 50) + "..." : codeContent));
        }
    }

    /**
     * 检查文本是否包含代码特征
     */
    private static boolean containsCodeIndicators(String text) {
        String commandLink = UserSetManage.Companion.get().getZTUserBean().getCommandLink();
        String codeLink;
        if (TextUtils.isEmpty(commandLink)) {
            codeLink = Config.COMMANDS;
        } else {
            codeLink = commandLink;
        }
        // 代码的常见特征
        try {
            String[] codeIndicators = codeLink.split(",");
            String lowerText = text.toLowerCase();
            for (String indicator : codeIndicators) {
                if (lowerText.contains(indicator.toLowerCase())) {
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            UUtils.showMsg(UUtils.getString(R.string.deepseek_settings_recognition_error_toast));
        }

        // 检查是否有连续的命令行特征
        return text.contains("$ ") || text.contains("# ");
    }
}
