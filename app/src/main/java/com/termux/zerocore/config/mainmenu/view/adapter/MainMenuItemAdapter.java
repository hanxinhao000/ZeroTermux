package com.termux.zerocore.config.mainmenu.view.adapter;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.xh_lib.utils.LogUtils;
import com.termux.R;
import com.termux.zerocore.config.mainmenu.config.MainMenuClickConfig;
import com.termux.zerocore.config.mainmenu.view.viewholder.MainMenuItemViewHolder;

import java.util.ArrayList;

public class MainMenuItemAdapter extends RecyclerView.Adapter<MainMenuItemViewHolder> {
    private static final String TAG = MainMenuItemAdapter.class.getSimpleName();
    private Context mContext;
    private ArrayList<MainMenuClickConfig> mMainMenuClickConfigs;

    public MainMenuItemAdapter(Context context, ArrayList<MainMenuClickConfig> mainMenuClickConfigs) {
        mContext = context;
        mMainMenuClickConfigs = mainMenuClickConfigs;
    }

    @NonNull
    @Override
    public MainMenuItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MainMenuItemViewHolder(LayoutInflater.from(mContext).inflate(R.layout.item_main_menu_click, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull MainMenuItemViewHolder holder, int position) {
        MainMenuClickConfig mainMenuClickConfig = mMainMenuClickConfigs.get(position);
        holder.mCodeImage.setImageDrawable(mainMenuClickConfig.getIcon(mContext));
        String xmlString = mainMenuClickConfig.getXmlString(mContext);
        if (!TextUtils.isEmpty(xmlString)) {
            holder.mCodeTitle.setText(xmlString);
        } else {
            holder.mCodeTitle.setText(mainMenuClickConfig.getString(mContext));
        }

        holder.mDisableIco.setVisibility(mainMenuClickConfig.isShowDisableIco() ? View.VISIBLE : View.INVISIBLE);
        holder.mItemlayout.setOnClickListener(view -> {
                LogUtils.e(TAG, "onBindViewHolder click ");
                mainMenuClickConfig.initContext(mContext);
                mainMenuClickConfig.onClick(view, mContext);
            }
        );
        holder.mItemlayout.setOnLongClickListener(view -> {
            return mainMenuClickConfig.onLongClick(view, mContext);
        });
        mainMenuClickConfig.setImageView(holder.mCodeImage);
        mainMenuClickConfig.setTextView(holder.mCodeTitle);
        mainMenuClickConfig.initViewStatus(mContext);
    }

    @Override
    public int getItemCount() {
        return mMainMenuClickConfigs.size();
    }

    public void release() {
        mContext = null;
    }
}
