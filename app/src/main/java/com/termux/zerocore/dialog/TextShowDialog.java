package com.termux.zerocore.dialog;

import android.content.Context;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;

import com.termux.R;

/**
 * @author ZEL
 * @create By ZEL on 2020/10/20 17:05
 **/
public class TextShowDialog extends BaseDialogCentre {
    public TextView start;
    public EditText edit_text;
    public TextView commit;
    public TextView name_edit;
    public TextView cancel;
    public LinearLayout commit_ll;
    public LinearLayout diyige_ll;
    public CardView tishi_card_view;
    public TextShowDialog(@NonNull Context context) {
        super(context);
    }

    public TextShowDialog(@NonNull Context context, int themeResId) {
        super(context, themeResId);
    }

    @Override
    void initViewDialog(View mView) {
        start = mView.findViewById(R.id.start);
        edit_text = mView.findViewById(R.id.edit_text);
        commit = mView.findViewById(R.id.commit);
        commit_ll = mView.findViewById(R.id.commit_ll);
        name_edit = mView.findViewById(R.id.name_edit);
        tishi_card_view = mView.findViewById(R.id.tishi_card_view);
        diyige_ll = mView.findViewById(R.id.diyige_ll);
        cancel = mView.findViewById(R.id.cancel);





        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

    }

    @Override
    int getContentView() {
        return R.layout.dialog_text_show;
    }
}
