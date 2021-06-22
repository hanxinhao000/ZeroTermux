package com.termux.zerocore.activity.adapter;

import android.view.View;

import com.example.xh_lib.utils.UUtils;
import com.termux.R;
import com.termux.zerocore.activity.view_holder.RestoreViewHolder;
import com.termux.zerocore.activity.view_holder.ViewHolder;

import java.io.File;
import java.util.List;


public class RestoreAdapter extends ListBaseAdapter<File> {

    public RestoreAdapter(List<File> list) {
        super(list);
    }

    @Override
    public ViewHolder getViewHolder() {
        return new RestoreViewHolder(View.inflate(UUtils.getContext(), R.layout.list_file_list, null));
    }

    @Override
    public void initView(int position, File file, ViewHolder viewHolder) {

        RestoreViewHolder restoreViewHolder = (RestoreViewHolder) viewHolder;

        restoreViewHolder.msg_title.setText(file.getName() + "    [" + getPrintSize(file.length()) + "]");

        restoreViewHolder.msg_message.setText(file.getAbsolutePath());

    }

    //单位换算
    public String getPrintSize(long size) {
        //如果字节数少于1024，则直接以B为单位，否则先除于1024，后3位因太少无意义
        if (size < 1024) {
            return String.valueOf(size) + "B";
        } else {
            size = size / 1024;
        }
        //如果原字节数除于1024之后，少于1024，则可以直接以KB作为单位
        //因为还没有到达要使用另一个单位的时候
        //接下去以此类推
        if (size < 1024) {
            return String.valueOf(size) + "KB";
        } else {
            size = size / 1024;
        }
        if (size < 1024) {
            //因为如果以MB为单位的话，要保留最后1位小数，
            //因此，把此数乘以100之后再取余
            size = size * 100;
            return String.valueOf((size / 100)) + "."
                + String.valueOf((size % 100)) + "MB";
        } else {
            //否则如果要以GB为单位的，先除于1024再作同样的处理
            size = size * 100 / 1024;
            return String.valueOf((size / 100)) + "."
                + String.valueOf((size % 100)) + "GB";
        }
    }

}
