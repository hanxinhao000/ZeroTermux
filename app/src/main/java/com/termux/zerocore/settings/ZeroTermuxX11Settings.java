package com.termux.zerocore.settings;

import android.content.Intent;
import android.os.Bundle;
import android.system.ErrnoException;
import android.system.Os;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.xh_lib.utils.UUtils;
import com.termux.R;
import com.termux.app.TermuxService;
import com.termux.x11.LoriePreferences;
import com.termux.zerocore.activity.SwitchActivity;
import com.termux.zerocore.bean.ZTUserBean;
import com.termux.zerocore.dialog.LoadingDialog;
import com.termux.zerocore.dialog.SwitchDialog;
import com.termux.zerocore.ftp.utils.UserSetManage;
import com.termux.zerocore.url.FileUrl;
import com.termux.zerocore.utils.UUUtils;
import com.zp.z_file.util.ZFileUUtils;

import java.io.File;

public class ZeroTermuxX11Settings extends AppCompatActivity {
    // 内部通道
    private CardView internal_passage;
    // 外部通道
    private CardView external_channels;
    private TextView x11_settings_title;
    private TextView x11_settings_summary;
    private TextView msg_title;
    private CardView msg_title_code_view;
    private ImageView x11_settings_img;
    private CardView zt_termux_x11_settings;
    private boolean mIsEnable = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_zero_termux_x11_settings);
        internal_passage = findViewById(R.id.internal_passage);
        external_channels = findViewById(R.id.external_channels);
        x11_settings_title = findViewById(R.id.x11_settings_title);
        x11_settings_summary = findViewById(R.id.x11_settings_summary);
        x11_settings_img = findViewById(R.id.x11_settings_img);
        zt_termux_x11_settings = findViewById(R.id.zt_termux_x11_settings);
        msg_title = findViewById(R.id.msg_title);
        msg_title_code_view = findViewById(R.id.msg_title_code_view);
        boolean internalPassage = UserSetManage.Companion.get().getZTUserBean().isInternalPassage();
        switchAisle(internalPassage);
        mIsEnable = internalPassage;
        internal_passage.setOnClickListener(view -> {
            boolean b = installAisleFile(true, (isSes) -> {
                UUtils.runOnUIThread(() -> {
                    if (isSes) {
                        showCloseDialog();
                    }
                });
            });
            if (!b) {
                return;
            }
            switchAisle(true);
            ZTUserBean ztUserBean = UserSetManage.Companion.get().getZTUserBean();
            ztUserBean.setInternalPassage(true);
            setX11SettingsEnable(true);
            UserSetManage.Companion.get().setZTUserBean(ztUserBean);
        });
        external_channels.setOnClickListener(view -> {
            boolean b = installAisleFile(false, (isSes) -> {
                UUtils.runOnUIThread(() -> {
                    if (isSes) {
                        showCloseDialog();
                    }
                });
            });
            if (!b) {
                return;
            }
            switchAisle(false);
            setX11SettingsEnable(false);
            ZTUserBean ztUserBean = UserSetManage.Companion.get().getZTUserBean();
            ztUserBean.setInternalPassage(false);
            UserSetManage.Companion.get().setZTUserBean(ztUserBean);
        });

        zt_termux_x11_settings.setOnClickListener(view -> {
            startActivity(new Intent(this, LoriePreferences.class));
        });
    }

    //安装内部/外部通道文件
    private boolean installAisleFile(boolean isInternalPassage, RunnableBoolean runnable) {
        LoadingDialog loadingDialog = new LoadingDialog(this);
        loadingDialog.show();

        File aislePathAPKFile = new File(FileUrl.INSTANCE.getAislePathAPK());
        File aislePathAPKFilePath = new File(FileUrl.INSTANCE.getAislePathAPKPath());
        File aislePathAPKSh = new File(FileUrl.INSTANCE.getAislePathSh());
        File aislePreferencePathSh = new File(FileUrl.INSTANCE.getAislePreferencePathSh());
        File aislePathSo = new File(FileUrl.INSTANCE.getAislePathSo());

        if (!aislePathAPKFile.exists()) {
            loadingDialog.dismiss();
            UUtils.showMsg(getString(R.string.x11_error));
            return false;
        }
        if (isInternalPassage) {
            UUtils.runOnThread(() -> {
                // 安装APK pkg install x11-repo && pkg install termux-x11-nightly
                try {
                    Os.chmod(aislePathAPKFile.getAbsolutePath(), 0777);
                } catch (ErrnoException e) {
                    e.printStackTrace();
                }
                if (!aislePathAPKFile.exists()) {
                    boolean delete = aislePathAPKFile.delete();
                    Log.i("TAG", "installAisleFile delete: " + delete);
                }
                if (!ZFileUUtils.writerFile("x11/aisle_zt_loader.apk", aislePathAPKFile)) {
                    UUtils.runOnUIThread(() -> {
                        showInstallLog(getString(R.string.x11_environment_error));
                    });
                    runnable.run(false);
                    UUtils.runOnUIThread(loadingDialog::dismiss);
                    return;
                }
                // 安装执行文件
                if (!ZFileUUtils.writerFile("x11/termux-x11", aislePathAPKSh)) {
                    UUtils.runOnUIThread(() -> {
                        showInstallLog(getString(R.string.x11_environment_error_x11));
                    });
                    runnable.run(false);
                    UUtils.runOnUIThread(loadingDialog::dismiss);
                    return;
                }

                if (!ZFileUUtils.writerFile("x11/termux-x11-preference-zt", aislePreferencePathSh)) {
                    UUtils.runOnUIThread(() -> {
                        showInstallLog(getString(R.string.x11_environment_error_x11));
                    });
                    runnable.run(false);
                    UUtils.runOnUIThread(loadingDialog::dismiss);
                    return;
                }
                ZFileUUtils.writerFile("x11/libXlorie.so", aislePathSo);
                try {
                    Os.chmod(aislePathAPKFile.getAbsolutePath(), 0444);
                    Os.chmod(aislePathAPKSh.getAbsolutePath(), 0777);
                } catch (ErrnoException e) {
                    e.printStackTrace();
                    UUtils.showMsg(getString(R.string.x11_internal_passage_install_error));
                    loadingDialog.dismiss();
                }
                UUtils.runOnUIThread(loadingDialog::dismiss);
                runnable.run(true);
            });
        } else {
            try {
                Os.chmod(aislePathAPKFile.getAbsolutePath(), 0777);
            } catch (ErrnoException e) {
                e.printStackTrace();
            }
            UUtils.runOnThread(() -> {
                // 安装APK
                if (!aislePathAPKFile.exists()) {
                    // 创建目录
                    boolean delete = aislePathAPKFile.delete();
                    if (!delete) {
                        showInstallLog(getString(R.string.x11_environment_error));
                    }
                }
                if (!ZFileUUtils.writerFile("x11/aisle_x11_loader.apk", aislePathAPKFile)) {
                    UUtils.runOnUIThread(() -> {
                        showInstallLog(getString(R.string.x11_environment_error));
                    });
                    runnable.run(false);
                    UUtils.runOnUIThread(loadingDialog::dismiss);
                    return;
                }
                if (!ZFileUUtils.writerFile("x11/termux-x11-preference", aislePreferencePathSh)) {
                    UUtils.runOnUIThread(() -> {
                        showInstallLog(getString(R.string.x11_environment_error_x11));
                    });
                    runnable.run(false);
                    UUtils.runOnUIThread(loadingDialog::dismiss);
                    return;
                }
                try {
                    Os.chmod(aislePathAPKFile.getAbsolutePath(), 0444);
                    Os.chmod(aislePathAPKSh.getAbsolutePath(), 0777);
                } catch (ErrnoException e) {
                    e.printStackTrace();
                    UUtils.showMsg(getString(R.string.x11_internal_passage_install_error));
                    loadingDialog.dismiss();
                }
                UUtils.runOnUIThread(loadingDialog::dismiss);
                runnable.run(true);
            });
        }

        return true;
    }

    private void showInstallLog(String s) {
        if (msg_title_code_view.getVisibility() == View.GONE) {
            msg_title_code_view.setVisibility(View.VISIBLE);
        }
        msg_title.setText(msg_title.getText() + "\n" + s);
    }
    private void setX11SettingsEnable(boolean isEnable) {
        if (isEnable) {
            x11_settings_title.setTextColor(getColor(R.color.color_ffffff));
            x11_settings_summary.setTextColor(getColor(R.color.color_ffffff));
            mIsEnable = true;
            x11_settings_img.setVisibility(View.VISIBLE);
        } else {
            x11_settings_title.setTextColor(getColor(R.color.color_553E3E3E));
            x11_settings_summary.setTextColor(getColor(R.color.color_553E3E3E));
            x11_settings_img.setVisibility(View.INVISIBLE);
            mIsEnable = false;
        }
    }

    private void switchAisle(boolean isInternalPassage) {
        internal_passage.setCardBackgroundColor(getColor(R.color.color_55000000));
        external_channels.setCardBackgroundColor(getColor(R.color.color_55000000));
        if (isInternalPassage) {
            internal_passage.setCardBackgroundColor(getColor(R.color.color_5548baf3));
            setX11SettingsEnable(true);
        } else {
            external_channels.setCardBackgroundColor(getColor(R.color.color_5548baf3));
            setX11SettingsEnable(false);
        }
    }

    private void showCloseDialog() {
        SwitchDialog switchDialog = new SwitchDialog(this);
        switchDialog.createSwitchDialog(getString(R.string.x11_dialog_reset));
        switchDialog.getOk().setOnClickListener(view -> {
            new Intent(ZeroTermuxX11Settings.this, TermuxService.class).setAction("com.termux.service_stop");
            System.exit(0);
            finish();
        });
        switchDialog.show();
    }

    interface RunnableBoolean {
        void run(boolean isSes);
    }
}
