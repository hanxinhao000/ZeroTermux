package com.termux.x11.utils;

import static android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN;

import android.app.Activity;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.termux.x11.MainActivity;
import com.termux.x11.Prefs;

/**
 * Shadow implementation for ZeroTermux: content root is {@code TermuxActivityRootView}
 * (RelativeLayout), not FrameLayout.
 */
public class FullscreenWorkaround {
    public static void assistActivity(Activity activity) {
        new FullscreenWorkaround(activity);
    }

    private final Activity mActivity;
    private int usableHeightPrevious;

    private FullscreenWorkaround(Activity activity) {
        mActivity = activity;
        FrameLayout content = activity.findViewById(android.R.id.content);
        content.getViewTreeObserver().addOnGlobalLayoutListener(this::possiblyResizeChildOfContent);
    }

    private void possiblyResizeChildOfContent() {
        Prefs p = MainActivity.getPrefs();
        if (
                !mActivity.hasWindowFocus() ||
                !((mActivity.getWindow().getAttributes().flags & FLAG_FULLSCREEN) == FLAG_FULLSCREEN) ||
                !p.Reseed.get() || !p.fullscreen.get() || SamsungDexUtils.checkDeXEnabled(mActivity)
        )
            return;

        ViewGroup contentRoot = mActivity.findViewById(android.R.id.content);
        if (contentRoot == null || contentRoot.getChildCount() == 0) {
            return;
        }
        View contentChild = contentRoot.getChildAt(0);
        ViewGroup.LayoutParams layoutParams = contentChild.getLayoutParams();
        if (layoutParams == null) {
            return;
        }

        int usableHeightNow = computeUsableHeight(contentChild);
        if (usableHeightNow != usableHeightPrevious) {
            int usableHeightSansKeyboard = contentChild.getRootView().getHeight();
            int heightDifference = usableHeightSansKeyboard - usableHeightNow;
            if (heightDifference > (usableHeightSansKeyboard / 4)) {
                layoutParams.height = usableHeightSansKeyboard - heightDifference;
            } else {
                layoutParams.height = usableHeightSansKeyboard;
            }
            contentChild.requestLayout();
            usableHeightPrevious = usableHeightNow;
        }
    }

    private int computeUsableHeight(View v) {
        Rect r = new Rect();
        v.getWindowVisibleDisplayFrame(r);
        return (r.bottom - r.top);
    }
}
