package com.termux.zerocore.activity.view_holder;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.termux.R;



/**
 * @author ZEL
 * @create By ZEL on 2020/12/17 14:42
 **/
public class MinLViewHolder extends RecyclerView.ViewHolder {

    public TextView name;
    public TextView value;
    public CardView item_card;
    public ImageView enter;
    public ImageView pin_top;
    public MinLViewHolder(@NonNull View itemView) {
        super(itemView);
        name = itemView.findViewById(R.id.name);
        value = itemView.findViewById(R.id.value);
        item_card = itemView.findViewById(R.id.item_card);
        enter = itemView.findViewById(R.id.enter);
        pin_top = itemView.findViewById(R.id.pin_top);
    }
}
