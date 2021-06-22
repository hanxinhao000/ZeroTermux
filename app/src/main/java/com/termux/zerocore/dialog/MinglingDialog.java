package com.termux.zerocore.dialog;

import android.content.Context;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;

import com.termux.R;

/**
 * @author ZEL
 * @create By ZEL on 2020/10/20 11:21
 **/
public class MinglingDialog extends BaseDialogCentre {


    public CardView delete;
    public CardView xiugai;

    public MinglingDialog(@NonNull Context context) {
        super(context);
    }

    public MinglingDialog(@NonNull Context context, int themeResId) {
        super(context, themeResId);
    }

    @Override
    void initViewDialog(View mView) {
        delete = mView.findViewById(R.id.delete);
        xiugai = mView.findViewById(R.id.xiugai);

    }

    @Override
    int getContentView() {
        return R.layout.dialog_mingling;
    }



}
