package com.termux.zerocore.deepseek.view;

// CustomTextView.java
import android.content.Context;
import android.graphics.Color;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import androidx.appcompat.widget.AppCompatTextView;

import com.example.xh_lib.utils.UUtils;
import com.termux.R;
import com.termux.app.TermuxActivity;

public class CustomTextView extends AppCompatTextView {

    public CustomTextView(Context context) {
        super(context);
        init();
    }

    public CustomTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CustomTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        // 禁用默认选择操作模式
        setCustomSelectionActionModeCallback(new ActionMode.Callback() {
            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                // 清空默认菜单
                menu.clear();
                // 添加自定义菜单项
                menu.add(0, 1, 0, UUtils.getString(R.string.action_copy));
                menu.add(0, 2, 1, UUtils.getString(R.string.deepseek_send_command));
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                // 在这里可以动态更新菜单项
                return true;
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                int start = getSelectionStart();
                int end = getSelectionEnd();
                String selectedText = getText().toString().substring(start, end);

                switch (item.getItemId()) {
                    case 1: // 复制
                        android.content.ClipboardManager clipboard =
                            (android.content.ClipboardManager) getContext()
                                .getSystemService(Context.CLIPBOARD_SERVICE);
                        android.content.ClipData clip = android.content.ClipData
                            .newPlainText("text", selectedText);
                        clipboard.setPrimaryClip(clip);
                        mode.finish();
                        return true;

                    case 2: // Test选项
                        // 自定义操作
                        com.termux.zerocore.utils.SingletonCommunicationUtils.getInstance().getmSingletonCommunicationListener().sendTextToTerminal(selectedText);
                        Context context = getContext();
                        if (context instanceof TermuxActivity) {
                            ((TermuxActivity) context).getDrawer().smoothClose();
                        }
                        mode.finish();
                        return true;
                }
                return false;
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
                // 清除选中样式
                clearSelectionStyle();
            }
        });

        // 设置长按监听器
        setOnLongClickListener(v -> {
            // 让系统处理选择
            return false;
        });
    }

    // 清除选中样式
    private void clearSelectionStyle() {
        CharSequence text = getText();
        if (text instanceof Spannable) {
            Spannable spannable = (Spannable) text;

            BackgroundColorSpan[] bgSpans = spannable.getSpans(0, spannable.length(), BackgroundColorSpan.class);
            ForegroundColorSpan[] fgSpans = spannable.getSpans(0, spannable.length(), ForegroundColorSpan.class);

            for (BackgroundColorSpan span : bgSpans) {
                spannable.removeSpan(span);
            }
            for (ForegroundColorSpan span : fgSpans) {
                spannable.removeSpan(span);
            }
        }
    }

    // Test选项的自定义操作
    private void showTestAction(String selectedText) {
        // 显示自定义对话框或执行其他操作
        android.widget.Toast.makeText(
            getContext(),
            "Test操作: " + selectedText,
            android.widget.Toast.LENGTH_SHORT
        ).show();
    }
}
