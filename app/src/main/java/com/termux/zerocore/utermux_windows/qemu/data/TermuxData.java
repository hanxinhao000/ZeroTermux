package com.termux.zerocore.utermux_windows.qemu.data;

import java.io.File;

public class TermuxData {

    private static TermuxData mTermuxData;
    public int isB_R;
    public int config;
    public File mFile;

    public String vncPassword;


    public static TermuxData getInstall() {

        if (mTermuxData == null) {


            synchronized (TermuxData.class) {


                if (mTermuxData == null) {
                    mTermuxData = new TermuxData();

                    return mTermuxData;
                } else {
                    return mTermuxData;
                }

            }


        } else {
            return mTermuxData;
        }


    }



    public String fileUrl;

    private IsQemuSul mIsQemuSul;

    public IsQemuSul getmIsQemuSul(){
        return mIsQemuSul;
    }

    public void setIsQemuSul(IsQemuSul mIsQemuSul){
        this.mIsQemuSul = mIsQemuSul;
    }

    public interface IsQemuSul{
        void error();

    }
}
