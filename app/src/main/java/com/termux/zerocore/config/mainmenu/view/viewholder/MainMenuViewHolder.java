package com.termux.zerocore.config.mainmenu.view.viewholder;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.termux.R;

public class MainMenuViewHolder extends RecyclerView.ViewHolder {
    public RecyclerView mItemMenuRec;
    public TextView mTitle;

    public MainMenuViewHolder(@NonNull View itemView) {
        super(itemView);
        mItemMenuRec = itemView.findViewById(R.id.item_menu_rec);
        mTitle = itemView.findViewById(R.id.title);
    }
}
