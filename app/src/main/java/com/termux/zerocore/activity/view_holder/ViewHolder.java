package com.termux.zerocore.activity.view_holder;

import android.view.View;

public class ViewHolder {

    private View mView;


    public ViewHolder(View mView) {
        this.mView = mView;
    }


    public View getView() {
        return mView;
    }

    public View findViewById(int id) {


        return mView.findViewById(id);
    }
}
