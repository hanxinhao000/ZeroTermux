package com.termux.zerocore.activity;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Environment;
import android.util.TypedValue;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.example.xh_lib.activity.BaseThemeActivity;
import com.example.xh_lib.statusBar.StatusBarCompat;
import com.example.xh_lib.utils.UUtils;
import com.termux.R;
import com.termux.app.TermuxService;
import com.termux.shared.termux.TermuxConstants;
import com.termux.zerocore.activity.adapter.ListBaseAdapter;
import com.termux.zerocore.activity.view_holder.ViewHolder;
import com.termux.zerocore.dialog.SwitchDialog;
import com.termux.zerocore.ftp.utils.UserSetManage;
import com.termux.zerocore.url.FileUrl;
import com.termux.zerocore.utils.FileIOUtils;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class FontActivity extends BaseThemeActivity {
    private ListView list_view;
    private File fontFile;
    private ArrayList<FondDatdBean> arrayList;
    private File termuxFontFile = new File("/data/data/com.termux/files/home/.termux/font.ttf");
    private FontAdapter fontAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        StatusBarCompat.setStatusBarColor(this, ContextCompat.getColor(this, R.color.color_161823));
        setContentView(R.layout.activity_font);
        list_view = findViewById(R.id.list_view);
        getBaseTitle().setVisibility(View.GONE);
        File file1 = new File("/data/data/com.termux/files/home/.termux/");
        if(!file1.exists()){
            file1.mkdirs();
        }

        fontFile = getFontPath();
        arrayList = new ArrayList<>();
        if(!(fontFile.exists())){
            boolean mkdirs = fontFile.mkdirs();
            if (!mkdirs){
                Toast.makeText(UUtils.getContext(), UUtils.getString(R.string.你没有SD卡权限), Toast.LENGTH_SHORT).show();
                finish();
            }
        }
        File termuxDefFontFile = new File(fontFile, "/termux_def.ttf");
        if(!(termuxDefFontFile.exists())){
            new Thread(() -> writerFile("font/font_termux.ttf", termuxDefFontFile)).start();
        }
        File[] files = fontFile.listFiles();
        if(files == null || files.length == 0){
            Toast.makeText(UUtils.getContext(), UUtils.getString(R.string.没有在内部存储), Toast.LENGTH_SHORT).show();
            return;
        }
        for (int i = 0; i < files.length; i++) {
            FondDatdBean fondDatdBean = new FondDatdBean();
            fondDatdBean.mFile = files[i];
            arrayList.add(fondDatdBean);
        }
        fontAdapter = new FontAdapter(arrayList);
        list_view.setAdapter(fontAdapter);
        list_view.setOnItemClickListener((parent, view, position, id) -> writerFileFont(arrayList.get(position).mFile));
    }

    private File getFontPath() {
        if (UserSetManage.Companion.get().getZTUserBean().isCreateFolderForSdcardAndroid()) {
            return FileIOUtils.INSTANCE.getAndroidDataHomeChildPath(UUtils.getContext(), FileUrl.INSTANCE.getMAIN_XINHAO_FONT_PATH());
        } else {
            return new File(Environment.getExternalStorageDirectory(), FileUrl.INSTANCE.getMAIN_XINHAO_FONT_PATH());
        }
    }

    @Override
    public void refreshUI(@NotNull TypedValue mBackground, @NotNull TypedValue mTextColor) {

    }

    class FontAdapter extends ListBaseAdapter<FondDatdBean> {
        public FontAdapter(List<FondDatdBean> list) {
            super(list);
        }
        @Override
        public ViewHolder getViewHolder() {
            return new FontViewHolder(View.inflate(UUtils.getContext(), R.layout.item_font,null));
        }

        @Override
        public void initView(int position, FondDatdBean fondDatdBean, ViewHolder viewHolder) {
            FontViewHolder fontViewHolder = (FontViewHolder) viewHolder;
            fontViewHolder.font_name.setText(fondDatdBean.mFile.getName());
            fontViewHolder.font_lujin.setText(fondDatdBean.mFile.getAbsolutePath());
            Typeface fromAsset = Typeface.createFromFile(fondDatdBean.mFile);
            if(fondDatdBean.mFile.getName().equals("termux_def.ttf")){
                fontViewHolder.font_st.setText(UUtils.getString(R.string.Utermux默认字体));
                fontViewHolder.font_st.setTypeface(fromAsset);
            } else {
                fontViewHolder.font_st.setText(UUtils.getString(R.string.Utermux中文字体));
                fontViewHolder.font_st.setTypeface(fromAsset);
            }
        }
    }
    class FontViewHolder extends ViewHolder{
        public TextView font_name;
        public TextView font_st;
        public TextView font_lujin;
        public FontViewHolder(View mView) {
            super(mView);
            font_name = (TextView) findViewById(R.id.font_name);
            font_st = (TextView) findViewById(R.id.font_st);
            font_lujin = (TextView) findViewById(R.id.font_lujin);
        }
    }
    class FondDatdBean{
        public File mFile;
    }
    //写出文件
    private void writerFile(String name, File mFile) {
        try {
            InputStream open = getAssets().open(name);
            int len = 0;
            if (!mFile.exists()) {
                mFile.createNewFile();
            }
            FileOutputStream fileOutputStream = new FileOutputStream(mFile);
            while ((len = open.read()) != -1) {
                fileOutputStream.write(len);
            }
            fileOutputStream.flush();
            open.close();
            fileOutputStream.close();
        } catch (Exception e) {
        }
    }

    //写出文件
    private void writerFileFont(File mFileSd) {
        try {
            if(termuxFontFile.exists()){
                termuxFontFile.delete();
            }
            boolean newFile = termuxFontFile.createNewFile();
            if(!newFile){
                Toast.makeText(UUtils.getContext(), UUtils.getString(R.string.创建文件错误), Toast.LENGTH_SHORT).show();
            }
            FileInputStream fileInputStream = new FileInputStream(mFileSd);
            FileOutputStream fileOutputStream = new FileOutputStream(termuxFontFile);
            int len = 0;
            byte [] b = new byte[1024];
            while ((len = fileInputStream.read(b)) != -1) {
                fileOutputStream.write(b,0,len);
            }
            fileOutputStream.flush();
            fileInputStream.close();
            fileOutputStream.close();
            Toast.makeText(FontActivity.this, UUtils.getString(R.string.字体设置成功), Toast.LENGTH_SHORT).show();
            SwitchDialog switchDialog1 = switchDialogShow(UUtils.getString(R.string.警告), UUtils.getString(R.string.如果进入终端没有任何显示));
            switchDialog1.getCancel().setOnClickListener(v -> {
                switchDialog1.dismiss();
                Intent intent = new Intent(FontActivity.this, TermuxService.class);
                intent.setAction(TermuxConstants.TERMUX_APP.TERMUX_SERVICE.ACTION_STOP_SERVICE);
                FontActivity.this.startService(intent);
                FontActivity.this.finish();
            });
            switchDialog1.getOk().setOnClickListener(v -> {
                switchDialog1.dismiss();
                Intent intent = new Intent(FontActivity.this, TermuxService.class);
                intent.setAction(TermuxConstants.TERMUX_APP.TERMUX_SERVICE.ACTION_STOP_SERVICE);
                FontActivity.this.startService(intent);
                FontActivity.this.finish();
            });
        } catch (Exception e) {
            Toast.makeText(this, e.getMessage() +":font", Toast.LENGTH_SHORT).show();
        }
    }

    private SwitchDialog switchDialogShow(String title, String msg){
        SwitchDialog switchDialog = new SwitchDialog(this);
        switchDialog.getTitle().setText(title);
        switchDialog.getMsg().setText(msg);
        switchDialog.getOther().setVisibility(View.GONE);
        switchDialog.getOk().setText(UUtils.getString(R.string.确定));
        switchDialog.getCancel().setText(UUtils.getString(R.string.取消));
        switchDialog.show();
        return switchDialog;
    }
}
