package com.termux.zerocore.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;

import com.example.xh_lib.utils.UUtils;
import com.termux.R;
import com.termux.app.TermuxService;
import com.termux.shared.termux.TermuxConstants;
import com.termux.zerocore.activity.adapter.CreateSystemAdapter;
import com.termux.zerocore.activity.utils.CreateSystemUtils;
import com.termux.zerocore.bean.ReadSystemBean;
import com.termux.zerocore.dialog.MyDialog;
import com.termux.zerocore.settings.BaseTitleActivity;
import com.termux.zerocore.utils.SingletonCommunicationUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 容器切换界面：仅负责 UI 交互，核心逻辑见 {@link CreateSystemUtils}。
 */
public class SwitchActivity extends BaseTitleActivity implements View.OnClickListener {

    private static final String ACTION_STOP_SERVICE =
        TermuxConstants.TERMUX_APP.TERMUX_SERVICE.ACTION_STOP_SERVICE;

    private ListView containerListView;
    private CreateSystemAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_switch);
        setBaseTitle(UUtils.getString(R.string.容器切换));

        containerListView = findViewById(R.id.list_switch);
        CardView addButton = findViewById(R.id.add_containers);
        addButton.setOnClickListener(this);

        CreateSystemUtils.ensureDefaultActiveConfig();
        setupListClickListener();
        reloadContainerList();
    }

    private void reloadContainerList() {
        CreateSystemUtils.LoadResult result = CreateSystemUtils.loadContainers();
        if (result.configError) {
            Toast.makeText(this, UUtils.getString(R.string.item_containers_toast_config_error),
                Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        List<ReadSystemBean> containers = new ArrayList<>(result.containers);
        CreateSystemUtils.markActiveContainer(containers);
        adapter = new CreateSystemAdapter(containers, this);
        containerListView.setAdapter(adapter);
    }

    private void setupListClickListener() {
        containerListView.setOnItemClickListener((parent, view, position, id) -> {
            if (adapter == null) {
                return;
            }
            showContainerActionDialog(adapter.mList.get(position), position);
        });
    }

    private void showContainerActionDialog(ReadSystemBean container, int position) {
        String[] actions = {
            UUtils.getString(R.string.删除),
            UUtils.getString(R.string.切换)
        };
        new AlertDialog.Builder(this)
            .setTitle(UUtils.getString(R.string.删除完成_需要重进才能刷新))
            .setItems(actions, (dialog, which) -> {
                if (which == 0) {
                    confirmDeleteContainer(container);
                } else {
                    performSwitch(container, position);
                }
            })
            .show();
    }

    // -------------------------------------------------------------------------
    // 删除
    // -------------------------------------------------------------------------

    private void confirmDeleteContainer(ReadSystemBean container) {
        new AlertDialog.Builder(this)
            .setTitle(UUtils.getString(R.string.item_containers_dialog_delete_system_title))
            .setMessage(UUtils.getString(R.string.item_containers_dialog_delete_system_msg))
            .setNegativeButton(UUtils.getString(R.string.item_containers_dialog_i_m_sure),
                (d, w) -> startDeleteContainer(container))
            .setPositiveButton(UUtils.getString(R.string.item_containers_dialog_i_don_t_delete_it),
                (d, w) -> showIgnoredToast())
            .setNeutralButton(UUtils.getString(R.string.item_containers_dialog_don_t_delete_it),
                (d, w) -> showIgnoredToast())
            .show();
    }

    private void startDeleteContainer(ReadSystemBean container) {
        new Thread(() -> {
            if (CreateSystemUtils.isMainContainer(container.dir)) {
                runOnUiThread(() -> Toast.makeText(this,
                    UUtils.getString(R.string.item_containers_toast_not_delete_main_system),
                    Toast.LENGTH_SHORT).show());
                return;
            }

            runOnUiThread(this::showDeleteProgressDialog);

            CreateSystemUtils.DeleteResult result =
                CreateSystemUtils.deleteContainer(this, container.dir);

            runOnUiThread(() -> handleDeleteResult(result, container.dir));
        }).start();
    }

    private void showDeleteProgressDialog() {
        MyDialog dialog = new MyDialog(this);
        dialog.show();
        dialog.getDialog_title().setText(
            UUtils.getString(R.string.item_containers_dialog_delete_system_loading_title));
        dialog.getDialog_pro().setText(
            UUtils.getString(R.string.item_containers_dialog_delete_system_loading_msg));
    }

    private void handleDeleteResult(CreateSystemUtils.DeleteResult result, String containerDir) {
        if (result.blockedAsMain) {
            Toast.makeText(this,
                UUtils.getString(R.string.item_containers_toast_not_delete_main_system),
                Toast.LENGTH_SHORT).show();
            return;
        }
        if (result.needsFallbackCleanup) {
            Toast.makeText(this,
                UUtils.getString(R.string.item_containers_toast_clearing_system_residues),
                Toast.LENGTH_SHORT).show();
            String cmd = "chmod 777 -R " + containerDir + "&& rm -rf " + containerDir + " \n";
            SingletonCommunicationUtils.getInstance()
                .getmSingletonCommunicationListener()
                .sendTextToTerminal(cmd);
            finish();
            return;
        }
        Toast.makeText(this,
            UUtils.getString(R.string.item_containers_dialog_deleted_successfully),
            Toast.LENGTH_SHORT).show();
        finish();
    }

    // -------------------------------------------------------------------------
    // 切换
    // -------------------------------------------------------------------------

    private void performSwitch(ReadSystemBean target, int position) {
        if (!CreateSystemUtils.switchContainer(target)) {
            Toast.makeText(this,
                UUtils.getString(R.string.item_containers_dialog_read_failed),
                Toast.LENGTH_SHORT).show();
            return;
        }

        CreateSystemUtils.clearActiveMarks(adapter.mList);
        adapter.mList.get(position).isCkeck = true;
        adapter.notifyDataSetChanged();
        showRebootDialog();
    }

    private void showRebootDialog() {
        new AlertDialog.Builder(this)
            .setTitle(UUtils.getString(R.string.提示))
            .setCancelable(false)
            .setMessage(UUtils.getString(R.string.item_containers_dialog_msg))
            .setNegativeButton(UUtils.getString(R.string.item_containers_dialog_need), (d, w) -> {
                Toast.makeText(this,
                    UUtils.getString(R.string.item_containers_dialog_manual_exit),
                    Toast.LENGTH_SHORT).show();
                new Intent(this, TermuxService.class).setAction(ACTION_STOP_SERVICE);
                System.exit(0);
                finish();
            })
            .setPositiveButton(UUtils.getString(R.string.item_containers_dialog_no_need), (d, w) -> {
                Toast.makeText(this,
                    UUtils.getString(R.string.item_containers_toast_settings_ok_reboot),
                    Toast.LENGTH_SHORT).show();
                finish();
            })
            .show();
    }

    // -------------------------------------------------------------------------
    // 创建
    // -------------------------------------------------------------------------

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.add_containers) {
            showCreateContainerDialog();
        }
    }

    private void showCreateContainerDialog() {
        EditText nameInput = new EditText(this);
        new AlertDialog.Builder(this)
            .setTitle(UUtils.getString(R.string.item_containers_dialog_input_linux_name))
            .setIcon(R.mipmap.linux_new_ico)
            .setView(nameInput)
            .setPositiveButton(UUtils.getString(R.string.确定), (d, w) -> {
                String name = nameInput.getText().toString().trim();
                if (name.isEmpty()) {
                    return;
                }
                if (!CreateSystemUtils.createContainer(name)) {
                    Toast.makeText(this,
                        UUtils.getString(R.string.item_containers_toast_create_system_error),
                        Toast.LENGTH_SHORT).show();
                    return;
                }
                reloadContainerList();
            })
            .setNegativeButton(UUtils.getString(R.string.取消), null)
            .show();
    }

    private void showIgnoredToast() {
        Toast.makeText(this,
            UUtils.getString(R.string.item_containers_toast_ignoring_operations),
            Toast.LENGTH_SHORT).show();
    }
}
