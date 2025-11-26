package com.termux.zerocore.view;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.TextUtils;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.xh_lib.utils.LogUtils;
import com.example.xh_lib.utils.UUtils;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.zerocore.activity.adapter.BoomMinLAdapter;
import com.termux.zerocore.activity.adapter.SSHAdapter;
import com.termux.zerocore.bean.MinLBean;
import com.termux.zerocore.bean.SSHDeviceBean;
import com.termux.zerocore.dialog.CommandDialog;
import com.termux.zerocore.utils.SaveData;
import com.termux.zerocore.utils.WindowsUtils;
import com.termux.zerocore.utils.SSHKeyUtils;

import java.io.File;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;


/**
 * @author ZEL
 * @create By ZEL on 2020/12/23 15:19
 **/
public class BoomWindow {
    public static final int REQUEST_CODE_IMPORT_KEY = 9001;
    public static String PENDING_IMPORT_ALIAS = "";

    public static String TAG = "BoomWindow";
    private static final String KEY_SSH_LIST = "ssh_device_list";
    private static boolean isInstallingEnv = false;
    public static boolean SWITCH = true;

    public TextView title;
    public RecyclerView recyclerView;
    public CardView qiehuan_mingl_zidong;
    public CardView qiehuan_command_zidong;
    public TextView qie_huan_string;
    public EditText file_name;
    public LinearLayout search123456;
    public LinearLayout popu_windows_jianpan;
    public LinearLayout popu_windows_huihua;
    public LinearLayout popu_windows_ssh;
    public LinearLayout layout_add_ssh;

    private View mView;
    private PopupWindow mPopupWindow;
    private TermuxActivity mTermuxActivity;

    public int getHigh(){
        return WindowsUtils.INSTANCE.dp2px(UUtils.getContext(),DiaLogData.BOOM_WINDOWS_OPEN_HEIGHT);
    }

