package com.termux.zerocore.view;

import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.view.WindowInsets;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.VideoView;

import androidx.annotation.RequiresApi;

import com.example.xh_lib.utils.UUtils;
import com.termux.R;
import com.termux.x11.MainActivity;
import com.termux.zerocore.ftp.utils.UserSetManage;

public class ZeroTermuxBackLayout extends RelativeLayout {
    private View back_color;
    private ImageView back_img;
    private CustomerVideoView back_video;
    private FrameLayout x11_view;
    private MainActivity mMainActivity;

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
        x11_view = viewLay.findViewById(R.id.x11_view);
        // 加入是否显示逻辑
        boolean internalPassage = UserSetManage.Companion.get().getZTUserBean().isInternalPassage();
        if (internalPassage) {
            mMainActivity = new MainActivity((Activity) mContext);
            x11_view.addView(mMainActivity);
        }
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
    public MainActivity getMainActivity() {
        return mMainActivity;
    }

}
