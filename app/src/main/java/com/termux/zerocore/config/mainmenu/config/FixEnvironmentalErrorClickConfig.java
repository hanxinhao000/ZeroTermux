package com.termux.zerocore.config.mainmenu.config;

import static com.termux.zerocore.config.mainmenu.MainMenuConfig.CODE_X11_FEATURES;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.system.ErrnoException;
import android.system.Os;
import android.util.Log;
import android.view.View;

import com.example.xh_lib.utils.UUtils;
import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.zerocore.url.FileUrl;
import com.zp.z_file.util.ZFileUUtils;

import java.io.File;

public class FixEnvironmentalErrorClickConfig extends BaseMenuClickConfig {
    @Override
    public int getType() {
        return CODE_X11_FEATURES;
    }

    @Override
    public Drawable getIcon(Context context) {
        return context.getDrawable(R.mipmap.x11_so_install);
    }

    @Override
    public String getString(Context context) {
        return context.getString(R.string.x11_so_install);
    }

    @Override
    public void onClick(View view, Context context) {
        TermuxActivity termuxActivity = (TermuxActivity) context;
        UUtils.runOnThread(() -> {
            File aislePathSo = new File(FileUrl.INSTANCE.getAislePathSo());
            File aislePathAPKFile = new File(FileUrl.INSTANCE.getAislePathAPK());
            try {
                Os.chmod(aislePathAPKFile.getAbsolutePath(), 0777);
            } catch (ErrnoException e) {
                e.printStackTrace();
            }
            if (!aislePathAPKFile.exists()) {
                boolean delete = aislePathAPKFile.delete();
                Log.i("TAG", "installAisleFile delete: " + delete);
            }
            if (!ZFileUUtils.writerFile(termuxActivity.mInternalPassage? "x11/aisle_zt_loader.apk"
                : "x11/aisle_x11_loader.apk", aislePathAPKFile)) {
                UUtils.runOnUIThread(() -> {
                    UUtils.showMsg(context.getString(R.string.x11_so_install_error));
                });
                return;
            }
            try {
                Os.chmod(aislePathAPKFile.getAbsolutePath(), 0444);
            } catch (ErrnoException e) {
                e.printStackTrace();
            }
            ZFileUUtils.writerFile("x11/libXlorie.so", aislePathSo);
            UUtils.runOnUIThread(() -> {
                UUtils.showMsg(context.getString(R.string.x11_so_install_ok));
            });
        });
    }
}
