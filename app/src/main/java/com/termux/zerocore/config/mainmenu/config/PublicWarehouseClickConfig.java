package com.termux.zerocore.config.mainmenu.config;

import static com.termux.zerocore.config.mainmenu.MainMenuConfig.CODE_ONLINE_FEATURES;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.EditText;

import com.example.xh_lib.utils.UUtils;
import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.zerocore.dialog.EditDialog;

public class PublicWarehouseClickConfig extends BaseMenuClickConfig {
    @Override
    public int getType() {
        return CODE_ONLINE_FEATURES;
    }

    @Override
    public Drawable getIcon(Context context) {
        return context.getDrawable(R.mipmap.gongongcangku);
    }

    @Override
    public String getString(Context context) {
        return context.getString(R.string.公共仓库);
    }

    @Override
    public void onClick(View view, Context context) {
        TermuxActivity termuxActivity = (TermuxActivity) context;
        EditDialog editDialog = new EditDialog(context);
        EditText edit_text = editDialog.getEdit_text();
        editDialog.getCancel().setText(UUtils.getString(R.string.如何创建服务器));
        editDialog.getCancel().setVisibility(View.GONE);
        editDialog.getCancel().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            }
        });
        editDialog.getOk().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String s = edit_text.getText().toString();
                if (s == null || s.isEmpty()) {
                    s = "http://10.242.164.19";
                }
                editDialog.dismiss();
                termuxActivity.startHttp(s);
            }
        });
        editDialog.show();
    }
}
