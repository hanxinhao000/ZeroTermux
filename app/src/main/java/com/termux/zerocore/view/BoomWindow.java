package com.termux.zerocore.view;

import android.content.Context;
import android.util.TypedValue;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.xh_lib.utils.UUtils;
import com.google.gson.Gson;
import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.zerocore.activity.adapter.BoomMinLAdapter;
import com.termux.zerocore.bean.MinLBean;
import com.termux.zerocore.dialog.CommandDialog;
import com.termux.zerocore.utils.SaveData;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


/**
 * @author ZEL
 * @create By ZEL on 2020/12/23 15:19
 **/
public class BoomWindow {


    public TextView title;
    public RecyclerView recyclerView;
    public CardView qiehuan_mingl_zidong;
    public CardView qiehuan_command_zidong;
    public TextView qie_huan_string;
    public EditText file_name;
    public LinearLayout search123456;
    public LinearLayout popu_windows_jianpan;
    public LinearLayout popu_windows_huihua;
    private View mView;


    public int high = 0;

    public int getHigh(){


            return dp2px(UUtils.getContext(),40);



    }

    public View getView(BoomMinLAdapter.CloseLiftListener closeLiftListener, TermuxActivity termuxActivity, PopupWindow popupWindow){


        mView = UUtils.getViewLay(R.layout.dialog_boom);

        calculateViewMeasure(mView);

        title = mView.findViewById(R.id.title);
        recyclerView = mView.findViewById(R.id.recyclerView);
        qiehuan_mingl_zidong = mView.findViewById(R.id.qiehuan_mingl_zidong);
        qie_huan_string = mView.findViewById(R.id.qie_huan_string);
        file_name = mView.findViewById(R.id.file_name);
        search123456 = mView.findViewById(R.id.search123456);
        qiehuan_command_zidong = mView.findViewById(R.id.qiehuan_command_zidong);


        qiehuan_command_zidong.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                popupWindow.dismiss();
                //命令
                CommandDialog commandDialog = new CommandDialog(termuxActivity);
                commandDialog.show();
                commandDialog.setCancelable(false);

            }
        });


        popu_windows_jianpan = mView.findViewById(R.id.popu_windows_jianpan);


        popu_windows_huihua = mView.findViewById(R.id.popu_windows_huihua);

        search123456.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String string = file_name.getText().toString();
                if(string == null && string.isEmpty()){
                    UUtils.showMsg(UUtils.getString(R.string.文件名不能为空));
                    return;
                }

                TermuxActivity.mTerminalView.sendTextToTerminal("find / -name " + string);

                popupWindow.dismiss();

            }
        });


        qiehuan_mingl_zidong.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String zidong1226 = SaveData.getData("zidong1226");
                if (zidong1226 == null || zidong1226.isEmpty() || zidong1226.equals("def")){

                    SaveData.saveData("zidong1226","123456");

                    qie_huan_string.setText(UUtils.getString(R.string.当前为自动));
                    UUtils.showMsg(UUtils.getString(R.string.切换成功));


                    popupWindow.dismiss();


                }else{
                    SaveData.saveData("zidong1226","def");
                    UUtils.showMsg(UUtils.getString(R.string.切换成功));
                    qie_huan_string.setText(UUtils.getString(R.string.当前为自动));

                    popupWindow.dismiss();
                }
            }
        });

        String zidong12261 = SaveData.getData("zidong1226");
        if (zidong12261 == null || zidong12261.isEmpty() || zidong12261.equals("def")){
            qie_huan_string.setText(UUtils.getString(R.string.当前为自动));
        }else{
            qie_huan_string.setText(UUtils.getString(R.string.当前为自动));

        }

        String zidong1226 = SaveData.getData("zidong1226");
        if (zidong1226 == null || zidong1226.isEmpty() || zidong1226.equals("def")){


            String commi22 = SaveData.getData("commi22");
            if (commi22 == null || commi22.isEmpty() || commi22.equals("def")) {

                title.setVisibility(View.VISIBLE);

                title.setText(UUtils.getString(R.string.没有找到命令));


            }else{

                try {

                    MinLBean minLBean = new Gson().fromJson(commi22, MinLBean.class);
                    if(minLBean.data.list.size() == 0){

                        title.setVisibility(View.VISIBLE);
                        title.setText(UUtils.getString(R.string.没有找到命令));

                    }else {
                        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(UUtils.getContext());
                        linearLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
                        recyclerView.setLayoutManager(linearLayoutManager);
                        BoomMinLAdapter boomMinLAdapter = new BoomMinLAdapter(minLBean.data.list, null);
                        boomMinLAdapter.setCloseLiftListener(closeLiftListener);
                        recyclerView.setAdapter(boomMinLAdapter);
                        title.setVisibility(View.GONE);
                    }
                }catch (Exception e){
                    title.setVisibility(View.VISIBLE);
                    title.setText(UUtils.getString(R.string.命令出错));
                    e.printStackTrace();
                }

            }

        }else{
            //自动




        }









        return mView;

    }


    private  void calculateViewMeasure(View view) {
        int w = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.EXACTLY);
        int h = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.EXACTLY);
        view.measure(w, h);
    }

    public  int px2dip(Context context, float pxValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (pxValue / scale + 0.5f);
    }

    public  int dp2px(Context context, int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
            context.getResources().getDisplayMetrics());
    }
}
