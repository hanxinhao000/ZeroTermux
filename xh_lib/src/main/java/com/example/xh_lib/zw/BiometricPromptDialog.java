package com.example.xh_lib.zw;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.example.xh_lib.R;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;


/**
 * Created by gaoyang on 2018/06/19.
 */
public class BiometricPromptDialog extends DialogFragment {

    public static final int STATE_NORMAL = 1;
    public static final int STATE_FAILED = 2;
    public static final int STATE_ERROR = 3;
    public static final int STATE_SUCCEED = 4;
    private TextView mStateTv;
    private TextView mUsePasswordBtn;
    private TextView mCancelBtn;
    private Activity mActivity;
    private OnBiometricPromptDialogActionCallback mDialogActionCallback;

    public interface OnBiometricPromptDialogActionCallback {
        void onDialogDismiss();
        void onUsePassword();
        void onCancel();
    }

    public static BiometricPromptDialog newInstance() {
        BiometricPromptDialog dialog = new BiometricPromptDialog();
        return dialog;
    }

    public void setOnBiometricPromptDialogActionCallback(OnBiometricPromptDialogActionCallback callback) {
        mDialogActionCallback = callback;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setupWindow(getDialog().getWindow());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.layout_biometric_prompt_dialog, container);

        RelativeLayout rootView = view.findViewById(R.id.root_view);
        rootView.setClickable(false);

        mStateTv = view.findViewById(R.id.state_tv);
        mUsePasswordBtn = view.findViewById(R.id.use_password_btn);
        mCancelBtn = view.findViewById(R.id.cancel_btn);

        mUsePasswordBtn.setVisibility(View.GONE);
        mUsePasswordBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mDialogActionCallback != null) {
                    mDialogActionCallback.onUsePassword();
                }

                dismiss();
            }
        });
        mCancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mDialogActionCallback != null) {
                    mDialogActionCallback.onCancel();
                }
                dismiss();
            }
        });
        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mActivity = (Activity) context;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mActivity = activity;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.color.bg_biometric_prompt_dialog);
        }
        return dialog;
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);

        if (mDialogActionCallback != null) {
            mDialogActionCallback.onDialogDismiss();
        }
    }

    private void setupWindow(Window window) {
        if (window != null) {
            WindowManager.LayoutParams lp = window.getAttributes();
            lp.gravity = Gravity.CENTER;
            lp.dimAmount = 0;
            lp.flags |= WindowManager.LayoutParams.FLAG_DIM_BEHIND;
            window.setAttributes(lp);
            window.setBackgroundDrawableResource(R.color.bg_biometric_prompt_dialog);
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }
    }

    public void setState(int state) {
        switch (state) {
            case STATE_NORMAL:
                mStateTv.setTextColor(ContextCompat.getColor(mActivity, R.color.text_quaternary));
                mStateTv.setText(mActivity.getString(R.string.biometric_dialog_state_normal));
                mCancelBtn.setVisibility(View.VISIBLE);
                break;
            case STATE_FAILED:
                mStateTv.setTextColor(ContextCompat.getColor(mActivity, R.color.text_red));
                mStateTv.setText(mActivity.getString(R.string.biometric_dialog_state_failed));
                mCancelBtn.setVisibility(View.VISIBLE);
                break;
            case STATE_ERROR:
                mStateTv.setTextColor(ContextCompat.getColor(mActivity, R.color.text_red));
                mStateTv.setText(mActivity.getString(R.string.biometric_dialog_state_error));
                mCancelBtn.setVisibility(View.GONE);
                break;
            case STATE_SUCCEED:
                mStateTv.setTextColor(ContextCompat.getColor(mActivity, R.color.text_green));
                mStateTv.setText(mActivity.getString(R.string.biometric_dialog_state_succeeded));
                mCancelBtn.setVisibility(View.VISIBLE);

                mStateTv.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        dismiss();
                    }
                }, 500);
                break;
        }
    }

}
