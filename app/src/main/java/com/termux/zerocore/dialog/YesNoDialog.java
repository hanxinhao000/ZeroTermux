package com.termux.zerocore.dialog;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.termux.R;

public class YesNoDialog extends Dialog {

    private Context mContext;

    private TextView dialog_title;
    private TextView dialog_msg;
    private TextView dialog_yes;
    private TextView dialog_no;
    private EditText input_system_name;


    public YesNoDialog(@NonNull Context context) {
        super(context,R.style.BaseDialog222);
        initView(context);
    }

    public YesNoDialog(@NonNull Context context, int themeResId) {
        super(context, themeResId);
        initView(context);
    }


    private void initView(Context mContext) {
        this.mContext = mContext;
        View inflate = View.inflate(mContext, R.layout.dialog_yes_no, null);

        dialog_title = inflate.findViewById(R.id.dialog_title);
        dialog_msg = inflate.findViewById(R.id.dialog_msg);
        dialog_yes = inflate.findViewById(R.id.dialog_yes);
        dialog_no = inflate.findViewById(R.id.dialog_no);
        input_system_name = inflate.findViewById(R.id.input_system_name);

        this.setCancelable(false);
        this.setContentView(inflate);
    }

    public YesNoDialog createEditDialog(String msg) {
        dialog_no.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });
        dialog_title.setText(msg);
        dialog_msg.setVisibility(View.GONE);
        input_system_name.setVisibility(View.VISIBLE);
        return this;
    }

    public TextView getTitleTv(){

        return dialog_title;
    }

    public TextView getMsgTv(){

        return dialog_msg;
    }

    public TextView getYesTv(){

        return dialog_yes;
    }
    public TextView getNoTv(){

        return dialog_no;
    }

    public EditText getInputSystemName(){

        return input_system_name;
    }


    @Override
    public void show() {
        super.show();
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        //lp.width = WindowManager.LayoutParams.FILL_PARENT;
        lp.width = (getWindow().getWindowManager().getDefaultDisplay().getWidth() - (getWindow().getWindowManager().getDefaultDisplay().getWidth() / 10) * 2);
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        getWindow().setAttributes(lp);
    }
}
