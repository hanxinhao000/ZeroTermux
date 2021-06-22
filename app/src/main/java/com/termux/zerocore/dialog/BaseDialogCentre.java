package com.termux.zerocore.dialog;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.NonNull;

import com.example.xh_lib.utils.UUtils;
import com.termux.R;



/**
 * @author ZEL
 * @create By ZEL on 2020/10/15 16:45
 **/
public abstract class BaseDialogCentre extends Dialog {

    public Context mContext;
    private boolean mMid = false;
    private boolean mOutCancel = true;

    private float mDimAmount = 0.5f;

    private int mMargin= 0;
    private int mWidth= 0;
    private int mHeight= 0;



    public BaseDialogCentre(@NonNull Context context) {
        super(context, R.style.BaseDialog222);
        initView(context);
    }

    public BaseDialogCentre(@NonNull Context context, int themeResId) {
        super(context, themeResId);
       // initView(context);
    }


    private void initView(Context mContext){

        this.mContext = mContext;

        int contentView = getContentView();

        View viewLay = UUtils.getViewLay(contentView);

        initViewDialog(viewLay);

        setContentView(viewLay);
    }

    public void dialogMid(){

        mMid = true;
    }

    @Override
    public void show() {
        super.show();

        WindowManager.LayoutParams attributes = getWindow().getAttributes();

        attributes.dimAmount = mDimAmount;

        mWidth = ((Activity)mContext).getWindowManager().getDefaultDisplay().getWidth();

        if(mMid){

           // attributes.width =  mWidth - ((mWidth / 10) * 2);
            attributes.width =  mWidth - 300;

        }else{

           // attributes.width =  mWidth - ((mWidth / 6) * 2);
            attributes.width =  mWidth - 300;

        }

        if (mHeight == 0) {
            attributes.height = WindowManager.LayoutParams.WRAP_CONTENT;
        } else {
            attributes.height = mHeight;
        }

        getWindow().setAttributes(attributes);

        setCancelable(mOutCancel);

    }

    abstract void initViewDialog(View mView);

    abstract int getContentView();
}
