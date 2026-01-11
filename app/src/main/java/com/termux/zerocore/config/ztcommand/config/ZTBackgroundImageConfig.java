package com.termux.zerocore.config.ztcommand.config;

import static com.termux.zerocore.config.ztcommand.config.ZTKeyConstants.ZT_ID_BACKGROUND_IMAGE;

import android.content.Context;

import com.example.xh_lib.utils.UUtils;
import com.termux.R;
import com.termux.zerocore.config.ztcommand.ZTSocketService;
import com.termux.zerocore.url.FileUrl;
import com.termux.zerocore.utils.FileIOUtils;

import java.io.File;
import java.io.FileInputStream;

// zt backgroundimage(bgi) 图片路径
// zt bgi 图片路径
public class ZTBackgroundImageConfig extends BaseOkJsonConfig {
    private static final String TAG = ZTBackgroundImageConfig.class.getSimpleName();
    private String mCommand;
    @Override
    public String getCommand(Context context, String command) {
        this.mCommand = command;
        return null;
    }

    @Override
    public int getId() {
        return ZT_ID_BACKGROUND_IMAGE;
    }

    @Override
    public void sendSocketMessage(ZTSocketService.ClientHandler clientHandler, Context context) {
        try {
            File setFileImage = new File(mCommand.split(" ")[1]);
            if (!setFileImage.exists()) {
                clientHandler.sendSocketMessage(getJson(1,
                    UUtils.getString(R.string.zt_command_image_backgroup) + ": " + setFileImage.getAbsolutePath(), ""));
                return;
            }
            File mainConfigImgFile = new File(FileUrl.INSTANCE.getMainConfigImg());
            if (!mainConfigImgFile.exists()) {
                mainConfigImgFile.mkdirs();
            }
            File file = new File(FileUrl.INSTANCE.getMainConfigImg() + "/back.jpg");
            UUtils.writerFileRawInput(file, new FileInputStream(setFileImage));
            FileIOUtils.INSTANCE.clearPathVideo();
            clientHandler.sendMessageToActivity(mCommand.split(" ")[0]);
            clientHandler.sendSocketMessage(getOkJson());
        } catch (Exception e) {
            e.printStackTrace();
            clientHandler.sendSocketMessage(getJson(1, e.toString(), ""));
        }
    }
}
