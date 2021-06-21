package com.example.xh_lib.zw;

import android.app.Activity;

/**
 * @author ZEL
 * @create By ZEL on 2020/7/16 15:17
 **/
public class FingerPrintUtils {


    private BiometricPromptManager mBiometricPromptManager;

    private Activity mActivity;

    public FingerPrintUtils(Activity mActivity,boolean isPassword){

        this.mActivity = mActivity;

        mBiometricPromptManager = BiometricPromptManager.from(mActivity, isPassword);

    }

    public void setStartFingerListener(final StartFingerListener mListener){



        if(mBiometricPromptManager.isBiometricPromptEnable()){


            mBiometricPromptManager.authenticate(new BiometricPromptManager.OnBiometricIdentifyCallback() {
                @Override
                public void onUsePassword() {
                    //使用密码按钮 [退出dialog指纹]
                    mListener.onUsePassword();
                }

                @Override
                public void onSucceeded() {
                    //验证成功
                    mListener.onSucceeded();
                }

                @Override
                public void onFailed() {
                    //验证失败
                    mListener.onFailed();
                }

                @Override
                public void onError(int code, String reason) {
                    //错误
                    mListener.onError(code, reason);
                }

                @Override
                public void onCancel() {
                    //返回了
                    mListener.onCancel();
                }
            });



        }else{

            //设备不支持 或设备未加指纹

            //设备不支持或您未启用系统指纹
            mListener.onNonsupport();

        }


    }



  public interface StartFingerListener{


      void onUsePassword();
      void onNonsupport();
      void onSucceeded();
      void onFailed();
      void onError(int code, String reason);
      void onCancel();


  }



}
