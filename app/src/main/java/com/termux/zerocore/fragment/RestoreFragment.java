package com.termux.zerocore.fragment;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Environment;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.xh_lib.utils.UUtils;
import com.github.mjdev.libaums.fs.UsbFile;
import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.app.TermuxInstaller;
import com.termux.app.TermuxService;
import com.termux.shared.termux.TermuxConstants;
import com.termux.zerocore.activity.BackNewActivity;
import com.termux.zerocore.activity.adapter.RestoreAdapter;
import com.termux.zerocore.data.UsbFileData;
import com.termux.zerocore.dialog.MyDialog;
import com.termux.zerocore.dialog.YesNoDialog;
import com.termux.zerocore.shell.ExeCommand;
import com.termux.zerocore.utils.QZUtils;
import com.termux.zerocore.utils.SaveData;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;



public class RestoreFragment extends BaseFragment {

    private File mFile12 = new File("/data/data/com.termux/files/usr");
    private File mFile13 = new File("/data/data/com.termux/files/home");
    private File mSdFile = new File(Environment.getExternalStorageDirectory(), "/xinhao/data/");
    private File mFileHomeFiles = new File("/data/data/com.termux/files/");
    private File mFileHome = new File("/data/data/com.termux/busybox");
    private File mFileHomeStatic = new File("/data/data/com.termux/busybox_static");
    private File mFileHomeProot = new File("/data/data/com.termux/proot");
    private File mFileHomeMain = new File("/data/data/com.termux/files/usr/bin/tar");
    private File mFileHomeMainTar = new File("/data/data/com.termux/busybox_tar");
    private ListView mListView;
    private TextView mTitle;
    private TextView mStartRe;
    private Process mProcess;
    private RestoreAdapter restoreAdapter;

    private ArrayList<File> files = new ArrayList<>();

    @Override
    public View getFragmentView() {
        return View.inflate(getContext(), R.layout.fragment_restore_k, null);
    }

    @Override
    public void initFragmentView(View mView) {

        mListView = (ListView) findViewById(R.id.list_view);
        mTitle = (TextView) findViewById(R.id.title);
        mStartRe = (TextView) findViewById(R.id.start_re);



        File[] files1 = mSdFile.listFiles();

        if (files1 == null) {
            mStartRe.setText(UUtils.getString(R.string.??????SD?????????));
            return;
        }

        files.clear();

        for (int i = 0; i < files1.length; i++) {

            if(files1[i].getAbsolutePath().endsWith("tar.gz")){
                files.add(files1[i]);
            }



        }

        if (files.size() == 0) {
            mStartRe.setText(UUtils.getString(R.string.??????SD?????????));
        }


        restoreAdapter = new RestoreAdapter(files);
        mListView.setAdapter(restoreAdapter);



        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            //???????????????.....


            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                File file = files.get(position);


                YesNoDialog yesNoDialog = new YesNoDialog(getActivity());

                yesNoDialog.getTitleTv().setText(UUtils.getString(R.string.????????????));
                yesNoDialog.getMsgTv().setText(UUtils.getString(R.string.???????????????));

                yesNoDialog.getYesTv().setText(UUtils.getString(R.string.???????????????));
                yesNoDialog.getYesTv().setVisibility(View.GONE);
                yesNoDialog.setCancelable(true);

                yesNoDialog.getNoTv().setText(UUtils.getString(R.string.??????));
                yesNoDialog.getNoTv().setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {



                        if (!(new File("/data/data/com.termux/files/home/storage").exists())){

                            Toast.makeText(UUtils.getContext(), UUtils.getString(R.string.??????????????????), Toast.LENGTH_SHORT).show();

                            UUtils.getHandler().post(new Runnable() {
                                @Override
                                public void run() {


                                    TermuxActivity.mTerminalView.sendTextToTerminal(UUtils.getString(R.string.??????????????????????????????));

                                    TermuxActivity.mTerminalView.sendTextToTerminal("termux-setup-storage ");
                                    getActivity().finish();
                                }
                            });



                            return;
                        }



                        if(yesNoDialog.getInputSystemName().getText().toString().isEmpty()){
                            yesNoDialog.getInputSystemName().setVisibility(View.VISIBLE);

                            Toast.makeText(getContext(), UUtils.getString(R.string.?????????????????????), Toast.LENGTH_SHORT).show();
                            return;
                        }


                        yesNoDialog.dismiss();

                        MyDialog myDialog = new MyDialog(getActivity());

                        myDialog.getDialog_title().setText(UUtils.getString(R.string.??????????????????????????????));

                        myDialog.getDialog_pro().setText(UUtils.getString(R.string.?????????????????????));
                        myDialog.getDialog_pro_prog().setVisibility(View.VISIBLE);
                        myDialog.getDialog_pro().setVisibility(View.VISIBLE);
                        myDialog.getDialog_pro_prog().setMax(100);
                        myDialog.getDialog_pro_prog().setProgress(6);

                        myDialog.show();

                        new QZUtils().main(myDialog,yesNoDialog.getInputSystemName().getText().toString(),file,RestoreFragment.this);


                    }
                });

                yesNoDialog.show();

                yesNoDialog.getTitleTv().setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            yesNoDialog.dismiss();
                        }
                    }
                );


                yesNoDialog.getYesTv().setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        yesNoDialog.dismiss();

                        AlertDialog.Builder ab = new AlertDialog.Builder(getActivity());

                        ab.setTitle(UUtils.getString(R.string.??????));

                        ab.setMessage(UUtils.getString(R.string.???????????????????????????????????????????????????));

                        ab.setPositiveButton(UUtils.getString(R.string.????????????????????????), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                mListView.setVisibility(View.GONE);
                                mTitle.setVisibility(View.VISIBLE);
                                Toast.makeText(getContext(), UUtils.getString(R.string.??????), Toast.LENGTH_SHORT).show();
                                ab.create().dismiss();
                                BackNewActivity.mIsRun = true;


                            }
                        });

                        ab.setNeutralButton(UUtils.getString(R.string.??????????????????), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                ab.create().dismiss();
                            }
                        });
                        ab.show();

                    }
                });


            }
        });


    }







    //????????????
    private void writerFile(String name, File mFile) {

        try {
            InputStream open = getContext().getAssets().open(name);

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

    private void writerFile(String name, File mFile, int size) {

        try {
            InputStream open = getContext().getAssets().open(name);

            int len = 0;

            byte[] b = new byte[size];

            if (!mFile.exists()) {
                mFile.createNewFile();
            }

            FileOutputStream fileOutputStream = new FileOutputStream(mFile);

            while ((len = open.read(b)) != -1) {
                fileOutputStream.write(b, 0, len);
            }

            fileOutputStream.flush();
            open.close();
            fileOutputStream.close();
        } catch (Exception e) {

            Log.e("XINHAO_HAN_FILE ", "writerFile: " + e.toString());
        }

    }


}
