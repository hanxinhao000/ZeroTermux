package com.termux.zerocore.view;

import android.content.Context;
import android.graphics.Rect;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.view.WindowInsets;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.VideoView;

import androidx.annotation.RequiresApi;

import com.example.xh_lib.utils.UUtils;
import com.termux.R;

public class ZeroTermuxBackLayout extends RelativeLayout {
    private View back_color;
    private ImageView back_img;
    private VideoView back_video;

    public ZeroTermuxBackLayout(Context context) {
        super(context);
    }

    public ZeroTermuxBackLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        initLayout(context);
    }

    public ZeroTermuxBackLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initLayout(context);
    }

    public ZeroTermuxBackLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initLayout(context);
    }


    /**
     * ZeroTermux
     */
    private void initLayout(Context mContext) {
        View viewLay = UUtils.getViewLay(R.layout.layout_zero_termux);
        back_color = viewLay.findViewById(R.id.back_color);
        back_img = viewLay.findViewById(R.id.back_img);
        back_video = viewLay.findViewById(R.id.back_video);
        addView(viewLay);
    }

    public View getBackColor() {
        return back_color;
    }

    public ImageView getBackImg() {
        return back_img;
    }

    public VideoView getBackVideo() {
        return back_video;
    }

}
