package com.termux.zerocore.dialog;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.termux.R;

public class MyDialog extends Dialog {

    private Context mContext;
    private TextView dialog_title;
    private TextView dialog_pro;
    private ProgressBar dialog_pro_prog;

    public MyDialog(@NonNull Context context) {
        super(context, R.style.BaseDialog222);
        initView(context);
    }

    public MyDialog(@NonNull Context context, int themeResId) {
        super(context, themeResId);
        initView(context);
    }


    private void initView(Context mContext) {
        this.mContext = mContext;
        View inflate = View.inflate(mContext, R.layout.dialog_download, null);
        dialog_title = inflate.findViewById(R.id.dialog_title);
        dialog_pro = inflate.findViewById(R.id.dialog_pro);
        dialog_pro_prog = inflate.findViewById(R.id.dialog_pro_prog);
        this.setCancelable(false);
        this.setContentView(inflate);
    }

    public TextView getDialog_title() {
        return dialog_title;
    }


    public TextView getDialog_pro() {

        return dialog_pro;
    }

    public ProgressBar getDialog_pro_prog() {

        return dialog_pro_prog;
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
