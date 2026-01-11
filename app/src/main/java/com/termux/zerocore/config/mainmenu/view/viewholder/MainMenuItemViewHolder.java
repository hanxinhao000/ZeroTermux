package com.termux.zerocore.config.mainmenu.view.viewholder;

import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.termux.R;

public class MainMenuItemViewHolder extends RecyclerView.ViewHolder {
    public ImageView mCodeImage;
    public TextView mCodeTitle;

    public LinearLayout mItemlayout;
    public MainMenuItemViewHolder(@NonNull View itemView) {
        super(itemView);
        mCodeImage = itemView.findViewById(R.id.code_image);
        mCodeTitle = itemView.findViewById(R.id.code_title);
        mItemlayout = itemView.findViewById(R.id.item_layout);
    }
}
