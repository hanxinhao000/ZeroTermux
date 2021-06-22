package com.termux.zerocore.dialog;

import android.content.Context;
import android.content.res.AssetManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import com.example.xh_lib.utils.UUtils;
import com.termux.R;

import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;



/**
 * @author ZEL
 * @create By ZEL on 2021/2/23 10:40
 **/
public class CommandDialog extends BaseDialogCentre {

    private EditText text_input;
    private RecyclerView recyclerView;
    private ArrayList<String> cmdList;
    private ArrayList<String> cmdListTemp;
    private CommandAdapter commandAdapter;
    private ImageView close_img;
    private ImageView fanhui;
    private LinearLayout search;
    private LinearLayout web_show;
    private WebView web_view;

    public CommandDialog(@NonNull Context context) {
        super(context);
    }

    public CommandDialog(@NonNull Context context, int themeResId) {
        super(context, themeResId);
    }

    @Override
    void initViewDialog(View mView) {
        text_input = mView.findViewById(R.id.text_input);
        cmdList = new ArrayList<>();
        cmdListTemp = new ArrayList<>();
        recyclerView = mView.findViewById(R.id.recyclerView);
        close_img = mView.findViewById(R.id.close_img);
        search = mView.findViewById(R.id.search);
        web_show = mView.findViewById(R.id.web_show);
        web_view = mView.findViewById(R.id.web_view);
        fanhui = mView.findViewById(R.id.fanhui);

        WebSettings settings = web_view.getSettings();
        settings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        fanhui.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                search.setVisibility(View.VISIBLE);
                web_show.setVisibility(View.GONE);
                fanhui.setVisibility(View.GONE);
            }
        });

        close_img.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
        initData();
    }

    @Override
    int getContentView() {
        return R.layout.dialog_command;
    }

    //初始化数据
    private void initData(){
        cmdList.clear();
        AssetManager assetManager = mContext.getAssets();
        if (assetManager != null) {
            try {
                cmdList.addAll(Arrays.asList(assetManager.list("cmd")));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }


        recyclerView.setLayoutManager(new LinearLayoutManager(mContext));

        cmdListTemp.addAll(cmdList);
        commandAdapter = new CommandAdapter(cmdListTemp);

        recyclerView.setAdapter(commandAdapter);


        text_input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {


                UUtils.runOnThread(new Runnable() {
                    @Override
                    public void run() {
                        if(s.toString().isEmpty()){
                            //为空
                            cmdListTemp.clear();
                            cmdListTemp.addAll(cmdList);
                        }else{
                            //不为空
                            cmdListTemp.clear();
                            for (int i = 0; i < cmdList.size(); i++) {

                                if (cmdList.get(i).contains(s)) {
                                    cmdListTemp.add(cmdList.get(i));
                                    cmdListTemp.sort(new Comparator<String>() {
                                        @Override
                                        public int compare(String o1, String o2) {
                                            return ((String) o1).toUpperCase().compareTo(((String) o2).toUpperCase());
                                        }
                                    });
                                }


                            }

                        }

                        UUtils.runOnUIThread(new Runnable() {
                            @Override
                            public void run() {
                                commandAdapter.notifyDataSetChanged();
                            }
                        });
                    }
                });




            }
        });



    }


    class CommandViewHolder extends RecyclerView.ViewHolder{

        public TextView item_text;

        public CommandViewHolder(@NonNull View itemView) {

            super(itemView);

            item_text = itemView.findViewById(R.id.item_text);
        }
    }


    class CommandAdapter extends RecyclerView.Adapter<CommandViewHolder>{

        private ArrayList<String> commAndList;

        public CommandAdapter(ArrayList<String> commAndList){

            this.commAndList = commAndList;
        }

        @NonNull
        @Override
        public CommandViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new CommandViewHolder(UUtils.getViewLayViewGroup(R.layout.command_item,parent));
        }

        @Override
        public void onBindViewHolder(@NonNull CommandViewHolder holder, int position) {

            if(position != 0) {
                if (position % 2 == 0) {
                    holder.item_text.setBackgroundResource(R.drawable.shape_line_2e84e6);

                } else {

                    holder.item_text.setBackgroundResource(R.drawable.shape_line_8cff5a);
                }
            }else{

                holder.item_text.setBackgroundResource(R.drawable.shape_line_2e84e6);

            }

            holder.item_text.setText(commAndList.get(position));

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    search.setVisibility(View.GONE);
                    web_show.setVisibility(View.VISIBLE);
                    fanhui.setVisibility(View.VISIBLE);
                    showCmdDetail(commAndList.get(position));
                }
            });
        }

        @Override
        public int getItemCount() {
            return commAndList.size();
        }
    }


    private void showCmdDetail(String cmd) {

        try {
            AssetManager assetManager = UUtils.getContext().getAssets();
            InputStreamReader reader = new InputStreamReader(assetManager.open(String.format("cmd/%s", cmd)));
            Parser parser = Parser.builder().build();
            Node document = parser.parseReader(reader);
            HtmlRenderer renderer = HtmlRenderer.builder().build();
            web_view.loadData(renderer.render(document), "text/html; charset=UTF-8", null);
        } catch (IOException e) {
            e.printStackTrace();
            UUtils.showMsg(UUtils.getString(R.string.出错了));
        }
    }



}