    public View getView(BoomMinLAdapter.CloseLiftListener closeLiftListener, TermuxActivity termuxActivity, PopupWindow popupWindow){
        this.mPopupWindow = popupWindow;
        this.mTermuxActivity = termuxActivity;
        mView = UUtils.getViewLay(R.layout.dialog_boom);
        calculateViewMeasure(mView);
        title = mView.findViewById(R.id.title);
        recyclerView = mView.findViewById(R.id.recyclerView);
        qiehuan_mingl_zidong = mView.findViewById(R.id.qiehuan_mingl_zidong);
        qie_huan_string = mView.findViewById(R.id.qie_huan_string);
        file_name = mView.findViewById(R.id.file_name);
        search123456 = mView.findViewById(R.id.search123456);
        qiehuan_command_zidong = mView.findViewById(R.id.qiehuan_command_zidong);
        popu_windows_jianpan = mView.findViewById(R.id.popu_windows_jianpan);
        popu_windows_huihua = mView.findViewById(R.id.popu_windows_huihua);
        popu_windows_ssh = mView.findViewById(R.id.popu_windows_ssh);
        layout_add_ssh = mView.findViewById(R.id.layout_add_ssh);
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
        //SSH按钮点击事件
        if (popu_windows_ssh != null) {
            popu_windows_ssh.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (layout_add_ssh != null) layout_add_ssh.setVisibility(View.VISIBLE);
                    title.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);

                    String binPath = "/data/data/com.termux/files/usr/bin/";
                    File sshFile = new File(binPath, "ssh");
                    File sshPassFile = new File(binPath, "sshpass");

                    if (sshFile.exists() && sshPassFile.exists()) {
                        isInstallingEnv = false;
                    } else {
                        if (isInstallingEnv) {
                            UUtils.showMsg("正在初始化中，请耐心等待...");
                        } else {
                            isInstallingEnv = true;
                            UUtils.showMsg("初次使用，正在初始化SSH环境...");
                            String installCmd = " { command -v ssh >/dev/null || pkg install openssh -y >/dev/null 2>&1; } && { command -v sshpass >/dev/null || pkg install sshpass -y >/dev/null 2>&1; }";
                            TermuxActivity.mTerminalView.sendTextToTerminal(" " + installCmd + "\n");
                        }
                    }
                    showSSHList(closeLiftListener);
                }
            });
        }
        if (layout_add_ssh != null) {
            layout_add_ssh.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String binPath = "/data/data/com.termux/files/usr/bin/";
                    File sshFile = new File(binPath, "ssh");
                    File sshPassFile = new File(binPath, "sshpass");

                    if (!sshFile.exists() || !sshPassFile.exists()) {
                        UUtils.showMsg("环境尚未就绪，请稍候...");
                        return;
                    }
                    isInstallingEnv = false;

                    showAddSSHDialog(closeLiftListener);
                }
            });
        }

        search123456.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String string = file_name.getText().toString();
                if(string == null || string.isEmpty()){
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
                if (layout_add_ssh != null) {
                    layout_add_ssh.setVisibility(View.GONE);
                }
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
        showBoomView(closeLiftListener);

        return mView;
    }

    private void showBoomView (BoomMinLAdapter.CloseLiftListener closeLiftListener) {
        if (layout_add_ssh != null) layout_add_ssh.setVisibility(View.GONE);
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
                        topSorting(minLBean.data.list);
                        title.setVisibility(View.GONE);
                        recyclerView.setVisibility(View.VISIBLE);
                        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(UUtils.getContext());
                        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
                        recyclerView.setLayoutManager(linearLayoutManager);
                        BoomMinLAdapter boomMinLAdapter = new BoomMinLAdapter(minLBean.data.list, null);
                        boomMinLAdapter.setCloseLiftListener(closeLiftListener);
                        boomMinLAdapter.setPinTopListener(new BoomMinLAdapter.PinTopListener() {
                            @Override
                            public void itemPinTopRefresh() {
                                topSorting(minLBean.data.list);
                                boomMinLAdapter.notifyDataSetChanged();
                                UUtils.runOnThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        saveCommandString(minLBean.data.list);
                                    }
                                });
                            }
                        });
                        recyclerView.setAdapter(boomMinLAdapter);
                    }
                }catch (Exception e){
                    title.setVisibility(View.VISIBLE);
                    title.setText(UUtils.getString(R.string.命令出错));
                    LogUtils.d(TAG, e.toString());
                }
            }
        }else{
            //自动
        }
    }

    private  void calculateViewMeasure(View view) {
        int w = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.EXACTLY);
        int h = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.EXACTLY);
        view.measure(w, h);
    }

    private void saveCommandString(List<MinLBean.DataNum> list) {
        MinLBean minLBean = new MinLBean();
        MinLBean.Data data = new MinLBean.Data();
        data.list = list;
        minLBean.data = data;
        String s = new Gson().toJson(minLBean);
        SaveData.saveData("commi22", s);
    }

    private List<MinLBean.DataNum> topSorting(List<MinLBean.DataNum> list) {
        List<MinLBean.DataNum> tempList = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            MinLBean.DataNum dataNum = list.get(i);
            if (dataNum.isPinTop) {
                tempList.add(dataNum);
                list.remove(i);
                i--;
            }
        }
        list.addAll(0, tempList);

        return list;
    }
    //快捷ssh
    private void showSSHList(BoomMinLAdapter.CloseLiftListener closeLiftListener) {
        String json = SaveData.getData(KEY_SSH_LIST);
        List<SSHDeviceBean> sshList = new ArrayList<>();
        if (!TextUtils.isEmpty(json) && !json.equals("def")) {
            try {
                Type listType = new TypeToken<List<SSHDeviceBean>>(){}.getType();
                sshList = new Gson().fromJson(json, listType);
            } catch (Exception e) {
                LogUtils.e(TAG, "SSH list parse error: " + e);
            }
        }

        if (sshList.isEmpty()) {
            title.setVisibility(View.VISIBLE);
            title.setText("暂无SSH设备，请点击上方添加");
            recyclerView.setVisibility(View.GONE);
            if (layout_add_ssh != null) layout_add_ssh.setVisibility(View.VISIBLE);
            return;
        } else {
            title.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(UUtils.getContext());
        recyclerView.setLayoutManager(linearLayoutManager);

        SSHAdapter sshAdapter = new SSHAdapter(sshList);
        List<SSHDeviceBean> finalSshList = sshList;

        sshAdapter.setOnSSHItemClickListener(new SSHAdapter.OnSSHItemClickListener() {
            //连接
            @Override
            public void onConnect(SSHDeviceBean bean) {
                String finalCmd = bean.generateConnectCommand();

                if (!android.text.TextUtils.isEmpty(bean.getPassword())) {
                    UUtils.showMsg("正在连接: " + bean.getAlias());
                }
                String runCmd = " clear && " + finalCmd;

                //屏蔽Connection refused用String runCmd = " clear && " + finalCmd + " 2>/dev/null || echo 'err'";

                TermuxActivity.mTerminalView.sendTextToTerminal(runCmd + "\n");

                if (mPopupWindow != null) {
                    mPopupWindow.dismiss();
                }
            }
            //删除
            @Override
            public void onDelete(int position, SSHDeviceBean bean) {
                AlertDialog.Builder builder = new AlertDialog.Builder(mTermuxActivity, android.R.style.Theme_DeviceDefault_Dialog);
                builder.setTitle("删除设备");
                builder.setMessage("确定要删除\"" + bean.getAlias() + "\"吗？\n(IP: " + bean.getHost() + ")");

                builder.setPositiveButton("删除", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finalSshList.remove(position);
                        SaveData.saveData(KEY_SSH_LIST, new Gson().toJson(finalSshList));
                        showSSHList(closeLiftListener);
                        UUtils.showMsg("已删除");
                    }
                });
                builder.setNegativeButton("取消", null);
                AlertDialog dialog = builder.create();
                dialog.show();
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(android.graphics.Color.parseColor("#FF5252"));
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(android.graphics.Color.LTGRAY);
            }
            //编辑
            @Override
            public void onEdit(int position, SSHDeviceBean bean) {
                showEditSSHDialog(closeLiftListener, position, bean);
            }
        });
        recyclerView.setAdapter(sshAdapter);
    }
    //添加设备的对话框
    private void showAddSSHDialog(BoomMinLAdapter.CloseLiftListener closeLiftListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(mTermuxActivity, android.R.style.Theme_DeviceDefault_Dialog);
        builder.setTitle("添加SSH设备");
        LinearLayout layout = new LinearLayout(mTermuxActivity);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);
        final EditText etAlias = new EditText(mTermuxActivity);
        etAlias.setHint("别名");
        etAlias.setSingleLine(true);
        etAlias.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_NEXT);
        layout.addView(etAlias);
        final EditText etHost = new EditText(mTermuxActivity);
        etHost.setHint("主机名(IP/域名)");
        etHost.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_URI);
        etHost.setSingleLine(true);
        etHost.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_NEXT);
        layout.addView(etHost);

        final EditText etPort = new EditText(mTermuxActivity);
        etPort.setHint("端口(默认22)");
        etPort.setText("22");
        etPort.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        etPort.setSingleLine(true);
        etPort.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_NEXT);
        layout.addView(etPort);

        final EditText etUser = new EditText(mTermuxActivity);
        etUser.setHint("用户名(默认root)");
        etUser.setText("root");
        etUser.setSingleLine(true);
        etUser.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_NEXT);
        layout.addView(etUser);

        final android.widget.CheckBox cbUseKey = new android.widget.CheckBox(mTermuxActivity);
        cbUseKey.setText("使用密钥登录(~/.ssh/别名.key)");
        cbUseKey.setTextColor(android.graphics.Color.WHITE);
        layout.addView(cbUseKey);

        final LinearLayout passwordLayout = new LinearLayout(mTermuxActivity);
        passwordLayout.setOrientation(LinearLayout.HORIZONTAL);
        passwordLayout.setGravity(Gravity.CENTER_VERTICAL);

        final EditText etPass = new EditText(mTermuxActivity);
        etPass.setHint("密码");
        LinearLayout.LayoutParams etParams = new LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        etPass.setLayoutParams(etParams);
        etPass.setSingleLine(true);
        etPass.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_DONE);
        etPass.setTransformationMethod(PasswordTransformationMethod.getInstance());

        final ImageView eyeIcon = new ImageView(mTermuxActivity);
        eyeIcon.setImageResource(android.R.drawable.ic_menu_view);
        eyeIcon.setColorFilter(android.graphics.Color.LTGRAY);
        eyeIcon.setPadding(20, 10, 10, 10);

        eyeIcon.setOnClickListener(new View.OnClickListener() {
            private boolean isShow = false;
            @Override
            public void onClick(View v) {
                isShow = !isShow;
                if (isShow) {
                    etPass.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                    eyeIcon.setColorFilter(android.graphics.Color.WHITE);
                } else {
                    etPass.setTransformationMethod(PasswordTransformationMethod.getInstance());
                    eyeIcon.setColorFilter(android.graphics.Color.LTGRAY);
                }
                etPass.setSelection(etPass.getText().length());
            }
        });

        passwordLayout.addView(etPass);
        passwordLayout.addView(eyeIcon);
        layout.addView(passwordLayout);

        //密钥处理
        final LinearLayout keyLayout = new LinearLayout(mTermuxActivity);
        keyLayout.setOrientation(LinearLayout.HORIZONTAL);
        keyLayout.setPadding(0, 20, 0, 0);
        keyLayout.setVisibility(View.GONE);

        android.widget.Button btnGenKey = new android.widget.Button(mTermuxActivity);
        btnGenKey.setText("生成");
        btnGenKey.setTextSize(12);

        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        btnGenKey.setLayoutParams(btnParams);

        android.widget.Button btnImportKey = new android.widget.Button(mTermuxActivity);
        btnImportKey.setText("导入");
        btnImportKey.setTextSize(12);
        btnImportKey.setLayoutParams(btnParams);

        android.widget.Button btnViewPub = new android.widget.Button(mTermuxActivity);
        btnViewPub.setText("复制公钥");
        btnViewPub.setTextSize(12);
        btnViewPub.setLayoutParams(btnParams);

        keyLayout.addView(btnGenKey);
        keyLayout.addView(btnImportKey);
        keyLayout.addView(btnViewPub);
        layout.addView(keyLayout);

        cbUseKey.setOnCheckedChangeListener(new android.widget.CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(android.widget.CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    passwordLayout.setVisibility(View.GONE);
                    keyLayout.setVisibility(View.VISIBLE);
                } else {
                    passwordLayout.setVisibility(View.VISIBLE);
                    keyLayout.setVisibility(View.GONE);
                }
            }
        });

        btnGenKey.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String currentAlias = etAlias.getText().toString().trim();
                if (android.text.TextUtils.isEmpty(currentAlias)) {
                    UUtils.showMsg("请先填写别名，密钥文件将以别名命名");
                    return;
                }
                new AlertDialog.Builder(mTermuxActivity)
                    .setTitle("生成密钥")
                    .setMessage("将生成新的RSA密钥对：\n~/.ssh/" + currentAlias.replaceAll("[^a-zA-Z0-9_\\-]", "_") + ".key\n\n如果该文件已存在，将被覆盖")
                    .setPositiveButton("生成", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            String cmd = SSHKeyUtils.getGenerateKeyCommand(currentAlias);
                            TermuxActivity.mTerminalView.sendTextToTerminal(cmd + "\n");
                            UUtils.showMsg("正在后台生成密钥...");
                        }
                    })
                    .setNegativeButton("取消", null)
                    .show();
            }
        });
        btnViewPub.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String currentAlias = etAlias.getText().toString().trim();
                if (android.text.TextUtils.isEmpty(currentAlias)) {
                    UUtils.showMsg("请先填写别名");
                    return;
                }

                File pubKeyFile = new File(SSHKeyUtils.getSSHDir(), currentAlias.replaceAll("[^a-zA-Z0-9_\\-]", "_") + ".key.pub");
                if (!pubKeyFile.exists()) {
                    UUtils.showMsg("公钥文件不存在，请先点击生成密钥");
                    return;
                }

                try {
                    java.io.FileInputStream fis = new java.io.FileInputStream(pubKeyFile);
                    byte[] data = new byte[(int) pubKeyFile.length()];
                    fis.read(data);
                    fis.close();
                    String pubKeyContent = new String(data, "UTF-8");

                    android.content.ClipboardManager cm = (android.content.ClipboardManager) mTermuxActivity.getSystemService(Context.CLIPBOARD_SERVICE);
                    android.content.ClipData mClipData = android.content.ClipData.newPlainText("SSH Public Key", pubKeyContent);
                    cm.setPrimaryClip(mClipData);

                    UUtils.showMsg("公钥已复制到剪贴板");
                } catch (Exception e) {
                    UUtils.showMsg("读取失败:" + e.getMessage());
                }
            }
        });
        //导入密钥
        btnImportKey.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String currentAlias = etAlias.getText().toString().trim();
                if (android.text.TextUtils.isEmpty(currentAlias)) {
                    UUtils.showMsg("先填写别名，需要根据别名保存密钥文件");
                    return;
                }

                if (isAliasExist(currentAlias, -1)) {
                    UUtils.showMsg("别名已存在，无法导入，请修改别名");
                    return;
                }

                PENDING_IMPORT_ALIAS = currentAlias;

                android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_GET_CONTENT);
                intent.setType("*/*");
                intent.addCategory(android.content.Intent.CATEGORY_OPENABLE);
                mTermuxActivity.startActivityForResult(intent, REQUEST_CODE_IMPORT_KEY);
            }
        });

        builder.setView(layout);

        builder.setPositiveButton("保存", null);
        builder.setNegativeButton("取消", null);

        AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(android.graphics.Color.WHITE);
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(android.graphics.Color.LTGRAY);

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String alias = etAlias.getText().toString().trim();
                String host = etHost.getText().toString().trim();
                String portStr = etPort.getText().toString().trim();

                if (android.text.TextUtils.isEmpty(alias)) {
                    UUtils.showMsg("请输入设备别名");
                    return;
                }
                if (android.text.TextUtils.isEmpty(host)) {
                    UUtils.showMsg("请输入主机地址");
                    return;
                }
                if (isAliasExist(alias, -1)) {
                    UUtils.showMsg("别名\"" + alias + "\"已存在，请更换");
                    return;
                }

                boolean isIp = android.util.Patterns.IP_ADDRESS.matcher(host).matches();
                boolean isDomain = android.util.Patterns.DOMAIN_NAME.matcher(host).matches();
                boolean isLocal = "localhost".equalsIgnoreCase(host);
                if (!isIp && !isDomain && !isLocal) {
                    UUtils.showMsg("主机地址格式错误");
                    return;
                }

                int port = 22;
                try {
                    port = Integer.parseInt(portStr);
                    if (port < 1 || port > 65535) {
                        UUtils.showMsg("端口无效(1-65535)");
                        return;
                    }
                } catch (Exception e) {
                    UUtils.showMsg("端口必须是数字");
                    return;
                }

                SSHDeviceBean bean = new SSHDeviceBean(
                    alias,
                    host,
                    port,
                    etUser.getText().toString().trim(),
                    etPass.getText().toString().trim(),
                    cbUseKey.isChecked()
                );

                saveNewSSHDevice(bean);
                showSSHList(closeLiftListener);

                dialog.dismiss();
                UUtils.showMsg("添加成功");
            }
        });
    }
    //校验别名
    private boolean isAliasExist(String alias, int excludePosition) {
        String json = SaveData.getData(KEY_SSH_LIST);
        if (TextUtils.isEmpty(json) || json.equals("def")) return false;

        try {
            Type listType = new TypeToken<List<SSHDeviceBean>>(){}.getType();
            List<SSHDeviceBean> list = new Gson().fromJson(json, listType);

            for (int i = 0; i < list.size(); i++) {
                if (i == excludePosition) continue;

                if (list.get(i).getAlias().equals(alias)) {
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
    private void saveNewSSHDevice(SSHDeviceBean newDevice) {
        String json = SaveData.getData(KEY_SSH_LIST);
        List<SSHDeviceBean> list = new ArrayList<>();
        if (!TextUtils.isEmpty(json) && !json.equals("def")) {
            try {
                Type listType = new TypeToken<List<SSHDeviceBean>>(){}.getType();
                list = new Gson().fromJson(json, listType);
            } catch (Exception e) {}
        }
        list.add(newDevice);
        SaveData.saveData(KEY_SSH_LIST, new Gson().toJson(list));
    }
    //编辑SSH对话框
    private void showEditSSHDialog(BoomMinLAdapter.CloseLiftListener closeLiftListener, final int position, final SSHDeviceBean oldBean) {
        AlertDialog.Builder builder = new AlertDialog.Builder(mTermuxActivity, android.R.style.Theme_DeviceDefault_Dialog);
        builder.setTitle("编辑SSH设备");

        LinearLayout layout = new LinearLayout(mTermuxActivity);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);

        final EditText etAlias = new EditText(mTermuxActivity);
        etAlias.setHint("别名");
        etAlias.setSingleLine(true);
        etAlias.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_NEXT);
        etAlias.setText(oldBean.getAlias());
        layout.addView(etAlias);

        final EditText etHost = new EditText(mTermuxActivity);
        etHost.setHint("主机名(IP/域名)");
        etHost.setSingleLine(true);
        etHost.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_NEXT);
        etHost.setText(oldBean.getHost());
        layout.addView(etHost);

        final EditText etPort = new EditText(mTermuxActivity);
        etPort.setHint("端口");
        etPort.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        etPort.setSingleLine(true);
        etPort.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_NEXT);
        etPort.setText(String.valueOf(oldBean.getPort()));
        layout.addView(etPort);

        final EditText etUser = new EditText(mTermuxActivity);
        etUser.setHint("用户名");
        etUser.setSingleLine(true);
        etUser.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_NEXT);
        etUser.setText(oldBean.getUsername());
        layout.addView(etUser);

        final android.widget.CheckBox cbUseKey = new android.widget.CheckBox(mTermuxActivity);
        cbUseKey.setText("使用密钥登录(~/.ssh/别名.key)");
        cbUseKey.setChecked(oldBean.isUseKey());
        cbUseKey.setTextColor(android.graphics.Color.WHITE);
        layout.addView(cbUseKey);

        final LinearLayout passwordLayout = new LinearLayout(mTermuxActivity);
        passwordLayout.setOrientation(LinearLayout.HORIZONTAL);
        passwordLayout.setGravity(Gravity.CENTER_VERTICAL);

        final EditText etPass = new EditText(mTermuxActivity);
        etPass.setHint("密码");
        LinearLayout.LayoutParams etParams = new LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        etPass.setLayoutParams(etParams);
        etPass.setSingleLine(true);
        etPass.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_DONE);
        etPass.setTransformationMethod(android.text.method.PasswordTransformationMethod.getInstance());
        etPass.setText(oldBean.getPassword());

        final ImageView eyeIcon = new ImageView(mTermuxActivity);
        eyeIcon.setImageResource(android.R.drawable.ic_menu_view);
        eyeIcon.setColorFilter(android.graphics.Color.LTGRAY);
        eyeIcon.setPadding(20, 10, 10, 10);

        eyeIcon.setOnClickListener(new View.OnClickListener() {
            private boolean isShow = false;
            @Override
            public void onClick(View v) {
                isShow = !isShow;
                if (isShow) {
                    etPass.setTransformationMethod(android.text.method.HideReturnsTransformationMethod.getInstance());
                    eyeIcon.setColorFilter(android.graphics.Color.WHITE);
                } else {
                    etPass.setTransformationMethod(android.text.method.PasswordTransformationMethod.getInstance());
                    eyeIcon.setColorFilter(android.graphics.Color.LTGRAY);
                }
                etPass.setSelection(etPass.getText().length());
            }
        });

        passwordLayout.addView(etPass);
        passwordLayout.addView(eyeIcon);
        layout.addView(passwordLayout);

        final LinearLayout keyLayout = new LinearLayout(mTermuxActivity);
        keyLayout.setOrientation(LinearLayout.HORIZONTAL);
        keyLayout.setPadding(0, 20, 0, 0);

        keyLayout.setVisibility(oldBean.isUseKey() ? View.VISIBLE : View.GONE);

        android.widget.Button btnGenKey = new android.widget.Button(mTermuxActivity);
        btnGenKey.setText("生成");
        btnGenKey.setTextSize(12);
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        btnGenKey.setLayoutParams(btnParams);

        android.widget.Button btnImportKey = new android.widget.Button(mTermuxActivity);
        btnImportKey.setText("导入");
        btnImportKey.setTextSize(12);
        btnImportKey.setLayoutParams(btnParams);

        android.widget.Button btnViewPub = new android.widget.Button(mTermuxActivity);
        btnViewPub.setText("复制公钥");
        btnViewPub.setTextSize(12);
        btnViewPub.setLayoutParams(btnParams);

        keyLayout.addView(btnGenKey);
        keyLayout.addView(btnImportKey);
        keyLayout.addView(btnViewPub);
        layout.addView(keyLayout);

        if (oldBean.isUseKey()) {
            passwordLayout.setVisibility(View.GONE);
        } else {
            keyLayout.setVisibility(View.GONE);
        }

        cbUseKey.setOnCheckedChangeListener(new android.widget.CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(android.widget.CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    passwordLayout.setVisibility(View.GONE);
                    keyLayout.setVisibility(View.VISIBLE);
                    etPass.setText("");
                } else {
                    passwordLayout.setVisibility(View.VISIBLE);
                    keyLayout.setVisibility(View.GONE);
                }
            }
        });

        btnGenKey.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String currentAlias = etAlias.getText().toString().trim();
                if (android.text.TextUtils.isEmpty(currentAlias)) {
                    UUtils.showMsg("请先填写别名，密钥文件将以别名命名");
                    return;
                }

                new AlertDialog.Builder(mTermuxActivity)
                    .setTitle("生成密钥")
                    .setMessage("这将生成新的RSA密钥对：\n~/.ssh/" + currentAlias.replaceAll("[^a-zA-Z0-9_\\-]", "_") + ".key\n\n如果该文件已存在，将被覆盖")
                    .setPositiveButton("生成", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            String cmd = SSHKeyUtils.getGenerateKeyCommand(currentAlias);
                            TermuxActivity.mTerminalView.sendTextToTerminal(cmd + "\n");
                            UUtils.showMsg("正在后台生成密钥...");
                        }
                    })
                    .setNegativeButton("取消", null)
                    .show();
            }
        });

        btnImportKey.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String currentAlias = etAlias.getText().toString().trim();
                if (android.text.TextUtils.isEmpty(currentAlias)) {
                    UUtils.showMsg("请先填写别名");
                    return;
                }

                if (isAliasExist(currentAlias, position)) {
                    UUtils.showMsg("别名已存在，无法导入，请修改别名。");
                    return;
                }

                PENDING_IMPORT_ALIAS = currentAlias;

                android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_GET_CONTENT);
                intent.setType("*/*");
                intent.addCategory(android.content.Intent.CATEGORY_OPENABLE);
                mTermuxActivity.startActivityForResult(intent, REQUEST_CODE_IMPORT_KEY);
            }
        });

        btnViewPub.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String currentAlias = etAlias.getText().toString().trim();
                if (android.text.TextUtils.isEmpty(currentAlias)) {
                    UUtils.showMsg("请先填写别名");
                    return;
                }
                File pubKeyFile = new File(SSHKeyUtils.getSSHDir(), currentAlias.replaceAll("[^a-zA-Z0-9_\\-]", "_") + ".key.pub");
                if (!pubKeyFile.exists()) {
                    UUtils.showMsg("公钥文件不存在，请先点击生成密钥");
                    return;
                }
                try {
                    java.io.FileInputStream fis = new java.io.FileInputStream(pubKeyFile);
                    byte[] data = new byte[(int) pubKeyFile.length()];
                    fis.read(data);
                    fis.close();
                    String pubKeyContent = new String(data, "UTF-8");

                    android.content.ClipboardManager cm = (android.content.ClipboardManager) mTermuxActivity.getSystemService(Context.CLIPBOARD_SERVICE);
                    android.content.ClipData mClipData = android.content.ClipData.newPlainText("SSH Public Key", pubKeyContent);
                    cm.setPrimaryClip(mClipData);

                    UUtils.showMsg("公钥已复制到剪贴板");
                } catch (Exception e) {
                    UUtils.showMsg("读取失败: " + e.getMessage());
                }
            }
        });
        builder.setView(layout);
        builder.setPositiveButton("保存", null);
        builder.setNegativeButton("取消", null);

        AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(android.graphics.Color.WHITE);
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(android.graphics.Color.LTGRAY);

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String alias = etAlias.getText().toString().trim();
                String host = etHost.getText().toString().trim();
                String portStr = etPort.getText().toString().trim();

                if (android.text.TextUtils.isEmpty(alias) || android.text.TextUtils.isEmpty(host)) {
                    UUtils.showMsg("别名和IP不能为空");
                    return;
                }

                if (isAliasExist(alias, position)) {
                    UUtils.showMsg("别名\"" + alias + "\"已存在，请更换");
                    return;
                }

                boolean isIp = android.util.Patterns.IP_ADDRESS.matcher(host).matches();
                boolean isDomain = android.util.Patterns.DOMAIN_NAME.matcher(host).matches();
                boolean isLocal = "localhost".equalsIgnoreCase(host);
                if (!isIp && !isDomain && !isLocal) {
                    UUtils.showMsg("主机地址格式错误");
                    return;
                }

                int port = 22;
                try { port = Integer.parseInt(portStr); } catch (Exception e) {}

                SSHDeviceBean newBean = new SSHDeviceBean(
                    alias,
                    host,
                    port,
                    etUser.getText().toString().trim(),
                    etPass.getText().toString().trim(),
                    cbUseKey.isChecked()
                );

                updateSSHDevice(position, newBean);
                showSSHList(closeLiftListener);
                dialog.dismiss();
                UUtils.showMsg("修改成功");
            }
        });
    }
    private void updateSSHDevice(int position, SSHDeviceBean newBean) {
        String json = SaveData.getData(KEY_SSH_LIST);
        List<SSHDeviceBean> list = new ArrayList<>();
        if (!android.text.TextUtils.isEmpty(json) && !json.equals("def")) {
            try {
                Type listType = new TypeToken<List<SSHDeviceBean>>(){}.getType();
                list = new Gson().fromJson(json, listType);
            } catch (Exception e) {}
        }
        if (position >= 0 && position < list.size()) {
            list.set(position, newBean);
            SaveData.saveData(KEY_SSH_LIST, new Gson().toJson(list));
        }
    }
}
