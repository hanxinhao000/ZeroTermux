package com.example.xh_lib.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;


import com.example.xh_lib.R;
import com.example.xh_lib.utils.UUtils;

import androidx.annotation.Nullable;
import cn.bingoogolapple.photopicker.activity.BGAPhotoPickerActivity;
import cn.bingoogolapple.qrcode.core.QRCodeView;
import cn.bingoogolapple.qrcode.zxing.ZXingView;

/***
 *
 *  使用方法
 *
 *  1.跳转到本Activity页面 比如:
 *
 *
 *     创建一个REQUEST_CODE 为 int 类型
 *
 *     如:
 *
 *     //值必须为 1 和本页面 [REQUEST_CODE] 对应
 *     private int REQUEST_CODE = 1;
 *
 *     跳转:
 *
 *     startActivityForResult(new Intent(SendActivity.this, ScanActivity.class), REQUEST_CODE);
 *
 *
 *  2.复制一下代码到你的ACTIVITY
 *
 *
 *
 *    @Override
 *     protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
 *         super.onActivityResult(requestCode, resultCode, data);
 *
 *         if (resultCode == REQUEST_CODE) {
 *         //返回结果在code中
 *             String code = data.getStringExtra("result");
 *         }
 *
 *
 *     }
 *
 *
 */


public class ScanActivity extends BaseActivity implements QRCodeView.Delegate, View.OnClickListener {
    private static final int REQUEST_CODE = 1;
    private static final int REQUEST_CODE_CHOOSE_QRCODE_FROM_GALLERY = 666;


    private ZXingView zxingView;
    private Context context;

    private ImageView imageview_back;

    private TextView tv_xiangce;





    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_scan);

        zxingView = findViewById(R.id.zxingView);
        imageview_back = findViewById(R.id.imageview_back);
        tv_xiangce = findViewById(R.id.tv_xiangce);
        imageview_back.setOnClickListener(this);
        tv_xiangce.setOnClickListener(this);
        context = ScanActivity.this;


        initView();

    }

    private void initView() {
        zxingView.setDelegate(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        zxingView.startCamera(); // 打开后置摄像头开始预览，但是并未开始识别
//        mZXingView.startCamera(Camera.CameraInfo.CAMERA_FACING_FRONT); // 打开前置摄像头开始预览，但是并未开始识别
        zxingView.startSpotAndShowRect();
    }

    @Override
    protected void onStop() {
        zxingView.stopCamera(); // 关闭摄像头预览，并且隐藏扫描框
        super.onStop();
    }




    @Override
    public void onScanQRCodeSuccess(String result) {
        vibrate();
        Intent intent = new Intent();
        intent.putExtra("result", result);

        UUtils.showLog("返回结果：result:" + result);
        setResult(REQUEST_CODE, intent);
        finish();
    }

    @Override
    public void onCameraAmbientBrightnessChanged(boolean isDark) {
        String tipText = zxingView.getScanBoxView().getTipText();
        String ambientBrightnessTip = "\n环境过暗，请打开闪光灯";
        if (isDark) {
            if (!tipText.contains(ambientBrightnessTip)) {
                zxingView.getScanBoxView().setTipText(tipText + ambientBrightnessTip);
            }
        } else {
            if (tipText.contains(ambientBrightnessTip)) {
                tipText = tipText.substring(0, tipText.indexOf(ambientBrightnessTip));
                zxingView.getScanBoxView().setTipText(tipText);
            }
        }
    }

    @Override
    public void onScanQRCodeOpenCameraError() {
    }

    @SuppressLint("MissingPermission")
    private void vibrate() {
        Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        vibrator.vibrate(200);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_CODE_CHOOSE_QRCODE_FROM_GALLERY) {
            final String picturePath = BGAPhotoPickerActivity.getSelectedPhotos(data).get(0);
            // 本来就用到 QRCodeView 时可直接调 QRCodeView 的方法，走通用的回调
            zxingView.decodeQRCode(picturePath);
        }
    }

    @Override
    protected void onDestroy() {
        zxingView.onDestroy(); // 销毁二维码扫描控件
        super.onDestroy();
    }

    @Override
    public void onClick(View view) {

        UUtils.showLog("返回结果：" + view.getId());
        int id = view.getId();
        if (id == R.id.imageview_back) {
            UUtils.showLog("返回结果：2");
            finish();
        } else if (id == R.id.tv_xiangce) {
            UUtils.showLog("返回结果：1");
            Intent photoPickerIntent = new BGAPhotoPickerActivity.IntentBuilder(this)
                    .cameraFileDir(null)
                    .maxChooseCount(1)
                    .selectedPhotos(null)
                    .pauseOnScroll(false)
                    .build();
            startActivityForResult(photoPickerIntent, REQUEST_CODE_CHOOSE_QRCODE_FROM_GALLERY);
        }
    }
}
