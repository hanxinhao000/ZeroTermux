package com.example.xh_lib.base;

/**
 * @author ZEL
 * @create By ZEL on 2020/4/7 15:10
 **/
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

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

        try {
            initView(position, mList.get(position), viewHolder);
        }catch (Exception e){
            e.printStackTrace();
        }


        return convertView;
    }


    public abstract ViewHolder getViewHolder();

    public abstract void initView(int position, T t, ViewHolder viewHolder);


}
