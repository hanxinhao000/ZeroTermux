package com.termux.zerocore.dialog;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.xh_lib.utils.UUtils;
import com.termux.R;
import com.termux.zerocore.config.ztcommand.config.ConfirmedDialogConfig;

public class ShowDialogActivity extends AppCompatActivity {
    public static final String EXTRA_TITLE = "dialog_title";
    public static final String EXTRA_MESSAGE = "dialog_message";
    public static final String EXTRA_TYPE = "dialog_type";
    public static final int DIALOG_TYPE_CONFIRMED_CANCEL = 0;
    public static final int DIALOG_TYPE_CONFIRMED = 1;
    public static final int DIALOG_TYPE_EDIT_TEXT = 2;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        if (intent == null) {
            sendMessageAndFinish(1, UUtils.getString(R.string.取消));
            return;
        }
        int intExtra = intent.getIntExtra(EXTRA_TYPE, DIALOG_TYPE_CONFIRMED_CANCEL);
        if (intExtra == DIALOG_TYPE_CONFIRMED) {
            setConfirmedCancelDialog(intent, false);
        } else if (intExtra == DIALOG_TYPE_EDIT_TEXT) {
            setEditDialog(intent);
        } else {
            setConfirmedCancelDialog(intent, true);
        }
    }

    private void setEditDialog(Intent intent) {
        YesNoDialog editDialog = new YesNoDialog(this);
        editDialog.createEditDialog(intent.getStringExtra(EXTRA_TITLE));
        editDialog.show();
        editDialog.getInputSystemName().setHint("");
        editDialog.getYesTv().setOnClickListener(view -> {
            sendMessageAndFinish(0,  editDialog.getInputSystemName().getText().toString());
        });
        editDialog.setOnDismissListener(dialogInterface -> {
            sendMessageAndFinish(3, UUtils.getString(R.string.zt_command_dialog_finish));
        });
        editDialog.getNoTv().setOnClickListener(view -> {
            sendMessageAndFinish(1, UUtils.getString(R.string.取消));
        });
    }
    private void setConfirmedCancelDialog(Intent intent, boolean isShowConfirmed) {
        SwitchDialog switchDialog = switchDialogShow(intent.getStringExtra(EXTRA_TITLE),
            intent.getStringExtra(EXTRA_MESSAGE), isShowConfirmed);
        switchDialog.setOnDismissListener(dialogInterface -> {
            sendMessageAndFinish(3, UUtils.getString(R.string.zt_command_dialog_finish));
        });
        if (isShowConfirmed) {
            switchDialog.getCancel().setOnClickListener(view -> {
                switchDialog.dismiss();
                sendMessageAndFinish(1, UUtils.getString(R.string.取消));
            });
        }
        switchDialog.getOk().setOnClickListener(view -> {
            switchDialog.dismiss();
            sendMessageAndFinish(0, UUtils.getString(R.string.确定));
        });
    }
    private SwitchDialog switchDialogShow(String title, String msg, boolean isShowConfirmed) {
        SwitchDialog switchDialog = new SwitchDialog(this);
        switchDialog.getTitle().setText(title);
        switchDialog.getMsg().setText(msg);
        switchDialog.setCancelable(false);
        switchDialog.getOther().setVisibility(View.GONE);
        switchDialog.getOk().setText(UUtils.getString(R.string.确定));
        if (isShowConfirmed) {
            switchDialog.getCancel().setText(UUtils.getString(R.string.取消));
            switchDialog.getCancel().setVisibility(View.VISIBLE);
        } else {
            switchDialog.getCancel().setVisibility(View.GONE);
        }
        switchDialog.show();
        return switchDialog;
    }

    private void sendMessageAndFinish(int code, String message) {
        ConfirmedDialogConfig.sendMessage(code, message);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        sendMessageAndFinish(2, UUtils.getString(R.string.zt_command_dialog_windows_finish));
    }
}
