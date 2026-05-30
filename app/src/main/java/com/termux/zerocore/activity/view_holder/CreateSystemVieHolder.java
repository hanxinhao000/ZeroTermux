package com.termux.zerocore.activity.view_holder;

import android.view.View;
import android.widget.TextView;

import androidx.cardview.widget.CardView;

import com.termux.R;

public class CreateSystemVieHolder extends ViewHolder {
    public TextView title;
    public TextView msg;
    public TextView time;
    public CardView itemContainers;
    public CreateSystemVieHolder(View mView) {
        super(mView);
        title = (TextView) findViewById(R.id.title);
        msg = (TextView) findViewById(R.id.msg);
        time = (TextView) findViewById(R.id.time);
        itemContainers = (CardView) findViewById(R.id.item_containers);
    }
}
