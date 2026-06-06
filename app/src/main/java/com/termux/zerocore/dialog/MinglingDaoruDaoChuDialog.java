package com.termux.zerocore.dialog;

import android.content.Context;
import android.os.Environment;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;

import com.example.xh_lib.utils.UUtils;
import com.google.gson.Gson;
import com.termux.R;
import com.termux.zerocore.bean.MinLBean;
import com.termux.zerocore.ftp.utils.UserSetManage;
import com.termux.zerocore.url.FileUrl;
import com.termux.zerocore.utils.FileIOUtils;
import com.termux.zerocore.utils.SaveData;

import java.io.File;


/**
 * @author ZEL
 * @create By ZEL on 2020/10/20 11:21
 **/
public class MinglingDaoruDaoChuDialog extends BaseDialogCentre {


    public CardView daoru;
    public CardView daochu;
    public File file ;
    public MinglingDaoruDaoChuDialog(@NonNull Context context) {
        super(context);
    }

    public MinglingDaoruDaoChuDialog(@NonNull Context context, int themeResId) {
        super(context, themeResId);
    }

    @Override
    void initViewDialog(View mView) {

        file = getCommandPathFile();
        if(!file.exists()){
            file.mkdir();
        }
        daoru = mView.findViewById(R.id.daoru);
        daochu = mView.findViewById(R.id.daochu);

        daoru.setOnClickListener(v -> {
            FileListConfigDialog fileListConfigDialog = new FileListConfigDialog(mContext);
            fileListConfigDialog.show();
            fileListConfigDialog.setOnItemFileClickListener(file -> {

                UUtils.showLog("输出文件:" + file.getAbsolutePath());

                String fileString = UUtils.getFileString(file);

                try {
                    new Gson().fromJson(fileString, MinLBean.class);
                    SaveData.saveData("commi22",fileString);
                    if(mSXXXXlistener!= null){
                        mSXXXXlistener.shuaxin();
                    }
                    UUtils.showMsg(UUtils.getString(R.string.已导入配置文件));
                    fileListConfigDialog.dismiss();
                    dismiss();
                }catch (Exception e){
                    e.printStackTrace();
                    TextShowDialog textShowDialog = new TextShowDialog(mContext);
                    textShowDialog.show();
                    textShowDialog.edit_text.setText(e.toString());
                }
            });

        });

        daochu.setOnClickListener(v -> {
            String commi22 = SaveData.getData("commi22");
            if(commi22 == null || commi22.isEmpty() || commi22.equals("def")){
                UUtils.showMsg(UUtils.getString(R.string.当前没有数据));
                return;
            }

            FileNameMingLDialog fileNameMingLDialog = new FileNameMingLDialog(mContext);
            fileNameMingLDialog.show();
            fileNameMingLDialog.setCancelable(false);
            fileNameMingLDialog.setOnSaveFileNameListener(name -> {

                UUtils.setFileString(new File(file,"" + name + ".txt"),commi22);
                fileNameMingLDialog.dismiss();
                dismiss();
                UUtils.showMsg(UUtils.getString(R.string.保存成功));
            });
        });

    }

    private File getCommandPathFile() {
        return com.termux.zerocore.utils.XinhaoStoragePath.getCommandDir(mContext);
    }

    @Override
    int getContentView() {
        return R.layout.dialog_mingling_daoru_daochu;
    }


    private SXXXXlistener mSXXXXlistener;
    public void setSXXXXlistener(SXXXXlistener mSXXXXlistener){
        this.mSXXXXlistener = mSXXXXlistener;
    }

    public interface SXXXXlistener{

        void shuaxin();
    }

}
