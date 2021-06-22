package com.termux.zerocore.activity.adapter;

import android.app.Activity;
import android.graphics.Color;
import android.view.View;

import com.example.xh_lib.utils.UUtils;
import com.termux.R;
import com.termux.zerocore.activity.view_holder.CreateSystemVieHolder;
import com.termux.zerocore.activity.view_holder.ViewHolder;
import com.termux.zerocore.bean.ReadSystemBean;

import java.util.List;



public class CreateSystemAdapter extends ListBaseAdapter<ReadSystemBean> {

    private Activity mActivity;

    public CreateSystemAdapter(List<ReadSystemBean> list, Activity activity) {
        super(list);
        mActivity = activity;
    }


    @Override
    public ViewHolder getViewHolder() {
        return new CreateSystemVieHolder(View.inflate(UUtils.getContext(), R.layout.list_create_system, null));
    }

    @Override
    public void initView(int position, ReadSystemBean readSystemBean, ViewHolder viewHolder) {

        CreateSystemVieHolder createSystemVieHolder = (CreateSystemVieHolder) viewHolder;

        createSystemVieHolder.title.setText(readSystemBean.name);

        createSystemVieHolder.msg.setText(readSystemBean.dir);


        if (readSystemBean.isCkeck) {

            createSystemVieHolder.title.setText(readSystemBean.name + "   <——");
            createSystemVieHolder.title.setTextColor(Color.parseColor("#ad0015"));

        } else {
            createSystemVieHolder.title.setText(readSystemBean.name);
            createSystemVieHolder.title.setTextColor(Color.parseColor("#ffffff"));
        }


    }
}
