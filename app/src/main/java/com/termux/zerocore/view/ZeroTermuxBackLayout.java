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

import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.xh_lib.utils.UUtils;
import com.termux.R;
import com.termux.x11.MainActivity;
import com.termux.zerocore.ftp.utils.UserSetManage;

public class ZeroTermuxBackLayout extends RelativeLayout {
    private View mBackgroundRoot;
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
        mBackgroundRoot = viewLay;
        back_color = viewLay.findViewById(R.id.back_color);
        back_img = viewLay.findViewById(R.id.back_img);
        back_video = viewLay.findViewById(R.id.back_video);
        x11_view = viewLay.findViewById(R.id.x11_view);
        // 加入是否显示逻辑
        boolean internalPassage = UserSetManage.Companion.get().getZTUserBean().isInternalPassage();
        if (internalPassage) {
            try {
                mMainActivity = new MainActivity((Activity) mContext);
                x11_view.addView(mMainActivity);
            } catch (Throwable e) {
                e.printStackTrace();
                android.util.Log.e("ZeroTermuxBackLayout", "X11 init failed, libXlorie.so may be missing for this ABI", e);
            }
        }
        addView(viewLay);
    }

    /** 内部通道 X11 画面需避开状态栏/导航栏，与终端层 fitsSystemWindows 对齐 */
    public void applyX11SystemInsets(Activity activity) {
        if (x11_view == null || mMainActivity == null || mBackgroundRoot == null || activity == null) {
            return;
        }
        int top = UUtils.getStatusBarHeight(activity);
        int bottom = UUtils.getNavigationBarHeight(activity);
        WindowInsetsCompat insets = ViewCompat.getRootWindowInsets(activity.getWindow().getDecorView());
        if (insets != null) {
            top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            bottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;
        }

        // 与终端层一致：整体上移背景层，避免 X11 桌面顶到状态栏区域
        RelativeLayout.LayoutParams bgLp = (RelativeLayout.LayoutParams) mBackgroundRoot.getLayoutParams();
        if (bgLp == null) {
            bgLp = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT
            );
        }
        bgLp.topMargin = top;
        bgLp.bottomMargin = bottom;
        mBackgroundRoot.setLayoutParams(bgLp);

        x11_view.setPadding(0, 0, 0, 0);
        x11_view.setClipToPadding(true);
        mBackgroundRoot.requestLayout();
        requestLayout();
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
