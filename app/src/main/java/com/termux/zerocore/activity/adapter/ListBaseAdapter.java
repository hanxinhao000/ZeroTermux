package com.termux.zerocore.activity.adapter;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.termux.zerocore.activity.view_holder.ViewHolder;

import java.util.List;

public abstract class ListBaseAdapter<T> extends BaseAdapter {

    public List<T> mList;

    public ListBaseAdapter(List<T> list) {
        mList = list;
    }

    @Override
    public int getCount() {
        return mList.size();
    }

    @Override
    public Object getItem(int position) {
        return mList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder = null;
        if (convertView == null) {
            viewHolder = getViewHolder();

            convertView = viewHolder.getView();

            convertView.setTag(viewHolder);


        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        initView(position, mList.get(position), viewHolder);


        return convertView;
    }


    public abstract ViewHolder getViewHolder();

    public abstract void initView(int position, T t, ViewHolder viewHolder);


}
