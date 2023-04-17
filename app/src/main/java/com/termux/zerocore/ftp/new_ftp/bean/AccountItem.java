package com.termux.zerocore.ftp.new_ftp.bean;

import androidx.annotation.NonNull;


import com.termux.zerocore.ftp.new_ftp.utils.StorageUtil;

import java.io.Serializable;

public class AccountItem implements Serializable {
    public long id = -1;
    public String account = "";
    public String password = "";
    public String path = StorageUtil.getMainStoragePath();
    public boolean writable = false;

    @Override
    @NonNull
    public String toString() {
        return "AccountItem{" +
                "id=" + id +
                ", account='" + account + '\'' +
                ", password='" + password + '\'' +
                ", path='" + path + '\'' +
                ", writable=" + writable +
                '}';
    }
}
