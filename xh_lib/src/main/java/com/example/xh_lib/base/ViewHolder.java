package com.example.xh_lib.base;

/**
 * @author ZEL
 * @create By ZEL on 2020/4/7 15:11
 **/
import android.view.View;

public class ViewHolder {
    private View mView;
    public ViewHolder(View mView) {
        this.mView = mView;
    }
    public View getView() {
        return mView;
    }
    public<T> T findViewById(int id) {
        return (T) mView.findViewById(id);
    }
}
