package com.termux.zerocore.activity.view_holder;

import android.view.View;
import android.widget.TextView;

import com.termux.R;

public class CreateSystemVieHolder extends ViewHolder {


    public TextView title;

    public TextView msg;



    public CreateSystemVieHolder(View mView) {
        super(mView);

        title = (TextView) findViewById(R.id.title);

        msg = (TextView) findViewById(R.id.msg);


    }
}
