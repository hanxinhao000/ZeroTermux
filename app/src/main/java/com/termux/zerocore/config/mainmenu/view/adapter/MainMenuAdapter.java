package com.termux.zerocore.config.mainmenu.view.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.termux.R;
import com.termux.zerocore.config.mainmenu.data.MainMenuCategoryData;
import com.termux.zerocore.config.mainmenu.view.viewholder.MainMenuViewHolder;
import com.termux.zerocore.ftp.utils.UserSetManage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class MainMenuAdapter extends RecyclerView.Adapter<MainMenuViewHolder> {
    private static String TAG = MainMenuAdapter.class.getSimpleName();
    private Context mContext;
    private ArrayList<MainMenuCategoryData> mMainMenuCategoryData;
    private HashMap<Integer, MainMenuItemAdapter> mainMenuItemAdapters;

    public MainMenuAdapter(Context context, ArrayList<MainMenuCategoryData> mainMenuCategoryData) {
        mContext = context;
        mMainMenuCategoryData = mainMenuCategoryData;
        mainMenuItemAdapters = new HashMap<>();
    }

    @NonNull
    @Override
    public MainMenuViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MainMenuViewHolder(LayoutInflater.from(mContext).inflate(R.layout.layout_menu_list, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull MainMenuViewHolder holder, int position) {
        int id = mMainMenuCategoryData.get(position).mId;
        holder.mTitle.setText(mMainMenuCategoryData.get(position).mTitle);
        holder.mItemMenuRec.setLayoutManager(new GridLayoutManager(mContext, 3));
        MainMenuItemAdapter mainMenuItemAdapter = new MainMenuItemAdapter(mContext, mMainMenuCategoryData.get(position).mClickArrayList);
        mainMenuItemAdapters.put(position, mainMenuItemAdapter);
        holder.mItemMenuRec.setAdapter(mainMenuItemAdapter);
        if (!UserSetManage.Companion.get().getZTUserBean().isCloseFoldMenu()) {
            holder.mOpenSettings.setVisibility(View.VISIBLE);
            boolean mainMenuItemShow = UserSetManage.Companion.get().getMainMenuItemShow(String.valueOf(id));
            Log.i(TAG, "onBindViewHolder mainMenuItemShow: " + mainMenuItemShow);
            if (mainMenuItemShow) {
                holder.mOpenSettings.setRotation(180);
                holder.mItemMenuRec.setVisibility(View.VISIBLE);
            } else {
                holder.mOpenSettings.setRotation(0);
                holder.mItemMenuRec.setVisibility(View.GONE);
            }
            holder.itemView.setOnClickListener(v -> {
                int visibility = holder.mItemMenuRec.getVisibility();
                if (visibility == View.VISIBLE) {
                    holder.mOpenSettings.setRotation(0);
                    holder.mItemMenuRec.setVisibility(View.GONE);
                    UserSetManage.Companion.get().setMainMenuItemShow(
                        String.valueOf(mMainMenuCategoryData.get(position).mId), UserSetManage.Companion.getITEM_GEON());
                } else {
                    holder.mOpenSettings.setRotation(180);
                    holder.mItemMenuRec.setVisibility(View.VISIBLE);
                    UserSetManage.Companion.get().setMainMenuItemShow(
                        String.valueOf(mMainMenuCategoryData.get(position).mId), UserSetManage.Companion.getITEM_VISIBLE());
                }
            });
        } else {
            holder.mOpenSettings.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return mMainMenuCategoryData.size();
    }

    public void release() {
        mContext = null;
        // 释放所有引用的 context 防止造成内存泄漏
        for (Map.Entry<Integer, MainMenuItemAdapter> entry : mainMenuItemAdapters.entrySet()) {
            entry.getValue().release();
        }
        mainMenuItemAdapters.clear();

        // 调用并且释放 Config 里边的release
        for (int i = 0; i < mMainMenuCategoryData.size(); i++) {
            MainMenuCategoryData mainMenuCategoryData = mMainMenuCategoryData.get(i);
            for (int j = 0; j < mainMenuCategoryData.mClickArrayList.size(); j++) {
                mainMenuCategoryData.mClickArrayList.get(j).release();
            }
        }
    }
}
