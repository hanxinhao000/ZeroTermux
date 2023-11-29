package com.termux.zerocore.activity.adapter;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.xh_lib.utils.UUtils;
import com.google.gson.Gson;
import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.zerocore.activity.view_holder.MinLViewHolder;
import com.termux.zerocore.bean.MinLBean;
import com.termux.zerocore.dialog.MingLShowDialog;
import com.termux.zerocore.dialog.MinglingDialog;
import com.termux.zerocore.utils.SaveData;

import java.util.List;


/**
 * @author ZEL
 * @create By ZEL on 2020/12/17 14:42
 **/
public class MinLAdapter extends RecyclerView.Adapter<MinLViewHolder> {

    private List<MinLBean.DataNum> dataNum;

    private  Activity activity;

    public MinLAdapter(List<MinLBean.DataNum> dataNum, Activity activity){

        this.dataNum = dataNum;
        this.activity = activity;

    }

    @NonNull
    @Override
    public MinLViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MinLViewHolder(UUtils.getViewLayViewGroup(R.layout.item_migl, parent));
    }

    @Override
    public void onBindViewHolder(@NonNull MinLViewHolder holder, int position) {

        MinLViewHolder minLViewHolder = holder;

        minLViewHolder.name.setText(dataNum.get(position).name);
        minLViewHolder.value.setText(dataNum.get(position).value);


        if(dataNum.get(position).isChecked){
            minLViewHolder.enter.setVisibility(View.VISIBLE);
        }else{
            minLViewHolder.enter.setVisibility(View.GONE);
        }


        minLViewHolder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mCloseLiftListener!= null){
                    mCloseLiftListener.close();
                }
                if(dataNum.get(position).isChecked){
                    TermuxActivity.mTerminalView.sendTextToTerminal(dataNum.get(position).value + " \n");
                }else{
                    TermuxActivity.mTerminalView.sendTextToTerminal(dataNum.get(position).value );
                }

            }
        });

        if(activity != null) {
            minLViewHolder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {


                    MinglingDialog minglingDialog = new MinglingDialog(activity);
                    minglingDialog.show();

                    minglingDialog.xiugai.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {

                            minglingDialog.dismiss();


                            MingLShowDialog mingLShowDialog = new MingLShowDialog(activity);
                            mingLShowDialog.show();

                            mingLShowDialog.name_edit.setText(dataNum.get(position).name);
                            mingLShowDialog.edit_text.setText(dataNum.get(position).value);
                            mingLShowDialog.switch_btn.setChecked(dataNum.get(position).isChecked);


                            mingLShowDialog.start.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {


                                    String nameString = mingLShowDialog.name_edit.getText().toString();
                                    String commitString = mingLShowDialog.edit_text.getText().toString();
                                    if (nameString == null || nameString.isEmpty()) {
                                        UUtils.showMsg(UUtils.getString(R.string.名称不能为空));
                                        return;
                                    }
                                    if (commitString == null || commitString.isEmpty()) {
                                        UUtils.showMsg(UUtils.getString(R.string.命令不能为空));
                                        return;
                                    }


                                    try {
                                        String commi22 = SaveData.getData("commi22");
                                        MinLBean minLBean = new Gson().fromJson(commi22, MinLBean.class);
                                        List<MinLBean.DataNum> list = minLBean.data.list;


                                        MinLBean.DataNum dataNum = list.get(position);

                                        dataNum.name = mingLShowDialog.name_edit.getText().toString();
                                        dataNum.value = mingLShowDialog.edit_text.getText().toString();
                                        dataNum.isChecked = mingLShowDialog.isChecked;

                                        list.remove(position);
                                        list.add(position, dataNum);

                                        String s = new Gson().toJson(minLBean);
                                        SaveData.saveData("commi22", s);
                                        UUtils.showMsg(UUtils.getString(R.string.修改成功));
                                        if (mSXListener != null) {
                                            mSXListener.sx();
                                        }
                                        mingLShowDialog.dismiss();
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        UUtils.showMsg(UUtils.getString(R.string.请再试一次));
                                        mingLShowDialog.dismiss();
                                    }


                                }
                            });


                     /*
                        minglingDialog.dismiss();*/
                        }
                    });

                    minglingDialog.delete.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {


                            String commi22 = SaveData.getData("commi22");
                            MinLBean minLBean = new Gson().fromJson(commi22, MinLBean.class);
                            List<MinLBean.DataNum> list = minLBean.data.list;


                            list.remove(position);


                            String s = new Gson().toJson(minLBean);
                            SaveData.saveData("commi22", s);


                            UUtils.showMsg(UUtils.getString(R.string.删除成功));
                            if (mSXListener != null) {
                                mSXListener.sx();
                            }
                            minglingDialog.dismiss();
                        }
                    });


                    return true;
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return dataNum.size();
    }

    private CloseLiftListener mCloseLiftListener;

    public void setCloseLiftListener(CloseLiftListener mCloseLiftListener){
        this.mCloseLiftListener = mCloseLiftListener;
    }


    private SXListener mSXListener;

    public void setSXListener(SXListener mSXListener){
        this.mSXListener = mSXListener;
    }
    public interface CloseLiftListener{

        void close();
    }

    public interface SXListener{

        void sx();
    }
}
