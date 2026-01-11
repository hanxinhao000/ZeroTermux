package com.termux.zerocore.config.ztcommand.config;

import static com.termux.zerocore.dialog.ShowDialogActivity.DIALOG_TYPE_CONFIRMED;
import static com.termux.zerocore.dialog.ShowDialogActivity.DIALOG_TYPE_CONFIRMED_CANCEL;
import static com.termux.zerocore.dialog.ShowDialogActivity.DIALOG_TYPE_EDIT_TEXT;
import static com.termux.zerocore.dialog.ShowDialogActivity.EXTRA_MESSAGE;
import static com.termux.zerocore.dialog.ShowDialogActivity.EXTRA_TITLE;
import static com.termux.zerocore.dialog.ShowDialogActivity.EXTRA_TYPE;
import static com.termux.zerocore.config.ztcommand.config.ZTKeyConstants.ZT_ID_CONFIRMED_DIALOG;

import android.content.Context;
import android.content.Intent;

import com.example.xh_lib.utils.LogUtils;
import com.example.xh_lib.utils.UUtils;
import com.termux.R;
import com.termux.zerocore.dialog.ShowDialogActivity;
import com.termux.zerocore.config.ztcommand.ZTSocketService;

import java.util.HashMap;
import java.util.Map;

public class ConfirmedDialogConfig extends BaseOkJsonConfig {
    private static final String TAG = ConfirmedDialogConfig.class.getSimpleName();
    private String mCommand;
    private static ZTSocketService.ClientHandler mClientHandler;
    @Override
    public String getCommand(Context context, String command) {
        this.mCommand = command;
        return null;
    }

    @Override
    public int getId() {
        return ZT_ID_CONFIRMED_DIALOG;
    }

    @Override
    public void sendSocketMessage(ZTSocketService.ClientHandler clientHandler, Context context) {
        mClientHandler = clientHandler;
        try {
            String[] commands = mCommand.split(" ");
            for (int i = 0; i < commands.length; i++) {
                String command = commands[i];
                if ("-c".equals(command)) {
                    confirmedCancelDialog(context, DIALOG_TYPE_CONFIRMED_CANCEL);
                    return;
                } else if ("-s".equals(command)) {
                    confirmedCancelDialog(context, DIALOG_TYPE_CONFIRMED);
                    return;
                } else if ("-e".equals(command)) {
                    confirmedCancelDialog(context, DIALOG_TYPE_EDIT_TEXT);
                    return;
                }
            }
            mClientHandler.sendSocketMessage(UUtils.getString(R.string.zt_command_dialog_help));
            mClientHandler = null;
        } catch (Exception e) {
            mClientHandler.sendSocketMessage(getJson(1, e.toString(), ""));
            mClientHandler = null;
        }

    }

    private void confirmedCancelDialog(Context context, int type) {
        UUtils.runOnUIThread(() -> {
            try {
                LogUtils.e(TAG, "sendSocketMessage start mCommand: " + mCommand);
                Intent intent = new Intent(context, ShowDialogActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                Map<String, String> stringStringMap = parseCommandSimple(mCommand);
                intent.putExtra(EXTRA_TITLE, stringStringMap.get("t"));
                intent.putExtra(EXTRA_MESSAGE, stringStringMap.get("m"));
                intent.putExtra(EXTRA_TYPE, type);
                context.startActivity(intent);
                mCommand = null;
            } catch (Exception e) {
                e.printStackTrace();
                mClientHandler.sendSocketMessage(getJson(1, e.toString(), ""));
                mClientHandler = null;
            }
        });
    }

    public static void sendMessage(int code, String message) {
        UUtils.runOnThread(() -> {
            mClientHandler.sendSocketMessage(new ConfirmedDialogConfig().getJson(code, message, ""));
            mClientHandler = null;
        });
    }

    public Map<String, String> parseCommandSimple(String command) {
        Map<String, String> result = new HashMap<>();
        result.put("t", null);
        result.put("m", null);

        if (command == null || command.trim().isEmpty()) {
            return result;
        }

        String[] parts = command.split("\\s+", -1); // 保留空字符串

        // 查找 -t 参数
        int tIndex = findParamIndex(parts, "-t");
        if (tIndex != -1) {
            result.put("t", extractParamValue(parts, tIndex));
        }

        // 查找 -m 参数
        int mIndex = findParamIndex(parts, "-m");
        if (mIndex != -1) {
            result.put("m", extractParamValue(parts, mIndex));
        }

        return result;
    }

    private int findParamIndex(String[] parts, String param) {
        for (int i = 0; i < parts.length; i++) {
            if (param.equals(parts[i])) {
                return i;
            }
        }
        return -1;
    }

    private String extractParamValue(String[] parts, int paramIndex) {
        if (paramIndex + 1 >= parts.length) {
            return null; // 参数后面没有值
        }

        StringBuilder value = new StringBuilder();
        for (int i = paramIndex + 1; i < parts.length; i++) {
            if (parts[i].startsWith("-")) {
                // 遇到下一个参数，停止提取
                break;
            }
            if (value.length() > 0) {
                value.append(" ");
            }
            value.append(parts[i]);
        }

        String result = value.toString().trim();
        return result.isEmpty() ? null : result;
    }
}
