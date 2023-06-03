package com.termux.app;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.Message;
import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.autofill.AutofillManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.blockchain.ub.utils.httputils.BaseHttpUtils;
import com.blockchain.ub.utils.httputils.HttpResponseListenerBase;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.xh_lib.utils.LogUtils;
import com.example.xh_lib.utils.SaveData;
import com.example.xh_lib.utils.UUtils;
import com.example.xh_lib.utils.UUtils2;
import com.google.gson.Gson;
import com.hjq.permissions.OnPermissionCallback;
import com.hjq.permissions.Permission;
import com.hjq.permissions.XXPermissions;
import com.lzy.okgo.model.Response;
import com.mallotec.reb.localeplugin.utils.LocaleHelper;
import com.termux.R;
import com.termux.app.api.file.FileReceiverActivity;
import com.termux.app.terminal.TermuxActivityRootView;
import com.termux.app.terminal.TermuxTerminalSessionActivityClient;
import com.termux.app.terminal.io.TermuxTerminalExtraKeys;
import com.termux.shared.activities.ReportActivity;

import com.termux.shared.activity.ActivityUtils;
import com.termux.shared.activity.media.AppCompatActivityUtils;
import com.termux.shared.android.PermissionUtils;
import com.termux.shared.data.DataUtils;
import com.termux.shared.data.IntentUtils;
import com.termux.shared.termux.TermuxConstants;
import com.termux.shared.termux.TermuxConstants.TERMUX_APP.TERMUX_ACTIVITY;
import com.termux.app.activities.HelpActivity;
import com.termux.app.activities.SettingsActivity;

import com.termux.app.terminal.TermuxSessionsListViewController;
import com.termux.app.terminal.io.TerminalToolbarViewPager;
import com.termux.app.terminal.TermuxTerminalViewClient;

import com.termux.shared.logger.Logger;
import com.termux.shared.termux.TermuxUtils;
import com.termux.shared.termux.crash.TermuxCrashUtils;
import com.termux.shared.termux.extrakeys.ExtraKeysView;
import com.termux.shared.termux.interact.TextInputDialogUtils;
import com.termux.shared.termux.settings.preferences.TermuxAppSharedPreferences;
import com.termux.shared.termux.settings.properties.TermuxAppSharedProperties;
import com.termux.shared.termux.theme.TermuxThemeUtils;
import com.termux.shared.theme.NightMode;
import com.termux.shared.view.KeyboardUtils;
import com.termux.shared.view.ViewUtils;
import com.termux.terminal.TerminalSession;
import com.termux.terminal.TerminalSessionClient;
import com.termux.view.TerminalRenderer;
import com.termux.view.TerminalView;
import com.termux.view.TerminalViewClient;
import com.termux.view.textselection.TextSelectionCursorController;
import com.termux.zerocore.activity.FontActivity;
import com.termux.zerocore.activity.SwitchActivity;
import com.termux.zerocore.activity.WebViewActivity;
import com.termux.zerocore.activity.adapter.BoomMinLAdapter;
import com.termux.zerocore.back.BackRestoreDialog;
import com.termux.zerocore.back.listener.CreateConversationListener;
import com.termux.zerocore.bean.EditPromptBean;
import com.termux.zerocore.bean.ZDYDataBean;
import com.termux.zerocore.broadcast.LocalReceiver;
import com.termux.zerocore.code.CodeString;
import com.termux.zerocore.dialog.BeautifySettingDialog;
import com.termux.zerocore.dialog.BoomCommandDialog;
import com.termux.zerocore.dialog.BoomZeroTermuxDialog;
import com.termux.zerocore.dialog.CommonCommandsDialog;
import com.termux.zerocore.dialog.DownLoadDialogBoom;
import com.termux.zerocore.dialog.EditDialog;
import com.termux.zerocore.dialog.LoadingDialog;
import com.termux.zerocore.dialog.OnLineShDialog;
import com.termux.zerocore.dialog.ProtocolDialog;
import com.termux.zerocore.dialog.SYFunBoomDialog;
import com.termux.zerocore.dialog.SwitchDialog;
import com.termux.zerocore.dialog.VNCConnectionDialog;
import com.termux.zerocore.dialog.adapter.ItemMenuAdapter;
import com.termux.zerocore.http.HTTPIP;
import com.termux.zerocore.otg.OTGManager;
import com.termux.zerocore.popuwindow.MenuLeftPopuListWindow;
import com.termux.zerocore.url.FileUrl;
import com.termux.zerocore.utermux_windows.qemu.activity.RunWindowActivity;
import com.termux.zerocore.utils.FileHttpUtils;
import com.termux.zerocore.utils.FileIOUtils;
import com.termux.zerocore.utils.IsInstallCommand;
import com.termux.zerocore.utils.PhoneUtils;
import com.termux.zerocore.utils.SmsUtils;
import com.termux.zerocore.utils.StartRunCommandUtils;
import com.termux.zerocore.utils.UUUtils;
import com.termux.zerocore.utils.VideoUtils;
import com.termux.zerocore.utils.WindowUtils;
import com.termux.zerocore.view.BoomWindow;
import com.termux.zerocore.view.xuehua.SnowView;
import com.termux.zerocore.zero.engine.ZeroCoreManage;
import com.zp.z_file.ui.ZFileListFragment;
import com.zp.z_file.zerotermux.CloseListener;
import com.zp.z_file.zerotermux.ZTConfig;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentTransaction;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.viewpager.widget.ViewPager;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

/**
 * A terminal emulator activity.
 * <p/>
 * See
 * <ul>
 * <li>http://www.mongrel-phones.com.au/default/how_to_make_a_local_service_and_bind_to_it_in_android</li>
 * <li>https://code.google.com/p/android/issues/detail?id=6426</li>
 * </ul>
 * about memory leaks.
 */
public final class TermuxActivity extends AppCompatActivity implements ServiceConnection, View.OnClickListener, MenuLeftPopuListWindow.ItemClickPopuListener, TerminalView.DoubleClickListener, View.OnFocusChangeListener {

    /**
     * The connection to the {@link TermuxService}. Requested in {@link #onCreate(Bundle)} with a call to
     * {@link #bindService(Intent, ServiceConnection, int)}, and obtained and stored in
     * {@link #onServiceConnected(ComponentName, IBinder)}.
     */
    TermuxService mTermuxService;

    /**
     * The {@link TerminalView} shown in  {@link TermuxActivity} that displays the terminal.
     */
    public static TerminalView mTerminalView;

    /**
     * The {@link TerminalViewClient} interface implementation to allow for communication between
     * {@link TerminalView} and {@link TermuxActivity}.
     */
    TermuxTerminalViewClient mTermuxTerminalViewClient;

    LocalBroadcastManager localBroadcastManager;
    LocalReceiver localReceiver;

    /**
     * Termux app shared preferences manager.
     */
    private TermuxAppSharedPreferences mPreferences;

    /**
     * Termux app shared properties manager, loaded from termux.properties
     */
    private TermuxAppSharedProperties mProperties;

    /**
     * The client for the {@link #mExtraKeysView}.
     */
    public static TermuxTerminalExtraKeys mTermuxTerminalExtraKeys;

    /**
     * The root view of the {@link TermuxActivity}.
     */
    TermuxActivityRootView mTermuxActivityRootView;

    /**
     * The space at the bottom of {@link @mTermuxActivityRootView} of the {@link TermuxActivity}.
     */
    View mTermuxActivityBottomSpaceView;

    /**
     * The terminal extra keys view.
     */
    ExtraKeysView mExtraKeysView;

    /**
     * The termux sessions list controller.
     */
    TermuxSessionsListViewController mTermuxSessionListViewController;

    /**
     * The {@link TermuxActivity} broadcast receiver for various things like terminal style configuration changes.
     */
    private final BroadcastReceiver mTermuxActivityBroadcastReceiver = new TermuxActivityBroadcastReceiver();

    /**
     * The last toast shown, used cancel current toast before showing new in {@link #showToast(String, boolean)}.
     */
    Toast mLastToast;


    /**
     * The {@link TerminalSessionClient} interface implementation to allow for communication between
     * {@link TerminalSession} and {@link TermuxActivity}.
     */
    TermuxTerminalSessionActivityClient mTermuxTerminalSessionActivityClient;

    /**
     * If between onResume() and onStop(). Note that only one session is in the foreground of the terminal view at the
     * time, so if the session causing a change is not in the foreground it should probably be treated as background.
     */
    private boolean mIsVisible;

    /**
     * If onResume() was called after onCreate().
     */
    private boolean mIsOnResumeAfterOnCreate = false;

    /**
     * If activity was restarted like due to call to {@link #recreate()} after receiving
     * {@link TERMUX_ACTIVITY#ACTION_RELOAD_STYLE}, system dark night mode was changed or activity
     * was killed by android.
     */
    private boolean mIsActivityRecreated = false;

    /**
     * The {@link TermuxActivity} is in an invalid state and must not be run.
     */
    private boolean mIsInvalidState;

    private int mNavBarHeight;

    private int mTerminalToolbarDefaultHeight;


    private static final int CONTEXT_MENU_SELECT_URL_ID = 0;
    private static final int CONTEXT_MENU_SHARE_TRANSCRIPT_ID = 1;
    private static final int CONTEXT_MENU_SHARE_SELECTED_TEXT = 10;
    private static final int CONTEXT_MENU_AUTOFILL_ID = 2;
    private static final int CONTEXT_MENU_RESET_TERMINAL_ID = 3;
    private static final int CONTEXT_MENU_KILL_PROCESS_ID = 4;
    private static final int CONTEXT_MENU_STYLING_ID = 5;
    private static final int CONTEXT_MENU_TOGGLE_KEEP_SCREEN_ON = 6;
    private static final int CONTEXT_MENU_HELP_ID = 7;
    private static final int CONTEXT_MENU_SETTINGS_ID = 8;
    private static final int CONTEXT_MENU_REPORT_ID = 9;


    private static final String ARG_TERMINAL_TOOLBAR_TEXT_INPUT = "terminal_toolbar_text_input";
    private static final String ARG_ACTIVITY_RECREATED = "activity_recreated";

    private static final String LOG_TAG = "Termux--Apk:TermuxActivity";
    private static final String TAG = "TermuxActivity";
    private OTGManager mOTGManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        Logger.logDebug(LOG_TAG, "onCreate");
        mIsOnResumeAfterOnCreate = true;

        if (savedInstanceState != null)
            mIsActivityRecreated = savedInstanceState.getBoolean(ARG_ACTIVITY_RECREATED, false);

        // Check if a crash happened on last run of the app and show a
        // notification with the crash details if it did

        // Delete ReportInfo serialized object files from cache older than 14 days
        ReportActivity.deleteReportInfoFilesOlderThanXDays(this, 14, false);

        // Load Termux app SharedProperties from disk
        mProperties = TermuxAppSharedProperties.getProperties();
        reloadProperties();

        setActivityTheme();

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_termux);

        // Load termux shared preferences
        // This will also fail if TermuxConstants.TERMUX_PACKAGE_NAME does not equal applicationId
        mPreferences = TermuxAppSharedPreferences.build(this, true);
        if (mPreferences == null) {
            // An AlertDialog should have shown to kill the app, so we don't continue running activity code
            mIsInvalidState = true;
            return;
        }


        mTermuxActivityRootView = findViewById(R.id.activity_termux_root_view);
        mTermuxActivityRootView.setActivity(this);
        mOTGManager = new OTGManager();
        mTermuxActivityBottomSpaceView = findViewById(R.id.activity_termux_bottom_space_view);
        mTermuxActivityRootView.setOnApplyWindowInsetsListener(new TermuxActivityRootView.WindowInsetsListener());

        View content = findViewById(android.R.id.content);
        content.setOnApplyWindowInsetsListener((v, insets) -> {
            mNavBarHeight = insets.getSystemWindowInsetBottom();
            return insets;
        });

        if (mProperties.isUsingFullScreen()) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
        }
        WindowUtils.setImmersionBar(TermuxActivity.this, 0.1f);

        setTermuxTerminalViewAndClients();

        setTerminalToolbarView(savedInstanceState);

        setSettingsButtonView();

        setNewSessionButtonView();

        setToggleKeyboardView();

        registerForContextMenu(mTerminalView);

        // Start the {@link TermuxService} and make it run regardless of who is bound to it
        Intent serviceIntent = new Intent(this, TermuxService.class);
        startService(serviceIntent);

        // Attempt to bind to the service, this will call the {@link #onServiceConnected(ComponentName, IBinder)}
        // callback if it succeeds.
        if (!bindService(serviceIntent, this, 0))
            throw new RuntimeException("bindService() failed");

        // Send the {@link TermuxConstants#BROADCAST_TERMUX_OPENED} broadcast to notify apps that Termux
        // app has been opened.
        TermuxUtils.sendTermuxOpenedBroadcast(this);


        createFiles();
        initZeroView();
        initStatue();
        initColorConfig();
        initListener();
        initScrollHandler();
        testFun();
    }


    private void initListener() {
        mTerminalView.getTextSelectionCursorControllerView().setAddCommend(new TextSelectionCursorController.AddCommend() {
            @Override
            public void editCommend(String edit) {
                if (!TextUtils.isEmpty(edit)) {
                    UUtils.showMsg(UUtils.getString(R.string.add_commend_msg_ok));
                    FileIOUtils.INSTANCE.commendSave(edit, edit, false);
                } else {
                    UUtils.showMsg(UUtils.getString(R.string.add_commend_msg_fail));
                }

            }
        });

        mTerminalView.setActionPointer2ClickListener(new TerminalView.ActionPointer2ClickListener() {
            @Override
            public void pointer2Click() {
                String toolbox_state = com.termux.view.zerotermux.SaveData.getData("toolbox_state", UUtils.getContext());
                LogUtils.d(TAG, "pointer2Click toolbox_state:" + toolbox_state);
                if (!(toolbox_state == null || toolbox_state.isEmpty() || toolbox_state.equals("def"))) {
                    return;
                }
                final LoadingDialog[] loadingDialog = {null};
                CommonCommandsDialog mCommonCommandsDialog = new CommonCommandsDialog(TermuxActivity.this);
                mCommonCommandsDialog.show();
                mCommonCommandsDialog.setCancelable(true);
                mCommonCommandsDialog.setCommonDialogListener(new ItemMenuAdapter.CommonDialogListener() {
                    @Override
                    public void video(@NonNull File file) {
                        VideoUtils.getInstance().setVideoView(back_video);
                        VideoUtils.getInstance().start(file);
                        back_video.setVisibility(View.VISIBLE);
                        back_img.setVisibility(View.GONE);
                        LogUtils.d(TAG, "BackVideo set file is :" + file.getAbsolutePath());
                    }
                });
                mCommonCommandsDialog.setClearStyleListener(new ItemMenuAdapter.ClearStyleListener() {
                    @Override
                    public void clear() {
                        VideoUtils.getInstance().onDestroy();
                        back_video.setVisibility(View.GONE);
                        back_img.setVisibility(View.GONE);
                        back_color.setVisibility(View.GONE);
                        TerminalRenderer.COLOR_TEXT = Color.parseColor("#ffffff");
                        ExtraKeysView.DEFAULT_BUTTON_TEXT_COLOR = Color.parseColor("#ffffff");
                        mTerminalView.invalidate();
                        if (mExtraKeysView != null) {
                            mExtraKeysView.setColorButton();
                            mExtraKeysView.invalidate();
                        }
                        if (mCommonCommandsDialog != null && mCommonCommandsDialog.isShowing()) {
                            mCommonCommandsDialog.dismiss();
                        }
                    }
                });
                mCommonCommandsDialog.setKeyViewListener(new ItemMenuAdapter.KeyViewListener() {
                    @Override
                    public void view(@NonNull View mView) {
                        if (mView == null) {
                            LogUtils.d(TAG, "key View is null, return.");
                            return;
                        }
                        if (key_bord.getChildCount() > 0) {
                            key_bord.removeAllViews();
                            getTerminalToolbarViewPager().setVisibility(View.VISIBLE);
                            mTerminalView.stopTextSelectionMode();

                            KeyboardUtils.clearDisableSoftKeyboardFlags(TermuxActivity.this);
                            KeyboardUtils.toggleSoftKeyboard(TermuxActivity.this);
                        } else {
                            try {
                                key_bord.addView(mView);
                                getTerminalToolbarViewPager().setVisibility(View.GONE);
                            } catch (Exception e) {
                                e.printStackTrace();
                                key_bord.removeAllViews();
                            }

                            KeyboardUtils.disableSoftKeyboard(TermuxActivity.this, mTerminalView);
                        }
                        if (mCommonCommandsDialog != null && mCommonCommandsDialog.isShowing()) {
                            mCommonCommandsDialog.dismiss();
                        }

                    }
                });
                mCommonCommandsDialog.setVShellDialogListener(new ItemMenuAdapter.VShellDialogListener() {
                    @Override
                    public void showDialog(boolean b) {
                        if (b) {
                            loadingDialog[0] = new LoadingDialog(TermuxActivity.this);
                            loadingDialog[0].show();
                        } else {
                            if (loadingDialog[0] != null && loadingDialog[0].isShowing()) {
                                loadingDialog[0].dismiss();
                            }
                        }
                    }

                    @Override
                    public void vShell(@NonNull ArrayList<String> environment, @NonNull ArrayList<String> processArgs) {
                        if (environment == null || processArgs == null) {
                            return;
                        }
                        mTerminalView.sendTextToTerminal(UUtils.arrayListToStringShell(processArgs) + "\n");
                        if (mCommonCommandsDialog != null && mCommonCommandsDialog.isShowing()) {
                            mCommonCommandsDialog.dismiss();
                        }

                    }
                });
            }
        });
    }

    public void initColorConfig() {
        String font_color = SaveData.INSTANCE.getStringOther("font_color");
        String back_color = SaveData.INSTANCE.getStringOther("back_color");
        String change_text = SaveData.INSTANCE.getStringOther("change_text");
        String change_text_show = SaveData.INSTANCE.getStringOther("change_text_show");
        if (!(font_color == null || font_color.isEmpty() || font_color.equals("def"))) {
            try {
                int color = Integer.parseInt(font_color);
                TerminalRenderer.COLOR_TEXT = color;
                ExtraKeysView.DEFAULT_BUTTON_TEXT_COLOR = color;
                mTerminalView.invalidate();
                UUtils.showLog("Test:111111");
                if (mExtraKeysView != null) {
                    mExtraKeysView.setColorButton();
                    mExtraKeysView.invalidate();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (!(back_color == null || back_color.isEmpty() || back_color.equals("def"))) {
            try {
                int color = Integer.parseInt(back_color);
                this.back_color.setBackgroundColor(color);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if ((change_text == null || change_text.isEmpty() || change_text.equals("def"))) {
            this.back_color.setAlpha(1f);
        } else {
            this.back_color.setAlpha(0.3f);
        }

        if ((change_text_show == null || change_text_show.isEmpty() || change_text_show.equals("def"))) {
            double_tishi.setVisibility(View.VISIBLE);
        } else {
            double_tishi.setVisibility(View.GONE);
        }
        if (FileIOUtils.INSTANCE.isPathVideo()) {
            //有视频
            String pathVideo = FileIOUtils.INSTANCE.getPathVideo();
            if (!TextUtils.isEmpty(pathVideo)) {
                File file = new File(pathVideo);
                if (file.exists()) {
                    VideoUtils.getInstance().setVideoView(back_video);
                    VideoUtils.getInstance().start(file);
                    back_video.setVisibility(View.VISIBLE);
                    back_img.setVisibility(View.GONE);
                }
            }
        } else {
            //没有视频
            File file = new File(FileUrl.INSTANCE.getMainConfigImg() + "/back.jpg");
            if (file.exists()) {
                Glide.with(TermuxActivity.this).load(file).diskCacheStrategy(DiskCacheStrategy.NONE).skipMemoryCache(true).into(back_img);
                back_video.setVisibility(View.GONE);
                back_img.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        Logger.logDebug(LOG_TAG, "onStart");

        if (mIsInvalidState) return;

        mIsVisible = true;

        if (mTermuxTerminalSessionActivityClient != null)
            mTermuxTerminalSessionActivityClient.onStart();

        if (mTermuxTerminalViewClient != null)
            mTermuxTerminalViewClient.onStart();

        if (mPreferences.isTerminalMarginAdjustmentEnabled())
            addTermuxActivityRootViewGlobalLayoutListener();

        registerTermuxActivityBroadcastReceiver();
    }

    @Override
    public void onResume() {
        super.onResume();
        //初始化ZeroTermux 引擎
        ZeroCoreManage.initEngineManage();

        Logger.logVerbose(LOG_TAG, "onResume");

        VideoUtils.getInstance().onResume();

        if (mIsInvalidState) return;

        if (mTermuxTerminalSessionActivityClient != null)
            mTermuxTerminalSessionActivityClient.onResume();

        if (mTermuxTerminalViewClient != null)
            mTermuxTerminalViewClient.onResume();

        // Check if a crash happened on last run of the app or if a plugin crashed and show a
        // notification with the crash details if it did
        TermuxCrashUtils.notifyAppCrashFromCrashLogFile(this, LOG_TAG);

        mIsOnResumeAfterOnCreate = false;


        isShow();

        /**
         *
         * OTG
         *
         *
         */

        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        filter.addAction(OTGManager.OTGManagerConstant.INSTANCE.getACTION_USB_PERMISSION());
        registerReceiver(mUsbReceiver, filter);

    }

    @Override
    protected void onRestart() {
        super.onRestart();
        isShow();
    }

    @Override
    protected void onStop() {
        super.onStop();

        Logger.logDebug(LOG_TAG, "onStop");

        if (mIsInvalidState) return;

        mIsVisible = false;

        if (mTermuxTerminalSessionActivityClient != null)
            mTermuxTerminalSessionActivityClient.onStop();

        if (mTermuxTerminalViewClient != null)
            mTermuxTerminalViewClient.onStop();

        removeTermuxActivityRootViewGlobalLayoutListener();

        unregisterTermuxActivityBroadcastReceiver();
        getDrawer().closeDrawers();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        Logger.logDebug(LOG_TAG, "onDestroy");

        VideoUtils.getInstance().onDestroy();
        if (mIsInvalidState) return;

        if (mTermuxService != null) {
            // Do not leave service and session clients with references to activity.
            mTermuxService.unsetTermuxTerminalSessionClient();
            mTermuxService = null;
        }
        unregisterReceiver(mUsbReceiver);
        if (localBroadcastManager!= null) {
            localBroadcastManager.unregisterReceiver(localReceiver);
        }

        try {
            unbindService(this);
        } catch (Exception e) {
            // ignore.
        }

        /**
         *
         * OTG
         *
         *
         */

        //  unregisterReceiver(mUsbReceiver);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle savedInstanceState) {
        Logger.logVerbose(LOG_TAG, "onSaveInstanceState");

        super.onSaveInstanceState(savedInstanceState);
        saveTerminalToolbarTextInput(savedInstanceState);
        savedInstanceState.putBoolean(ARG_ACTIVITY_RECREATED, true);
    }


    /**
     * Part of the {@link ServiceConnection} interface. The service is bound with
     * {@link #bindService(Intent, ServiceConnection, int)} in {@link #onCreate(Bundle)} which will cause a call to this
     * callback method.
     */
    @Override
    public void onServiceConnected(ComponentName componentName, IBinder service) {

        Logger.logDebug(LOG_TAG, "onServiceConnected");

        mTermuxService = ((TermuxService.LocalBinder) service).service;

        setTermuxSessionsListView();

        fileManager();
        locaBroadcast();
        final Intent intent = getIntent();
        setIntent(null);

        if (mTermuxService.isTermuxSessionsEmpty()) {
            if (mIsVisible) {
                TermuxInstaller.setupBootstrapIfNeeded(TermuxActivity.this, () -> {
                    if (mTermuxService == null) return; // Activity might have been destroyed.
                    try {
                        boolean launchFailsafe = false;
                        if (intent != null && intent.getExtras() != null) {
                            launchFailsafe = intent.getExtras().getBoolean(TERMUX_ACTIVITY.EXTRA_FAILSAFE_SESSION, false);
                        }
                        mTermuxTerminalSessionActivityClient.addNewSession(launchFailsafe, null);
                    } catch (WindowManager.BadTokenException e) {
                        // Activity finished - ignore.
                    }
                });
            } else {
                // The service connected while not in foreground - just bail out.
                finishActivityIfNotFinishing();
            }
        } else {
            // If termux was started from launcher "New session" shortcut and activity is recreated,
            // then the original intent will be re-delivered, resulting in a new session being re-added
            // each time.
            if (!mIsActivityRecreated && intent != null && Intent.ACTION_RUN.equals(intent.getAction())) {
                // Android 7.1 app shortcut from res/xml/shortcuts.xml.
                boolean isFailSafe = intent.getBooleanExtra(TERMUX_ACTIVITY.EXTRA_FAILSAFE_SESSION, false);
                mTermuxTerminalSessionActivityClient.addNewSession(isFailSafe, null);
            } else {
                mTermuxTerminalSessionActivityClient.setCurrentSession(mTermuxTerminalSessionActivityClient.getCurrentStoredSessionOrLast());
            }
        }

        // Update the {@link TerminalSession} and {@link TerminalEmulator} clients.
        mTermuxService.setTermuxTerminalSessionClient(mTermuxTerminalSessionActivityClient);
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {

        Logger.logDebug(LOG_TAG, "onServiceDisconnected");

        // Respect being stopped from the {@link TermuxService} notification action.
        finishActivityIfNotFinishing();
    }


    private void reloadProperties() {
        mProperties.loadTermuxPropertiesFromDisk();

        if (mTermuxTerminalViewClient != null)
            mTermuxTerminalViewClient.onReloadProperties();
    }


    private void setActivityTheme() {
        // Update NightMode.APP_NIGHT_MODE
        TermuxThemeUtils.setAppNightMode(mProperties.getNightMode());

        // Set activity night mode. If NightMode.SYSTEM is set, then android will automatically
        // trigger recreation of activity when uiMode/dark mode configuration is changed so that
        // day or night theme takes affect.
        AppCompatActivityUtils.setNightMode(this, NightMode.getAppNightMode().getName(), true);
    }

    private void setMargins() {
        RelativeLayout relativeLayout = findViewById(R.id.activity_termux_root_relative_layout);
        int marginHorizontal = mProperties.getTerminalMarginHorizontal();
        int marginVertical = mProperties.getTerminalMarginVertical();
        ViewUtils.setLayoutMarginsInDp(relativeLayout, marginHorizontal, marginVertical, marginHorizontal, marginVertical);
    }


    public void addTermuxActivityRootViewGlobalLayoutListener() {
        getTermuxActivityRootView().getViewTreeObserver().addOnGlobalLayoutListener(getTermuxActivityRootView());
    }

    public void removeTermuxActivityRootViewGlobalLayoutListener() {
        if (getTermuxActivityRootView() != null)
            getTermuxActivityRootView().getViewTreeObserver().removeOnGlobalLayoutListener(getTermuxActivityRootView());
    }


    private void setTermuxTerminalViewAndClients() {
        // Set termux terminal view and session clients
        mTermuxTerminalSessionActivityClient = new TermuxTerminalSessionActivityClient(this);
        mTermuxTerminalViewClient = new TermuxTerminalViewClient(this, mTermuxTerminalSessionActivityClient);

        // Set termux terminal view
        mTerminalView = findViewById(R.id.terminal_view);
        mTerminalView.setTerminalViewClient(mTermuxTerminalViewClient);

        if (mTermuxTerminalViewClient != null)
            mTermuxTerminalViewClient.onCreate();
        if (mTermuxTerminalViewClient != null) {
            mTermuxTerminalViewClient.setKeyUpDown(new TermuxTerminalViewClient.KeyUpDown() {
                @Override
                public void keyDown(int key) {
                    if (getDrawer().isDrawerOpen(Gravity.LEFT) || getDrawer().isDrawerOpen(Gravity.RIGHT)) {
                        getDrawer().closeDrawers();
                        return;
                    }
                    if (key == KeyEvent.KEYCODE_VOLUME_UP) {
                        getDrawer().openDrawer(Gravity.LEFT);
                        return;
                    }

                    if (key == KeyEvent.KEYCODE_VOLUME_DOWN) {
                        getDrawer().openDrawer(Gravity.RIGHT);
                        return;
                    }
                }
            });
        }

        if (mTermuxTerminalSessionActivityClient != null)
            mTermuxTerminalSessionActivityClient.onCreate();
    }

    private void setTermuxSessionsListView() {
        ListView termuxSessionsListView = findViewById(R.id.terminal_sessions_list);
        mTermuxSessionListViewController = new TermuxSessionsListViewController(this, mTermuxService.getTermuxSessions());
        termuxSessionsListView.setAdapter(mTermuxSessionListViewController);
        termuxSessionsListView.setOnItemClickListener(mTermuxSessionListViewController);
        termuxSessionsListView.setOnItemLongClickListener(mTermuxSessionListViewController);
    }


    private void setTerminalToolbarView(Bundle savedInstanceState) {
        mTermuxTerminalExtraKeys = new TermuxTerminalExtraKeys(this, mTerminalView,
            mTermuxTerminalViewClient, mTermuxTerminalSessionActivityClient);

        final ViewPager terminalToolbarViewPager = getTerminalToolbarViewPager();
        if (mPreferences.shouldShowTerminalToolbar())
            terminalToolbarViewPager.setVisibility(View.VISIBLE);

        ViewGroup.LayoutParams layoutParams = terminalToolbarViewPager.getLayoutParams();
        mTerminalToolbarDefaultHeight = layoutParams.height;

        setTerminalToolbarHeight();

        String savedTextInput = null;
        if (savedInstanceState != null)
            savedTextInput = savedInstanceState.getString(ARG_TERMINAL_TOOLBAR_TEXT_INPUT);

        terminalToolbarViewPager.setAdapter(new TerminalToolbarViewPager.PageAdapter(this, savedTextInput));
        terminalToolbarViewPager.addOnPageChangeListener(new TerminalToolbarViewPager.OnPageChangeListener(this, terminalToolbarViewPager));
    }

    private void setTerminalToolbarHeight() {
        final ViewPager terminalToolbarViewPager = getTerminalToolbarViewPager();
        if (terminalToolbarViewPager == null) return;

        ViewGroup.LayoutParams layoutParams = terminalToolbarViewPager.getLayoutParams();
        layoutParams.height = Math.round(mTerminalToolbarDefaultHeight *
            (mTermuxTerminalExtraKeys.getExtraKeysInfo() == null ? 0 : mTermuxTerminalExtraKeys.getExtraKeysInfo().getMatrix().length) *
            mProperties.getTerminalToolbarHeightScaleFactor());
        terminalToolbarViewPager.setLayoutParams(layoutParams);
    }

    public void toggleTerminalToolbar() {
        final ViewPager terminalToolbarViewPager = getTerminalToolbarViewPager();
        if (terminalToolbarViewPager == null) return;

        final boolean showNow = mPreferences.toogleShowTerminalToolbar();
        Logger.showToast(this, (showNow ? getString(R.string.msg_enabling_terminal_toolbar) : getString(R.string.msg_disabling_terminal_toolbar)), true);
        terminalToolbarViewPager.setVisibility(showNow ? View.VISIBLE : View.GONE);
        if (showNow && isTerminalToolbarTextInputViewSelected()) {
            // Focus the text input view if just revealed.
            findViewById(R.id.terminal_toolbar_text_input).requestFocus();
        }
    }


    private void saveTerminalToolbarTextInput(Bundle savedInstanceState) {
        if (savedInstanceState == null) return;

        final EditText textInputView = findViewById(R.id.terminal_toolbar_text_input);
        if (textInputView != null) {
            String textInput = textInputView.getText().toString();
            if (!textInput.isEmpty())
                savedInstanceState.putString(ARG_TERMINAL_TOOLBAR_TEXT_INPUT, textInput);
        }
    }


    private void setSettingsButtonView() {
        ImageButton settingsButton = findViewById(R.id.settings_button);
        settingsButton.setOnClickListener(v -> {
            ActivityUtils.startActivity(this, new Intent(this, SettingsActivity.class));
        });
    }

    private void setNewSessionButtonView() {
        View newSessionButton = findViewById(R.id.new_session_button);
        newSessionButton.setOnClickListener(v -> mTermuxTerminalSessionActivityClient.addNewSession(false, null));
        newSessionButton.setOnLongClickListener(v -> {
            TextInputDialogUtils.textInput(TermuxActivity.this, R.string.title_create_named_session, null,
                R.string.action_create_named_session_confirm, text -> mTermuxTerminalSessionActivityClient.addNewSession(false, text),
                R.string.action_new_session_failsafe, text -> mTermuxTerminalSessionActivityClient.addNewSession(true, text),
                -1, null, null);
            return true;
        });
    }

    private void setToggleKeyboardView() {
        findViewById(R.id.toggle_keyboard_button).setOnClickListener(v -> {
          /*  mTermuxTerminalViewClient.onToggleSoftKeyboardRequest();
            getDrawer().closeDrawers();*/
            indexSwitch(0);
        });

        findViewById(R.id.toggle_keyboard_button).setOnLongClickListener(v -> {
            //toggleTerminalToolbar();
            return true;
        });
        findViewById(R.id.select_new_session_button).setOnClickListener(v -> {
            indexSwitch(1);
        });
    }


    @SuppressLint("RtlHardcoded")
    @Override
    public void onBackPressed() {
        if (getDrawer().isDrawerOpen(Gravity.LEFT)) {
            getDrawer().closeDrawers();
        } else {
            finishActivityIfNotFinishing();
        }
    }

    public void finishActivityIfNotFinishing() {
        // prevent duplicate calls to finish() if called from multiple places
        if (!TermuxActivity.this.isFinishing()) {
            finish();
        }
    }

    /**
     * Show a toast and dismiss the last one if still visible.
     */
    public void showToast(String text, boolean longDuration) {
        if (text == null || text.isEmpty()) return;
        if (mLastToast != null) mLastToast.cancel();
        mLastToast = Toast.makeText(TermuxActivity.this, text, longDuration ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT);
        mLastToast.setGravity(Gravity.TOP, 0, 0);
        mLastToast.show();
    }


    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        TerminalSession currentSession = getCurrentSession();
        if (currentSession == null) return;

        boolean addAutoFillMenu = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AutofillManager autofillManager = getSystemService(AutofillManager.class);
            if (autofillManager != null && autofillManager.isEnabled()) {
                addAutoFillMenu = true;
            }
        }

        menu.add(Menu.NONE, CONTEXT_MENU_SELECT_URL_ID, Menu.NONE, R.string.action_select_url);
        menu.add(Menu.NONE, CONTEXT_MENU_SHARE_TRANSCRIPT_ID, Menu.NONE, R.string.action_share_transcript);
        if (!DataUtils.isNullOrEmpty(mTerminalView.getStoredSelectedText()))
            menu.add(Menu.NONE, CONTEXT_MENU_SHARE_SELECTED_TEXT, Menu.NONE, R.string.action_share_selected_text);
        if (addAutoFillMenu)
            menu.add(Menu.NONE, CONTEXT_MENU_AUTOFILL_ID, Menu.NONE, R.string.action_autofill_password);
        menu.add(Menu.NONE, CONTEXT_MENU_RESET_TERMINAL_ID, Menu.NONE, R.string.action_reset_terminal);
        menu.add(Menu.NONE, CONTEXT_MENU_KILL_PROCESS_ID, Menu.NONE, getResources().getString(R.string.action_kill_process, getCurrentSession().getPid())).setEnabled(currentSession.isRunning());
        menu.add(Menu.NONE, CONTEXT_MENU_STYLING_ID, Menu.NONE, R.string.action_style_terminal);
        menu.add(Menu.NONE, CONTEXT_MENU_TOGGLE_KEEP_SCREEN_ON, Menu.NONE, R.string.action_toggle_keep_screen_on).setCheckable(true).setChecked(mPreferences.shouldKeepScreenOn());
        menu.add(Menu.NONE, CONTEXT_MENU_HELP_ID, Menu.NONE, R.string.action_open_help);
        menu.add(Menu.NONE, CONTEXT_MENU_SETTINGS_ID, Menu.NONE, R.string.action_open_settings);
        menu.add(Menu.NONE, CONTEXT_MENU_REPORT_ID, Menu.NONE, R.string.action_report_issue);
    }

    /**
     * Hook system menu to show context menu instead.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        mTerminalView.showContextMenu();
        return false;
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        TerminalSession session = getCurrentSession();

        switch (item.getItemId()) {
            case CONTEXT_MENU_SELECT_URL_ID:
                mTermuxTerminalViewClient.showUrlSelection();
                return true;
            case CONTEXT_MENU_SHARE_TRANSCRIPT_ID:
                mTermuxTerminalViewClient.shareSessionTranscript();
                return true;
            case CONTEXT_MENU_SHARE_SELECTED_TEXT:
                mTermuxTerminalViewClient.shareSelectedText();
                return true;
            case CONTEXT_MENU_AUTOFILL_ID:
                requestAutoFill();
                return true;
            case CONTEXT_MENU_RESET_TERMINAL_ID:
                onResetTerminalSession(session);
                return true;
            case CONTEXT_MENU_KILL_PROCESS_ID:
                showKillSessionDialog(session);
                return true;
            case CONTEXT_MENU_STYLING_ID:
                showStylingDialog();
                return true;
            case CONTEXT_MENU_TOGGLE_KEEP_SCREEN_ON:
                toggleKeepScreenOn();
                return true;
            case CONTEXT_MENU_HELP_ID:
                ActivityUtils.startActivity(this, new Intent(this, HelpActivity.class));
                return true;
            case CONTEXT_MENU_SETTINGS_ID:
                ActivityUtils.startActivity(this, new Intent(this, SettingsActivity.class));
                return true;
            case CONTEXT_MENU_REPORT_ID:
                mTermuxTerminalViewClient.reportIssueFromTranscript();
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    @Override
    public void onContextMenuClosed(Menu menu) {
        super.onContextMenuClosed(menu);
        // onContextMenuClosed() is triggered twice if back button is pressed to dismiss instead of tap for some reason
        mTerminalView.onContextMenuClosed(menu);
    }

    private void showKillSessionDialog(TerminalSession session) {
        if (session == null) return;

        final AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setIcon(android.R.drawable.ic_dialog_alert);
        b.setMessage(R.string.title_confirm_kill_process);
        b.setPositiveButton(android.R.string.yes, (dialog, id) -> {
            dialog.dismiss();
            session.finishIfRunning();
        });
        b.setNegativeButton(android.R.string.no, null);
        b.show();
    }

    private void onResetTerminalSession(TerminalSession session) {
        if (session != null) {
            session.reset();
            showToast(getResources().getString(R.string.msg_terminal_reset), true);

            if (mTermuxTerminalSessionActivityClient != null)
                mTermuxTerminalSessionActivityClient.onResetTerminalSession();
        }
    }

    private void showStylingDialog() {
        Intent stylingIntent = new Intent();
        stylingIntent.setClassName(TermuxConstants.TERMUX_STYLING_PACKAGE_NAME, TermuxConstants.TERMUX_STYLING.TERMUX_STYLING_ACTIVITY_NAME);
        try {
            startActivity(stylingIntent);
        } catch (ActivityNotFoundException | IllegalArgumentException e) {
            // The startActivity() call is not documented to throw IllegalArgumentException.
            // However, crash reporting shows that it sometimes does, so catch it here.
            new AlertDialog.Builder(this).setMessage(getString(R.string.error_styling_not_installed))
                .setPositiveButton(R.string.action_styling_install,
                    (dialog, which) -> ActivityUtils.startActivity(this, new Intent(Intent.ACTION_VIEW, Uri.parse(TermuxConstants.TERMUX_STYLING_FDROID_PACKAGE_URL))))
                .setNegativeButton(android.R.string.cancel, null).show();
        }
    }

    private void toggleKeepScreenOn() {
        if (mTerminalView.getKeepScreenOn()) {
            mTerminalView.setKeepScreenOn(false);
            mPreferences.setKeepScreenOn(false);
        } else {
            mTerminalView.setKeepScreenOn(true);
            mPreferences.setKeepScreenOn(true);
        }
    }

    private void requestAutoFill() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AutofillManager autofillManager = getSystemService(AutofillManager.class);
            if (autofillManager != null && autofillManager.isEnabled()) {
                autofillManager.requestAutofill(mTerminalView);
            }
        }
    }


    /**
     * For processes to access primary external storage (/sdcard, /storage/emulated/0, ~/storage/shared),
     * termux needs to be granted legacy WRITE_EXTERNAL_STORAGE or MANAGE_EXTERNAL_STORAGE permissions
     * if targeting targetSdkVersion 30 (android 11) and running on sdk 30 (android 11) and higher.
     */
    public void requestStoragePermission(boolean isPermissionCallback) {
        new Thread() {
            @Override
            public void run() {
                // Do not ask for permission again
                int requestCode = isPermissionCallback ? -1 : PermissionUtils.REQUEST_GRANT_STORAGE_PERMISSION;

                // If permission is granted, then also setup storage symlinks.
                if (PermissionUtils.checkAndRequestLegacyOrManageExternalStoragePermission(
                    TermuxActivity.this, requestCode, !isPermissionCallback)) {
                    if (isPermissionCallback)
                        Logger.logInfoAndShowToast(TermuxActivity.this, LOG_TAG,
                            getString(com.termux.shared.R.string.msg_storage_permission_granted_on_request));

                    TermuxInstaller.setupStorageSymlinks(TermuxActivity.this);
                } else {
                    if (isPermissionCallback)
                        Logger.logInfoAndShowToast(TermuxActivity.this, LOG_TAG,
                            getString(com.termux.shared.R.string.msg_storage_permission_not_granted_on_request));
                }
            }
        }.start();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Logger.logVerbose(LOG_TAG, "onActivityResult: requestCode: " + requestCode + ", resultCode: " + resultCode + ", data: " + IntentUtils.getIntentString(data));
        if (requestCode == PermissionUtils.REQUEST_GRANT_STORAGE_PERMISSION) {
            requestStoragePermission(true);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Logger.logVerbose(LOG_TAG, "onRequestPermissionsResult: requestCode: " + requestCode + ", permissions: " + Arrays.toString(permissions) + ", grantResults: " + Arrays.toString(grantResults));
        if (requestCode == PermissionUtils.REQUEST_GRANT_STORAGE_PERMISSION) {
            requestStoragePermission(true);
        }
    }


    public int getNavBarHeight() {
        return mNavBarHeight;
    }

    public TermuxActivityRootView getTermuxActivityRootView() {
        return mTermuxActivityRootView;
    }

    public View getTermuxActivityBottomSpaceView() {
        return mTermuxActivityBottomSpaceView;
    }

    public ExtraKeysView getExtraKeysView() {
        return mExtraKeysView;
    }

    public TermuxTerminalExtraKeys getTermuxTerminalExtraKeys() {
        return mTermuxTerminalExtraKeys;
    }

    public void setExtraKeysView(ExtraKeysView extraKeysView) {
        mExtraKeysView = extraKeysView;
    }

    public DrawerLayout getDrawer() {
        return (DrawerLayout) findViewById(R.id.drawer_layout);
    }


    public ViewPager getTerminalToolbarViewPager() {
        return (ViewPager) findViewById(R.id.terminal_toolbar_view_pager);
    }

    public float getTerminalToolbarDefaultHeight() {
        return mTerminalToolbarDefaultHeight;
    }

    public boolean isTerminalViewSelected() {
        return getTerminalToolbarViewPager().getCurrentItem() == 0;
    }

    public boolean isTerminalToolbarTextInputViewSelected() {
        return getTerminalToolbarViewPager().getCurrentItem() == 1;
    }


    public void termuxSessionListNotifyUpdated() {
        mTermuxSessionListViewController.notifyDataSetChanged();
    }

    public boolean isVisible() {
        return mIsVisible;
    }

    public boolean isOnResumeAfterOnCreate() {
        return mIsOnResumeAfterOnCreate;
    }

    public boolean isActivityRecreated() {
        return mIsActivityRecreated;
    }


    public TermuxService getTermuxService() {
        return mTermuxService;
    }

    public TerminalView getTerminalView() {
        return mTerminalView;
    }

    public TermuxTerminalViewClient getTermuxTerminalViewClient() {
        return mTermuxTerminalViewClient;
    }

    public TermuxTerminalSessionActivityClient getTermuxTerminalSessionClient() {
        return mTermuxTerminalSessionActivityClient;
    }

    @Nullable
    public TerminalSession getCurrentSession() {
        if (mTerminalView != null)
            return mTerminalView.getCurrentSession();
        else
            return null;
    }

    public TermuxAppSharedPreferences getPreferences() {
        return mPreferences;
    }

    public TermuxAppSharedProperties getProperties() {
        return mProperties;
    }


    public static void updateTermuxActivityStyling(Context context, boolean recreateActivity) {
        // Make sure that terminal styling is always applied.
        Intent stylingIntent = new Intent(TERMUX_ACTIVITY.ACTION_RELOAD_STYLE);
        stylingIntent.putExtra(TERMUX_ACTIVITY.EXTRA_RECREATE_ACTIVITY, recreateActivity);
        context.sendBroadcast(stylingIntent);
    }

    private void registerTermuxActivityBroadcastReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(TERMUX_ACTIVITY.ACTION_NOTIFY_APP_CRASH);
        intentFilter.addAction(TERMUX_ACTIVITY.ACTION_RELOAD_STYLE);
        intentFilter.addAction(TERMUX_ACTIVITY.ACTION_REQUEST_PERMISSIONS);

        registerReceiver(mTermuxActivityBroadcastReceiver, intentFilter);
    }

    private void unregisterTermuxActivityBroadcastReceiver() {
        unregisterReceiver(mTermuxActivityBroadcastReceiver);
    }

    private boolean fixTermuxActivityBroadcastReceiverIntent(Intent intent) {
        if (intent == null) return false;

        String extraReloadStyle = intent.getStringExtra(TERMUX_ACTIVITY.EXTRA_RELOAD_STYLE);
        if ("storage".equals(extraReloadStyle)) {
            intent.removeExtra(TERMUX_ACTIVITY.EXTRA_RELOAD_STYLE);
            intent.setAction(TERMUX_ACTIVITY.ACTION_REQUEST_PERMISSIONS);
        }
        resBroadcastReceiever(extraReloadStyle);

        if ("readsms".equals(extraReloadStyle) || "contactperson".equals(extraReloadStyle)) {
            return true;
        }

        return false;
    }

    @Override
    public void onFocusChange(View view, boolean b) {
        if (view == null) {
            LogUtils.e(TAG, "onFocusChange view is null");
            return;
        }
    }

    class TermuxActivityBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null) return;

            if (mIsVisible) {
                if (fixTermuxActivityBroadcastReceiverIntent(intent)) {
                    return;
                }

                switch (intent.getAction()) {
                    case TERMUX_ACTIVITY.ACTION_NOTIFY_APP_CRASH:
                        Logger.logDebug(LOG_TAG, "Received intent to notify app crash");
                        TermuxCrashUtils.notifyAppCrashFromCrashLogFile(context, LOG_TAG);
                        return;
                    case TERMUX_ACTIVITY.ACTION_RELOAD_STYLE:
                        Logger.logDebug(LOG_TAG, "Received intent to reload styling");
                        reloadActivityStyling(intent.getBooleanExtra(TERMUX_ACTIVITY.EXTRA_RECREATE_ACTIVITY, true));
                        return;
                    case TERMUX_ACTIVITY.ACTION_REQUEST_PERMISSIONS:
                        Logger.logDebug(LOG_TAG, "Received intent to request storage permissions");
                        requestStoragePermission(false);
                        return;
                    default:
                }
            }
        }
    }


    private void reloadActivityStyling(boolean recreateActivity) {
        if (mProperties != null) {
            reloadProperties();

            if (mExtraKeysView != null) {
                mExtraKeysView.setButtonTextAllCaps(mProperties.shouldExtraKeysTextBeAllCaps());
                mExtraKeysView.reload(mTermuxTerminalExtraKeys.getExtraKeysInfo(), mTerminalToolbarDefaultHeight);
            }

            // Update NightMode.APP_NIGHT_MODE
            TermuxThemeUtils.setAppNightMode(mProperties.getNightMode());
        }

        setMargins();
        setTerminalToolbarHeight();

        FileReceiverActivity.updateFileReceiverActivityComponentsState(this);

        if (mTermuxTerminalSessionActivityClient != null)
            mTermuxTerminalSessionActivityClient.onReloadActivityStyling();

        if (mTermuxTerminalViewClient != null)
            mTermuxTerminalViewClient.onReloadActivityStyling();

        // To change the activity and drawer theme, activity needs to be recreated.
        // It will destroy the activity, including all stored variables and views, and onCreate()
        // will be called again. Extra keys input text, terminal sessions and transcripts will be preserved.
        if (recreateActivity) {
            Logger.logDebug(LOG_TAG, "Recreating activity");
            TermuxActivity.this.recreate();
        }
    }


    public static void startTermuxActivity(@NonNull final Context context) {
        ActivityUtils.startActivity(context, newInstance(context));
    }

    public static Intent newInstance(@NonNull final Context context) {
        Intent intent = new Intent(context, TermuxActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }


    /**
     * ZeroView
     */
    private LinearLayout code_ll;
    private LinearLayout rongqi;
    private LinearLayout back_res;
    private LinearLayout linux_online;
    private LinearLayout qemu;
    private LinearLayout cmd_command;
    private LinearLayout moe;
    private LinearLayout msg;
    private LinearLayout files_mulu;
    private TextView version;
    private TextView eg_tv;
    private TextView text_start;
    private LinearLayout title_mb;
    private LinearLayout github;
    private LinearLayout start_command;
    private LinearLayout xuanfu;
    private LinearLayout ziti;
    private LinearLayout zero_tier;
    private LinearLayout download_http;
    private LinearLayout zt_title;
    private LinearLayout vnc_start;
    private LinearLayout xue_hua;
    private LinearLayout termux_pl;
    private LinearLayout quanping;
    private LinearLayout zero_fun;
    private LinearLayout yuyan;
    private LinearLayout shiyan_fun;
    private LinearLayout zerotermux_bbs;
    private LinearLayout key_bord;
    private TextView service_status;
    private TextView service_eg;
    private TextView msg_tv;
    private TextView ip_status;
    private TextView qq_group_tv;
    private TextView telegram_group_tv;
    private TextView double_tishi;
    private TextView xue_hua_start;
    private EditText mDataMessage;
    private CardView mDataMessageCard;
    private FrameLayout xue_fragment;
    private LinearLayout online_sh;
    private LinearLayout beautify;
    private View back_color;
    private View layout_menu;
    private ImageView back_img;
    private VideoView back_video;

    private FrameLayout frame_file;
    private RelativeLayout session_rl;


    /**
     * ZeroTermux
     */

    private void initZeroView() {


        code_ll = findViewById(R.id.code_ll);
        frame_file = findViewById(R.id.frame_file);
        session_rl = findViewById(R.id.session_rl);
        telegram_group_tv = findViewById(R.id.telegram_group_tv);
        qq_group_tv = findViewById(R.id.qq_group_tv);
        zerotermux_bbs = findViewById(R.id.zerotermux_bbs);
        rongqi = findViewById(R.id.rongqi);
        layout_menu = findViewById(R.id.layout_menu);
        back_res = findViewById(R.id.back_res);
        linux_online = findViewById(R.id.linux_online);
        qemu = findViewById(R.id.qemu);
        cmd_command = findViewById(R.id.cmd_command);
        moe = findViewById(R.id.moe);
        msg = findViewById(R.id.msg);
        files_mulu = findViewById(R.id.files_mulu);
        version = findViewById(R.id.version);
        eg_tv = findViewById(R.id.eg_tv);
        title_mb = findViewById(R.id.title_mb);
        github = findViewById(R.id.github);
        start_command = findViewById(R.id.start_command);
        text_start = findViewById(R.id.text_start);
        xuanfu = findViewById(R.id.xuanfu);
        ziti = findViewById(R.id.ziti);
        key_bord = findViewById(R.id.key_bord);
        service_status = findViewById(R.id.service_status);
        service_eg = findViewById(R.id.service_eg);
        zero_tier = findViewById(R.id.zero_tier);
        download_http = findViewById(R.id.download_http);
        vnc_start = findViewById(R.id.vnc_start);
        zt_title = findViewById(R.id.zt_title);
        msg_tv = findViewById(R.id.msg_tv);
        xue_fragment = findViewById(R.id.xue_fragment);
        mDataMessage = findViewById(R.id.data_message);
        mDataMessageCard = findViewById(R.id.data_message_card);
        xue_hua = findViewById(R.id.xue_hua);
        xue_hua_start = findViewById(R.id.xue_hua_start);
        termux_pl = findViewById(R.id.termux_pl);
        quanping = findViewById(R.id.quanping);
        yuyan = findViewById(R.id.yuyan);
        ip_status = findViewById(R.id.ip_status);
        zero_fun = findViewById(R.id.zero_fun);
        shiyan_fun = findViewById(R.id.shiyan_fun);
        double_tishi = findViewById(R.id.double_tishi);
        online_sh = findViewById(R.id.online_sh);
        beautify = findViewById(R.id.beautify);
        back_color = mTermuxActivityRootView.getBack_color();
        back_img = mTermuxActivityRootView.getBack_img();
        back_video = mTermuxActivityRootView.getBack_video();

        try {
            double_tishi.setText(double_tishi.getText() + "\n" + TermuxInstaller.determineTermuxArchName().toUpperCase());

        } catch (Exception e) {
            e.printStackTrace();
        }

        code_ll.setOnClickListener(this);
        rongqi.setOnClickListener(this);
        back_res.setOnClickListener(this);
        linux_online.setOnClickListener(this);
        qemu.setOnClickListener(this);
        cmd_command.setOnClickListener(this);
        moe.setOnClickListener(this);
        msg.setOnClickListener(this);
        online_sh.setOnClickListener(this);
        zerotermux_bbs.setOnClickListener(this);
        telegram_group_tv.setOnClickListener(this);
        qq_group_tv.setOnClickListener(this);
        files_mulu.setOnClickListener(this);
        github.setOnClickListener(this);
        start_command.setOnClickListener(this);
        xuanfu.setOnClickListener(this);
        ziti.setOnClickListener(this);
        zero_tier.setOnClickListener(this);
        download_http.setOnClickListener(this);
        vnc_start.setOnClickListener(this);
        xue_hua.setOnClickListener(this);
        termux_pl.setOnClickListener(this);
        quanping.setOnClickListener(this);
        zero_fun.setOnClickListener(this);
        yuyan.setOnClickListener(this);
        beautify.setOnClickListener(this);
        shiyan_fun.setOnClickListener(this);
        mTerminalView.setOnFocusChangeListener(this);
        zt_title.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                UUtils.showMsg(UUtils.getString(R.string.已开始打印APP实时delog));

                mTerminalView.sendTextToTerminal("adb logcat * | find \"trace\" \n");
                //  mTerminalView.sendTextToTerminal("adb shell \n");
                return true;
            }
        });

        mTerminalView.setDoubleClickListener(this);
        title_mb.setVisibility(View.GONE);
        String xieyi = SaveData.INSTANCE.getStringOther("xieyi");
        if (xieyi == null || xieyi.isEmpty() || xieyi.equals("def")) {
            ProtocolDialog protocolDialog = new ProtocolDialog(this);
            protocolDialog.show();
            protocolDialog.setCancelable(false);
        }
        getServiceVs();
        getDrawer().addDrawerListener(new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerSlide(@NonNull @NotNull View drawerView, float slideOffset) {
                int i = (int) (slideOffset * 100);
                if (getDrawer().isDrawerVisible(Gravity.LEFT)) {
                    if (i < 50) {
                        title_mb.setVisibility(View.GONE);
                    } else {
                        title_mb.setVisibility(View.VISIBLE);
                        ip_status.setText(UUtils.getHostIP());
                    }
                }
            }

            @Override
            public void onDrawerOpened(@NonNull @NotNull View drawerView) {
                //  title_mb.setVisibility(View.VISIBLE);
                WindowUtils.setImmersionBar(TermuxActivity.this, 0.6f);
                setEgInstallStatus();
                layout_menu.setFocusable(true);
                dataMessage();
            }

            @Override
            public void onDrawerClosed(@NonNull @NotNull View drawerView) {
                WindowUtils.setImmersionBar(TermuxActivity.this, 0.1f);
                //  title_mb.setVisibility(View.GONE);
                setEgInstallStatus();
                layout_menu.setFocusable(false);
            }

            @Override
            public void onDrawerStateChanged(int newState) {

            }
        });
        refStartCommandStat();

    }

    private void dataMessage() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String dataMessageFileString = FileIOUtils.INSTANCE.getDataMessageFileString();
                if (dataMessageFileString != null && !(dataMessageFileString.trim().isEmpty())) {
                    UUtils.getHandler().post(new Runnable() {
                        @Override
                        public void run() {
                            mDataMessageCard.setVisibility(View.VISIBLE);
                            mDataMessage.setText(UUtils.getString(R.string.data_message) + "\n\n" + dataMessageFileString);
                        }
                    });
                } else {
                    UUtils.getHandler().post(new Runnable() {
                        @Override
                        public void run() {
                            mDataMessageCard.setVisibility(View.GONE);
                        }
                    });
                }
            }
        }).start();
    }

    private void setEgInstallStatus() {
        version.setText(UUtils.getString(R.string.版本) + " : " + UUtils2.INSTANCE.getVersionName(UUtils.getContext()));
        String versionName = ZeroCoreManage.getVersionName();
        if (!TextUtils.isEmpty(versionName)) {
            eg_tv.setText(UUtils.getString(R.string.engine_vision) + " : " + versionName);
        } else {
            eg_tv.setText(UUtils.getString(R.string.engine_vision) + " : " + UUtils.getString(R.string.engine_not_install));
        }
    }

    /**
     * 刷新状态
     */

    private void initStatue() {
        String xue_statues = SaveData.INSTANCE.getStringOther("xue_statues");

        if (xue_statues == null || xue_statues.isEmpty() || xue_statues.equals("def")) {
            xue_hua_start.setText(UUtils.getString(R.string.雪花关));
            xue_fragment.removeAllViews();
        } else {
            xue_hua_start.setText(UUtils.getString(R.string.雪花开));
            SnowView snowView = new SnowView(TermuxActivity.this);
            xue_fragment.removeAllViews();
            xue_fragment.addView(snowView);
        }
    }


    @Override
    public void onClick(View v) {

        switch (v.getId()) {

            /**
             * 源切换功能
             *
             *
             */
            case R.id.code_ll:
                ArrayList<MenuLeftPopuListWindow.MenuLeftPopuListData> menuLeftPopuListData = new ArrayList<>();

                //清华
                MenuLeftPopuListWindow.MenuLeftPopuListData qinghua = new MenuLeftPopuListWindow.MenuLeftPopuListData(R.mipmap.qinghua_ico, UUtils.getString(R.string.清华源), 1);
                menuLeftPopuListData.add(qinghua);
                //北京
                MenuLeftPopuListWindow.MenuLeftPopuListData beijing = new MenuLeftPopuListWindow.MenuLeftPopuListData(R.mipmap.beijing, UUtils.getString(R.string.北京源), 2);
                menuLeftPopuListData.add(beijing);
                //官方
                MenuLeftPopuListWindow.MenuLeftPopuListData guanfang = new MenuLeftPopuListWindow.MenuLeftPopuListData(R.mipmap.guanfang, UUtils.getString(R.string.官方源), 3);
                menuLeftPopuListData.add(guanfang);
                //NJU
                MenuLeftPopuListWindow.MenuLeftPopuListData nju = new MenuLeftPopuListWindow.MenuLeftPopuListData(R.mipmap.nju_ico, UUtils.getString(R.string.nju), 496);
                menuLeftPopuListData.add(nju);
                //ustc
                MenuLeftPopuListWindow.MenuLeftPopuListData ustc = new MenuLeftPopuListWindow.MenuLeftPopuListData(R.mipmap.mingl_ico, UUtils.getString(R.string.ustc), 4666);
                menuLeftPopuListData.add(ustc);
                //哈尔滨
                MenuLeftPopuListWindow.MenuLeftPopuListData heb = new MenuLeftPopuListWindow.MenuLeftPopuListData(R.mipmap.mingl_ico, UUtils.getString(R.string.hit), 46667);
                menuLeftPopuListData.add(heb);

                showMenuDialog(menuLeftPopuListData, code_ll);

                break;

            case R.id.vnc_start:
                ArrayList<MenuLeftPopuListWindow.MenuLeftPopuListData> menuLeftPopuListDatavnc = new ArrayList<>();
                //快速vnc
                MenuLeftPopuListWindow.MenuLeftPopuListData ksvnc = new MenuLeftPopuListWindow.MenuLeftPopuListData(R.mipmap.dsk, UUtils.getString(R.string.快速VNC), 10);
                menuLeftPopuListDatavnc.add(ksvnc);
                //自定vnc
                MenuLeftPopuListWindow.MenuLeftPopuListData zdvnc = new MenuLeftPopuListWindow.MenuLeftPopuListData(R.mipmap.dsk, UUtils.getString(R.string.自定VNC), 11);
                menuLeftPopuListDatavnc.add(zdvnc);

                //高级vnc
                MenuLeftPopuListWindow.MenuLeftPopuListData gjvnc = new MenuLeftPopuListWindow.MenuLeftPopuListData(R.mipmap.dsk, UUtils.getString(R.string.高级VNC), 12);
                menuLeftPopuListDatavnc.add(gjvnc);

                showMenuDialog(menuLeftPopuListDatavnc, vnc_start);

                break;

            /**
             *
             * 容器
             *
             */
            case R.id.rongqi:
                getDrawer().closeDrawer(Gravity.LEFT);
                startActivity(new Intent(this, SwitchActivity.class));
                break;

            /**
             *
             * 备份恢复
             *
             */
            case R.id.back_res:
                getDrawer().closeDrawer(Gravity.LEFT);
                //  startActivity(new Intent(this, BackNewActivity.class));
                BackRestoreDialog backRestoreDialog = new BackRestoreDialog(this);
                backRestoreDialog.setCreateConversationListener(new CreateConversationListener() {
                    @Override
                    public void create() {
                        mTermuxTerminalSessionActivityClient.addNewSession(false, "Zero session");
                    }
                });
                backRestoreDialog.initData();
                backRestoreDialog.show();
                backRestoreDialog.setCancelable(true);
                backRestoreDialog.createStoragePath();
                break;
            case R.id.linux_online:
                getDrawer().closeDrawer(Gravity.LEFT);
                LoadingDialog loadingDialog = new LoadingDialog(this);
                loadingDialog.show();
                loadingDialog.setCancelable(false);
                UUtils.runOnThread(new Runnable() {
                    @Override
                    public void run() {
                        UUtils.writerFile("linux/termux_linux_toolx.zip", new File(FileUrl.INSTANCE.getMainHomeUrl(), "/termux_linux_toolx.zip"));
                        UUtils.runOnThread(new Runnable() {
                            @Override
                            public void run() {
                                loadingDialog.dismiss();
                                mTerminalView.sendTextToTerminal(CodeString.INSTANCE.getRunLinuxSh());
                            }
                        });
                    }
                });
                break;
            case R.id.qemu:

                ArrayList<MenuLeftPopuListWindow.MenuLeftPopuListData> menuLeftPopuListData1 = new ArrayList<>();
                //官方
                MenuLeftPopuListWindow.MenuLeftPopuListData qemuData = new MenuLeftPopuListWindow.MenuLeftPopuListData(R.mipmap.qemu_ico_hai, UUtils.getString(R.string.海的QEMU), 5);
                menuLeftPopuListData1.add(qemuData);

                MenuLeftPopuListWindow.MenuLeftPopuListData zeroData = new MenuLeftPopuListWindow.MenuLeftPopuListData(R.mipmap.windows_xp, UUtils.getString(R.string.Zero), 501);
                menuLeftPopuListData1.add(zeroData);

                MenuLeftPopuListWindow.MenuLeftPopuListData win7Data = new MenuLeftPopuListWindow.MenuLeftPopuListData(R.mipmap.windows, UUtils.getString(R.string.Win7模拟), 502);
                menuLeftPopuListData1.add(win7Data);

                MenuLeftPopuListWindow.MenuLeftPopuListData winXpData = new MenuLeftPopuListWindow.MenuLeftPopuListData(R.mipmap.windows_xp_ico, UUtils.getString(R.string.WinXp), 503);
                menuLeftPopuListData1.add(winXpData);

                showMenuDialog(menuLeftPopuListData1, qemu);

                break;
            case R.id.cmd_command:


                BoomCommandDialog boomCommandDialog = new BoomCommandDialog(TermuxActivity.this);
                boomCommandDialog.show();
                boomCommandDialog.setCancelable(true);

                break;
            case R.id.zerotermux_bbs:


                Intent intent2 = new Intent(this, WebViewActivity.class);
                intent2.putExtra("title", "ZeroTermux 论坛");
                intent2.putExtra("content", HTTPIP.ZERO_BBS);
                startActivity(intent2);

                break;
            case R.id.moe:

                getDrawer().closeDrawer(Gravity.LEFT);
                mTerminalView.sendTextToTerminal(CodeString.INSTANCE.getRunMoeSh());

                break;

            case R.id.msg:
                ArrayList<MenuLeftPopuListWindow.MenuLeftPopuListData> menuphoneMsg = new ArrayList<>();

                MenuLeftPopuListWindow.MenuLeftPopuListData msg_phone = new MenuLeftPopuListWindow.MenuLeftPopuListData(R.mipmap.install_msg_phone, UUtils.getString(R.string.安装短信读取工具), 6);
                menuphoneMsg.add(msg_phone);

                showMenuDialog(menuphoneMsg, msg);
                break;

            case R.id.files_mulu:


                try {
                    Intent intent = new Intent();
                    intent.setAction("com.utermux.files.action");
                    startActivity(intent);
                } catch (Exception e) {
                    e.printStackTrace();

                    try {
                        installApk(getAssets().open("apk/utermux_file_plug.ip"), "files");
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }

                }


                break;
            case R.id.github:
                // SendJoinUtils.INSTANCE.sendJoin(this);

                Intent intent = new Intent();
                intent.setData(Uri.parse("https://github.com/hanxinhao000/ZeroTermux"));//Url 就是你要打开的网址
                intent.setAction(Intent.ACTION_VIEW);
                startActivity(intent); //启动浏览器
                break;
            case R.id.start_command:


                //refStartCommandStat()
                if (StartRunCommandUtils.INSTANCE.isRun()) {

                    StartRunCommandUtils.INSTANCE.endRun();
                } else {
                    StartRunCommandUtils.INSTANCE.startRun();
                }

                refStartCommandStat();

                break;
            case R.id.xuanfu:


                try {
                    Intent intent1 = new Intent();
                    intent1.setAction("com.zero_float.action.ENTER");
                    startActivity(intent1);
                } catch (Exception e) {
                    e.printStackTrace();

                    try {
                        installApk(getAssets().open("apk/zero_float.ip"), "zeroFloat");
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }

                }


                break;

            case R.id.ziti:
                getDrawer().close();
                title_mb.setVisibility(View.GONE);
                startActivity(new Intent(this, FontActivity.class));
                break;

            case R.id.zero_tier:


                EditDialog editDialog = new EditDialog(this);

                EditText edit_text = editDialog.getEdit_text();

                editDialog.getCancel().setText(UUtils.getString(R.string.如何创建服务器));
                editDialog.getCancel().setVisibility(View.GONE);

                editDialog.getCancel().setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {


                    }
                });


                editDialog.getOk().setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {


                        String s = edit_text.getText().toString();


                        if (s == null || s.isEmpty()) {

                            s = "http://10.242.164.19";

                        }

                        editDialog.dismiss();
                        startHttp(s);


                    }
                });


                editDialog.show();


                break;

            case R.id.download_http:


                startHttp1(HTTPIP.IP);


                break;

            case R.id.xue_hua:

                String xue_statues = SaveData.INSTANCE.getStringOther("xue_statues");

                if (xue_statues == null || xue_statues.isEmpty() || xue_statues.equals("def")) {
                    xue_hua_start.setText(UUtils.getString(R.string.雪花开));
                    SnowView snowView = new SnowView(TermuxActivity.this);
                    xue_fragment.removeAllViews();
                    xue_fragment.addView(snowView);
                    SaveData.INSTANCE.saveStringOther("xue_statues", "true");
                } else {
                    xue_hua_start.setText(UUtils.getString(R.string.雪花关));
                    xue_fragment.removeAllViews();
                    SaveData.INSTANCE.saveStringOther("xue_statues", "def");
                }


                break;
            //官方插件
            case R.id.termux_pl:


                ArrayList<MenuLeftPopuListWindow.MenuLeftPopuListData> menuphoneGfCj = new ArrayList<>();

                MenuLeftPopuListWindow.MenuLeftPopuListData msg_api = new MenuLeftPopuListWindow.MenuLeftPopuListData(R.drawable.ic_launcher, UUtils.getString(R.string.TermuxApi), 20);
                menuphoneGfCj.add(msg_api);

                MenuLeftPopuListWindow.MenuLeftPopuListData msg_tasker = new MenuLeftPopuListWindow.MenuLeftPopuListData(R.drawable.ic_launcher, UUtils.getString(R.string.TermuxTasker), 21);
                menuphoneGfCj.add(msg_tasker);

                MenuLeftPopuListWindow.MenuLeftPopuListData msg_boot = new MenuLeftPopuListWindow.MenuLeftPopuListData(R.drawable.ic_launcher, UUtils.getString(R.string.TermuxBoot), 22);
                menuphoneGfCj.add(msg_boot);

                MenuLeftPopuListWindow.MenuLeftPopuListData msg_styling = new MenuLeftPopuListWindow.MenuLeftPopuListData(R.drawable.ic_launcher, UUtils.getString(R.string.TermuxStyling), 23);
                menuphoneGfCj.add(msg_styling);

                MenuLeftPopuListWindow.MenuLeftPopuListData msg_x11 = new MenuLeftPopuListWindow.MenuLeftPopuListData(R.mipmap.termux_x11, UUtils.getString(R.string.termux_x11), 24);
                menuphoneGfCj.add(msg_x11);

                showMenuDialog(menuphoneGfCj, termux_pl);

                break;

            //全屏 WindowUtils
            case R.id.quanping:
                if (quanping.getTag() == null) {
                    WindowUtils.setFullScreen(this);
                    quanping.setTag("fff");
                    //mExtraKeysView.setVisibility(View.GONE);
                    if (getExtraKeysView() != null && getTerminalToolbarViewPager() != null) {
                        getExtraKeysView().setVisibility(View.GONE);
                        getTerminalToolbarViewPager().setVisibility(View.GONE);
                    }
                } else {
                    WindowUtils.exitFullScreen(this);
                    quanping.setTag(null);
                    //mExtraKeysView.setVisibility(View.VISIBLE);
                    if (getExtraKeysView() != null && getTerminalToolbarViewPager() != null) {
                        getExtraKeysView().setVisibility(View.VISIBLE);
                        getTerminalToolbarViewPager().setVisibility(View.VISIBLE);
                    }
                }
                break;

            case R.id.yuyan:

                ArrayList<MenuLeftPopuListWindow.MenuLeftPopuListData> yuyan_list = new ArrayList<>();

                MenuLeftPopuListWindow.MenuLeftPopuListData msg_zh = new MenuLeftPopuListWindow.MenuLeftPopuListData(R.mipmap.zhongwen, UUtils.getString(R.string.中文), 30);
                yuyan_list.add(msg_zh);

                MenuLeftPopuListWindow.MenuLeftPopuListData msg_en = new MenuLeftPopuListWindow.MenuLeftPopuListData(R.mipmap.yingwen_ico, UUtils.getString(R.string.English), 31);
                yuyan_list.add(msg_en);

                showMenuDialog(yuyan_list, yuyan);

                break;
            //底层弹窗
            case R.id.zero_fun:
                getDrawer().close();

                BoomZeroTermuxDialog boomZeroTermuxDialog = new BoomZeroTermuxDialog(this);
                boomZeroTermuxDialog.show();
                boomZeroTermuxDialog.setCancelable(true);


                break;

            case R.id.shiyan_fun:

                SYFunBoomDialog syFunBoomDialog = new SYFunBoomDialog(this);
                syFunBoomDialog.show();
                syFunBoomDialog.setCancelable(true);

                break;

            case R.id.online_sh:

                getDrawer().close();

                OnLineShDialog mOnLineShDialog = new OnLineShDialog(this);

                mOnLineShDialog.setOnItemClickListener(new OnLineShDialog.OnItemClickListener() {
                    @Override
                    public void click(@NotNull String msg) {
                        mTerminalView.sendTextToTerminal(msg + "\n");
                        mOnLineShDialog.dismiss();
                    }
                });

                mOnLineShDialog.show();

                mOnLineShDialog.setCancelable(true);

                break;

            case R.id.qq_group_tv:
                UUtils.copyToClip("878847174");
                break;
            case R.id.telegram_group_tv:
                Intent intent1 = new Intent();
                intent1.setData(Uri.parse("https://t.me/ztcommunity"));
                intent1.setAction(Intent.ACTION_VIEW);
                this.startActivity(intent1);
                break;
            case R.id.beautify:

                getDrawer().close();

                BeautifySettingDialog mBeautifySettingDialog = new BeautifySettingDialog(this);

                mBeautifySettingDialog.setBackColorChange(new BeautifySettingDialog.BackColorChange() {
                    @Override
                    public void onColorChange(int color) {
                        back_color.setBackgroundColor(color);
                    }

                    @Override
                    public void onColorApChange(int ap) {

                    }
                });

                mBeautifySettingDialog.setOnChangeImageFile(new BeautifySettingDialog.OnChangeImageFile() {
                    @Override
                    public void onChangImage(@NotNull File mFile) {
                        back_video.setVisibility(View.GONE);
                        back_img.setVisibility(View.VISIBLE);
                        Glide.with(TermuxActivity.this).load(mFile).diskCacheStrategy(DiskCacheStrategy.NONE).skipMemoryCache(true).into(back_img);
                    }
                });

                mBeautifySettingDialog.setOnChangeTextView(new BeautifySettingDialog.OnChangeTextView() {
                    @Override
                    public void onChange(boolean change) {
                        Logger.logDebug(LOG_TAG, "change:" + change);
                        if (change) {
                            back_color.setAlpha(0.3f);
                        } else {
                            back_color.setAlpha(1f);
                        }
                    }
                });
                mBeautifySettingDialog.setOnTextCheckedChangeListener(new BeautifySettingDialog.OnTextCheckedChangeListener() {
                    @Override
                    public void onChange(boolean change) {
                        Logger.logDebug(LOG_TAG, "setOnTextCheckedChangeListener:" + change);
                        if (change) {
                            double_tishi.setVisibility(View.VISIBLE);
                        } else {
                            double_tishi.setVisibility(View.GONE);
                        }
                    }
                });
                mBeautifySettingDialog.setFontColorChange(new BeautifySettingDialog.FontColorChange() {
                    @Override
                    public void onColorChange(int color) {
                        TerminalRenderer.COLOR_TEXT = color;
                        ExtraKeysView.DEFAULT_BUTTON_TEXT_COLOR = color;
                        mTerminalView.invalidate();
                        UUtils.showLog("Test:22222");
                        if (mExtraKeysView != null) {
                            mExtraKeysView.setColorButton();
                            mExtraKeysView.invalidate();
                        }
                    }

                    @Override
                    public void onColorApChange(int color) {
                        TerminalRenderer.COLOR_TEXT = color;
                        ExtraKeysView.DEFAULT_BUTTON_TEXT_COLOR = color;
                        mTerminalView.invalidate();
                        UUtils.showLog("Test:333333");
                        if (mExtraKeysView != null) {
                            mExtraKeysView.setColorButton();
                            mExtraKeysView.invalidate();
                        }

                    }
                });

                mBeautifySettingDialog.show();

                mBeautifySettingDialog.setCancelable(true);


                break;


        }

    }


    private void showMenuDialog(ArrayList<MenuLeftPopuListWindow.MenuLeftPopuListData> arrayList, View showView) {

        MenuLeftPopuListWindow menuLeftPopuListWindow = new MenuLeftPopuListWindow(this);
        menuLeftPopuListWindow.setItemClickPopuListener(this);
        menuLeftPopuListWindow.setListData(arrayList);
        menuLeftPopuListWindow.showAsDropDown(showView, 250, -200);


    }

    /**
     * 刷新状态
     */

    private void refStartCommandStat() {


        if (StartRunCommandUtils.INSTANCE.isRun()) {

            text_start.setText(UUtils.getString(R.string.开机启动开));

        } else {

            text_start.setText(UUtils.getString(R.string.开机启动));

        }


    }


    @Override
    protected void onPause() {
        super.onPause();
        VideoUtils.getInstance().pause();
        title_mb.setVisibility(View.GONE);
        getDrawer().close();

    }

    /**
     * 菜单点击事件
     *
     * @param id
     * @param index
     */

    @Override
    public void itemClick(int id, int index, MenuLeftPopuListWindow mMenuLeftPopuListWindow) {
        mMenuLeftPopuListWindow.dismiss();
        switch (id) {
            //清华
            case 1:

                SwitchDialog switchDialog = switchDialogShow(UUtils.getString(R.string.警告), UUtils.getString(R.string.该操作会覆盖您的文件记录));

                switchDialog.getCancel().setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        switchDialog.dismiss();

                    }
                });
                switchDialog.getOk().setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        switchDialog.dismiss();
                        mTerminalView.sendTextToTerminal(CodeString.INSTANCE.getQH());
                    }
                });


                break;
            //北京
            case 2:

                SwitchDialog switchDialog1 = switchDialogShow(UUtils.getString(R.string.警告), UUtils.getString(R.string.该操作会覆盖您的文件记录));

                switchDialog1.getCancel().setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        switchDialog1.dismiss();

                    }
                });
                switchDialog1.getOk().setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        switchDialog1.dismiss();
                        mTerminalView.sendTextToTerminal(CodeString.INSTANCE.getBJ());
                    }
                });
                break;
            //官方
            case 3:

                SwitchDialog switchDialog2 = switchDialogShow(UUtils.getString(R.string.警告), UUtils.getString(R.string.该操作会覆盖您的文件记录));

                switchDialog2.getCancel().setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        switchDialog2.dismiss();

                    }
                });
                switchDialog2.getOk().setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        switchDialog2.dismiss();
                        UUtils.writerFile("code/sources.list", new File(FileUrl.INSTANCE.getSourcesUrl()));
                        UUtils.writerFile("code/science.list", new File(FileUrl.INSTANCE.getScienceUrl()));
                        UUtils.writerFile("code/game.list", new File(FileUrl.INSTANCE.getGameUrl()));
                        mTerminalView.sendTextToTerminal(CodeString.INSTANCE.getUpDate());
                    }
                });


                break;

            //NJU
            case 496:

                SwitchDialog switchDialog15 = switchDialogShow(UUtils.getString(R.string.警告), UUtils.getString(R.string.该操作会覆盖您的文件记录));

                switchDialog15.getCancel().setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        switchDialog15.dismiss();

                    }
                });
                switchDialog15.getOk().setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        switchDialog15.dismiss();
                        mTerminalView.sendTextToTerminal(CodeString.INSTANCE.getNJU());
                    }
                });
                break;

            //USTC
            case 4666:

                SwitchDialog ustc = switchDialogShow(UUtils.getString(R.string.警告), UUtils.getString(R.string.该操作会覆盖您的文件记录));

                ustc.getCancel().setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ustc.dismiss();

                    }
                });
                ustc.getOk().setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ustc.dismiss();
                        mTerminalView.sendTextToTerminal(CodeString.INSTANCE.getUSTC());
                    }
                });
                break;
            //heb
            case 46667:

                SwitchDialog heb = switchDialogShow(UUtils.getString(R.string.警告), UUtils.getString(R.string.该操作会覆盖您的文件记录));

                heb.getCancel().setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        heb.dismiss();

                    }
                });
                heb.getOk().setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        heb.dismiss();
                        mTerminalView.sendTextToTerminal(CodeString.INSTANCE.getHEB());
                    }
                });
                break;
            //qemu
            case 5:
                getDrawer().closeDrawer(Gravity.LEFT);


                SwitchDialog msgQemuLine = switchDialogShow(UUtils.getString(R.string.选择方式), UUtils.getString(R.string.要获取最新版本));

                msgQemuLine.show();
                msgQemuLine.getOk().setText(UUtils.getString(R.string.线上脚本));
                msgQemuLine.getOk().setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        msgQemuLine.dismiss();
                        mTerminalView.sendTextToTerminal(CodeString.INSTANCE.getRunLineQemu() + "\n");

                    }
                });
                msgQemuLine.getCancel().setText(UUtils.getString(R.string.本地脚本));
                msgQemuLine.getCancel().setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        msgQemuLine.dismiss();
                        LoadingDialog loadingDialog = new LoadingDialog(TermuxActivity.this);
                        loadingDialog.show();
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                UUtils.writerFile("qemu/utqemu.sh", new File(FileUrl.INSTANCE.getMainHomeUrl(), "/utqemu.sh"));
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        mTerminalView.sendTextToTerminal(CodeString.INSTANCE.getRunQemuSh());
                                        loadingDialog.dismiss();
                                    }
                                });
                            }
                        }).start();

                    }
                });
                msgQemuLine.setCancelable(true);


                break;

            case 501:

                startActivity(new Intent(this, RunWindowActivity.class));
                break;
            case 502:

                XXPermissions.with(TermuxActivity.this)
                    .permission(Permission.WRITE_EXTERNAL_STORAGE)
                    .permission(Permission.READ_EXTERNAL_STORAGE)
                    .request(new OnPermissionCallback() {

                        @Override
                        public void onGranted(List<String> permissions, boolean all) {
                            if (all) {

                                File zeroTermuxShare = FileUrl.INSTANCE.getZeroTermuxShare();
                                if (!zeroTermuxShare.exists()) {
                                    zeroTermuxShare.mkdirs();
                                }

                                getDrawer().closeDrawer(Gravity.LEFT);
                                UUtils.writerFile("qemu/qemu_win7.sh", new File(FileUrl.INSTANCE.getMainHomeUrl(), "/qemu_win7.sh"));
                                mTerminalView.sendTextToTerminal(CodeString.INSTANCE.getRunWin7Sh());

                            } else {

                                UUtils.showMsg("无权限");
                            }
                        }

                        @Override
                        public void onDenied(List<String> permissions, boolean never) {
                            if (never) {
                                UUtils.showMsg("无权限");
                                // 如果是被永久拒绝就跳转到应用权限系统设置页面
                                XXPermissions.startPermissionActivity(TermuxActivity.this, permissions);
                            } else {
                                UUtils.showMsg("无权限");
                            }
                        }
                    });


                break;

            case 503:

                XXPermissions.with(TermuxActivity.this)
                    .permission(Permission.WRITE_EXTERNAL_STORAGE)
                    .permission(Permission.READ_EXTERNAL_STORAGE)
                    .request(new OnPermissionCallback() {

                        @Override
                        public void onGranted(List<String> permissions, boolean all) {
                            if (all) {

                                File zeroTermuxShare = FileUrl.INSTANCE.getZeroTermuxShare();
                                if (!zeroTermuxShare.exists()) {
                                    zeroTermuxShare.mkdirs();
                                }

                                getDrawer().closeDrawer(Gravity.LEFT);
                                UUtils.writerFile("qemu/qemu_winxp.sh", new File(FileUrl.INSTANCE.getMainHomeUrl(), "/qemu_winxp.sh"));
                                mTerminalView.sendTextToTerminal(CodeString.INSTANCE.getRunWinXPSh());

                            } else {

                                UUtils.showMsg("无权限");
                            }
                        }

                        @Override
                        public void onDenied(List<String> permissions, boolean never) {
                            if (never) {
                                UUtils.showMsg("无权限");
                                // 如果是被永久拒绝就跳转到应用权限系统设置页面
                                XXPermissions.startPermissionActivity(TermuxActivity.this, permissions);
                            } else {
                                UUtils.showMsg("无权限");
                            }
                        }
                    });


                break;
            case 6:
                getDrawer().closeDrawer(Gravity.LEFT);

                SwitchDialog msg = switchDialogShow(UUtils.getString(R.string.警告), UUtils.getString(R.string.该操作有风险));

                msg.getCancel().setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        msg.dismiss();

                    }
                });
                msg.getOk().setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        msg.dismiss();
                        File file = new File(FileUrl.INSTANCE.getSmsUrl());
                        if (file.exists()) {

                            UUtils.showMsg(UUtils.getString(R.string.您已安装工具));

                        } else {


                            UUtils.writerFile("runcommand/smsread", new File(FileUrl.INSTANCE.getSmsUrl()));
                            UUtils.writerFile("runcommand/readcontacts", new File(FileUrl.INSTANCE.getPhoneUrl()));

                            TermuxActivity.mTerminalView.sendTextToTerminal(CodeString.INSTANCE.getRunsmsChomdSh());
                            TermuxActivity.mTerminalView.sendTextToTerminal(CodeString.INSTANCE.getRunPhoneChomdSh());

                            UUtils.showMsg(UUtils.getString(R.string.安装完成));


                        }

                    }
                });

                break;

            //快速
            case 10:
                UUtils.showLog("插件:快速");
                getDrawer().closeDrawer(Gravity.LEFT);
                try {

                    Intent intent = new Intent();
                    intent.setAction("com.utermux.action.vnc");
                    intent.putExtra("utermux_as", "false");
                    intent.putExtra("address", "127.0.0.1");
                    intent.putExtra("port", "5901");
                    intent.putExtra("password", "123456");

                    startActivity(intent);

                } catch (Exception e) {
                    e.printStackTrace();
                    UUtils.showLog("插件:" + e.toString());
                    UUtils.showMsg(UUtils.getString(R.string.请下载插件));
                    startHttp1(HTTPIP.IP);
                }


                break;


            //自定
            case 11:
                getDrawer().closeDrawer(Gravity.LEFT);


                VNCConnectionDialog vncConnectionDialog = new VNCConnectionDialog(this);

                vncConnectionDialog.show();

                vncConnectionDialog.setCancelable(false);


                vncConnectionDialog.getOk().setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {


                        try {
                            Intent intent = new Intent();
                            intent.setAction("com.utermux.action.vnc");
                            intent.putExtra("utermux_as", "false");
                            intent.putExtra("address", vncConnectionDialog.getAddress().getText().toString());
                            intent.putExtra("port", vncConnectionDialog.getPort().getText().toString());
                            intent.putExtra("password", vncConnectionDialog.getPassword().getText().toString());
                            startActivity(intent);
                        } catch (Exception e) {
                            e.printStackTrace();
                            UUtils.showLog("插件:" + e.toString());
                            UUtils.showMsg(UUtils.getString(R.string.请下载插件));
                            startHttp1(HTTPIP.IP);
                        }


                    }
                });


                break;
            //高级
            case 12:
                getDrawer().closeDrawer(Gravity.LEFT);

                try {
                    Intent intent = new Intent();
                    intent.setAction("com.utermux.action.vnc");
                    intent.putExtra("utermux_as", "true");
                    startActivity(intent);

                } catch (Exception e) {
                    e.printStackTrace();
                    UUtils.showLog("插件:" + e.toString());
                    UUtils.showMsg(UUtils.getString(R.string.请下载插件));
                    startHttp1(HTTPIP.IP);
                }

                break;
            //API
            case 20:
                getDrawer().closeDrawer(Gravity.LEFT);
                try {
                    installApk(getAssets().open("apk/termux_api.ip"), "termux_api");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            //Tasker
            case 21:
                getDrawer().closeDrawer(Gravity.LEFT);
                try {
                    installApk(getAssets().open("apk/termux_tasker.ip"), "termux_tasker");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            //BOOT
            case 22:
                getDrawer().closeDrawer(Gravity.LEFT);
                try {
                    installApk(getAssets().open("apk/termux_boot.ip"), "termux_boot");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;

            //styling
            case 23:
                getDrawer().closeDrawer(Gravity.LEFT);
                try {
                    installApk(getAssets().open("apk/termux_styling.ip"), "termux_styling");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;

            //x11
            case 24:
                getDrawer().closeDrawer(Gravity.LEFT);
                try {
                    installApk(getAssets().open("apk/termux_x11.ip"), "termux_x11");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;

            //中文
            case 30:
                //  Intent intent = new Intent(this, TermuxActivity.class);
                LocaleHelper.Companion.getInstance()
                    .language(getLocale("2")).apply(this);
                // startActivity(intent);


                break;
            //英文
            case 31:
                LocaleHelper.Companion.getInstance()
                    .language(getLocale("1")).apply(this);
                break;
        }

    }


    private SwitchDialog switchDialogShow(String title, String msg) {

        getDrawer().closeDrawer(Gravity.LEFT);
        SwitchDialog switchDialog = new SwitchDialog(this);

        switchDialog.getTitle().setText(title);
        switchDialog.getMsg().setText(msg);
        switchDialog.getOther().setVisibility(View.GONE);
        switchDialog.getOk().setText(UUtils.getString(R.string.确定));
        switchDialog.getCancel().setText(UUtils.getString(R.string.取消));

        switchDialog.show();


        return switchDialog;

    }


    @Override
    public void doubleClicke(float x) {


        int width = getWindow().getWindowManager().getDefaultDisplay().getWidth();
        UUtils.showLog("点击位置:" + x + " 屏幕总宽度:" + width);

        if (x <= 100) {

            getDrawer().openDrawer(Gravity.LEFT);
            return;
        }

        if (x >= width - 100) {
            getDrawer().openDrawer(Gravity.RIGHT);
            return;
        }
        BoomWindow.SWITCH = false;
        showBoomDialog(false);
    }

    private void showBoomDialog(boolean bln) {
        final PopupWindow[] popupWindow = {new PopupWindow()};
        final BoomWindow[] boomWindow = {new BoomWindow()};
        boomWindow[0].setCalculateWindows(new BoomWindow.CalculateWindows() {
            @Override
            public void windowsVariety(boolean bln) {
                popupWindow[0].dismiss();
                popupWindow[0] = null;
                showBoomDialog(bln);
            }
        });
        popupWindow[0].setContentView(boomWindow[0].getView(new BoomMinLAdapter.CloseLiftListener() {
            @Override
            public void close() {
                popupWindow[0].dismiss();
            }
        }, TermuxActivity.this, popupWindow[0]));
        popupWindow[0].setOutsideTouchable(true);
        //  popupWindow.setAnimationStyle(R.style.Animation);
        popupWindow[0].setWidth(ViewGroup.LayoutParams.MATCH_PARENT);
        popupWindow[0].setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        popupWindow[0].showAsDropDown(mTerminalView, 0, -boomWindow[0].getHigh(bln));
        popupWindow[0].setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                boomWindow[0] = null;
            }
        });
        boomWindow[0].popu_windows_huihua.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mTermuxTerminalSessionActivityClient.addNewSession(false, null);
                popupWindow[0].dismiss();

            }
        });
        boomWindow[0].popu_windows_jianpan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, 0);
                getDrawer().closeDrawers();
                popupWindow[0].dismiss();
            }
        });
    }

    //判断短消息是否正在读取
    private boolean isPhoneRun = false;

    private void resBroadcastReceiever(String msg) {
        Logger.logDebug(LOG_TAG, "resBroadcastReceiever start:" + msg);
        if (msg == null) {
            return;
        }

        if (msg.equals("readsms")) {

            boolean vim = IsInstallCommand.INSTANCE.isInstall(this, "vim", CodeString.INSTANCE.getRunsmsInstallSh());

            if (vim) {

                XXPermissions.with(this)
                    .permission(Permission.READ_SMS)
                    .request(new OnPermissionCallback() {

                        @Override
                        public void onGranted(List<String> permissions, boolean all) {
                            if (all) {
                                // UUtils.showMsg("获取录音和日历权限成功");

                                String smsInPhone = SmsUtils.getSmsInPhone();
                                UUtils.setFileString(new File(FileUrl.INSTANCE.getSmsUrlFile()), smsInPhone);
                                UUtils.sleepSetRunMm(new Runnable() {
                                    @Override
                                    public void run() {
                                        TermuxActivity.mTerminalView.sendTextToTerminal("cd ~ && cd ~ && vim sms.txt \n");
                                    }
                                }, 100);

                            } else {
                                // UUtils.showMsg(("获取部分权限成功，但部分权限未正常授予"));
                                TermuxActivity.mTerminalView.sendTextToTerminal("echo " + UUtils.getString(R.string.无权限读取) + "! \n");
                            }
                        }

                        @Override
                        public void onDenied(List<String> permissions, boolean never) {
                            if (never) {
                                TermuxActivity.mTerminalView.sendTextToTerminal("echo " + UUtils.getString(R.string.无权限读取) + "! \n");
                                // 如果是被永久拒绝就跳转到应用权限系统设置页面
                                XXPermissions.startPermissionActivity(TermuxActivity.this, permissions);
                            } else {
                                TermuxActivity.mTerminalView.sendTextToTerminal("echo " + UUtils.getString(R.string.无权限读取) + "! \n");
                            }
                        }
                    });

            }

        }

        //联系人
        if (msg.equals("contactperson")) {

            if (!isPhoneRun) {

                synchronized (TermuxActivity.class) {

                    isPhoneRun = true;

                    boolean vim = IsInstallCommand.INSTANCE.isInstall(this, "vim", CodeString.INSTANCE.getRunsmsInstallSh());

                    if (vim) {

                        XXPermissions.with(this)
                            .permission(Permission.READ_CONTACTS)
                            .request(new OnPermissionCallback() {

                                @Override
                                public void onGranted(List<String> permissions, boolean all) {

                                    if (all) {
                                        // UUtils.showMsg("获取录音和日历权限成功");

                                        LoadingDialog loadingDialog = new LoadingDialog(TermuxActivity.this);
                                        loadingDialog.show();

                                        new Thread(new Runnable() {
                                            @Override
                                            public void run() {
                                                String allContacts = PhoneUtils.getAllContacts(UUtils.getContext());
                                                UUtils.setFileString(new File(FileUrl.INSTANCE.getPhoneUrlFile()), allContacts);
                                                UUtils.sleepSetRunMm(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        TermuxActivity.this.runOnUiThread(new Runnable() {
                                                            @Override
                                                            public void run() {
                                                                isPhoneRun = false;
                                                                loadingDialog.dismiss();
                                                                TermuxActivity.mTerminalView.sendTextToTerminal("cd ~ && cd ~ && vim phone.txt \n");
                                                            }
                                                        });
                                                    }
                                                }, 100);
                                            }
                                        }).start();

                                    } else {
                                        isPhoneRun = false;
                                        // UUtils.showMsg(("获取部分权限成功，但部分权限未正常授予"));
                                        TermuxActivity.mTerminalView.sendTextToTerminal("echo " + UUtils.getString(R.string.无权限读取) + "! \n");
                                    }
                                }

                                @Override
                                public void onDenied(List<String> permissions, boolean never) {
                                    isPhoneRun = false;
                                    if (never) {
                                        TermuxActivity.mTerminalView.sendTextToTerminal("echo " + UUtils.getString(R.string.无权限读取) + "! \n");
                                        // 如果是被永久拒绝就跳转到应用权限系统设置页面
                                        XXPermissions.startPermissionActivity(TermuxActivity.this, permissions);
                                    } else {
                                        TermuxActivity.mTerminalView.sendTextToTerminal("echo " + UUtils.getString(R.string.无权限读取) + "! \n");
                                    }
                                }
                            });

                    } else {
                        isPhoneRun = false;
                    }

                }


            } else {

                TermuxActivity.mTerminalView.sendTextToTerminal("echo " + UUtils.getString(R.string.请等待) + "! \n");

            }


        }


        if (msg.equals("left")) {

            getDrawer().openDrawer(Gravity.LEFT);

        }

        if (msg.equals("right")) {

            getDrawer().openDrawer(Gravity.RIGHT);

        }


    }


    private void installApk(InputStream inputStream, String fileName) {


        SwitchDialog switchDialog1 = switchDialogShow(UUtils.getString(R.string.警告), UUtils.getString(R.string.您未安装该插件));

        switchDialog1.getCancel().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchDialog1.dismiss();

            }
        });
        switchDialog1.getOk().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchDialog1.dismiss();


                XXPermissions.with(TermuxActivity.this)
                    .permission(Permission.WRITE_EXTERNAL_STORAGE)
                    .permission(Permission.READ_EXTERNAL_STORAGE)
                    .permission(Permission.REQUEST_INSTALL_PACKAGES)
                    .request(new OnPermissionCallback() {

                        @Override
                        public void onGranted(List<String> permissions, boolean all) {
                            if (all) {
                                if (!FileUrl.INSTANCE.getZeroTermuxApk().exists()) {
                                    FileUrl.INSTANCE.getZeroTermuxApk().mkdirs();
                                }
                                File file1 = new File(Environment.getExternalStorageDirectory(), "/xinhao/apk/" + fileName + ".apk");
                                LoadingDialog loadingDialog = new LoadingDialog(TermuxActivity.this);
                                loadingDialog.show();
                                UUtils.writerFileRawInput(file1, inputStream);

                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {

                                        try {
                                            Thread.sleep(2000);
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }

                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                loadingDialog.dismiss();
                                                UUtils.installApk(UUtils.getContext(), file1.getAbsolutePath());
                                            }
                                        });

                                    }
                                }).start();


                            } else {

                                UUtils.showMsg("无权限");
                            }
                        }

                        @Override
                        public void onDenied(List<String> permissions, boolean never) {
                            if (never) {
                                UUtils.showMsg("无权限");
                                // 如果是被永久拒绝就跳转到应用权限系统设置页面
                                XXPermissions.startPermissionActivity(TermuxActivity.this, permissions);
                            } else {
                                UUtils.showMsg("无权限");
                            }
                        }
                    });


            }
        });


    }


    //创建目录

    private void createFiles() {

     /*   File file = new File(FileUrl.INSTANCE.getMainConfigUrl());
        File fileProperties = new File(FileUrl.INSTANCE.getMainConfigUrl(), "/termux.properties");

        if(!file.exists()){
            file.mkdirs();
        }
        if(!fileProperties.exists()){

            UUtils.writerFile("properties/termux.properties",fileProperties);
            reloadActivityStyling();
        }*/

        if (!FileUrl.INSTANCE.getZeroTermuxHome().exists()) {


            /**
             *
             * 防止某些手机重复显示此Dialog
             *
             *
             */
            String sdcard_xinhao = SaveData.INSTANCE.getStringOther("sdcard_xinhao");

            if (sdcard_xinhao == null || sdcard_xinhao.isEmpty() || sdcard_xinhao.equals("def")) {

                SaveData.INSTANCE.saveStringOther("sdcard_xinhao", "true");

                SwitchDialog switchDialog2 = switchDialogShow(UUtils.getString(R.string.警告), UUtils.getString(R.string.需要在您的手机));

                switchDialog2.getCancel().setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        switchDialog2.dismiss();

                        finish();
                    }
                });
                switchDialog2.setCancelable(false);
                switchDialog2.getOk().setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        switchDialog2.dismiss();

                        XXPermissions.with(TermuxActivity.this)
                            .permission(Permission.WRITE_EXTERNAL_STORAGE)
                            .permission(Permission.READ_EXTERNAL_STORAGE)
                            .request(new OnPermissionCallback() {

                                @Override
                                public void onGranted(List<String> permissions, boolean all) {
                                    if (all) {

                                        if (!FileUrl.INSTANCE.getZeroTermuxHome().exists()) {
                                            FileUrl.INSTANCE.getZeroTermuxHome().mkdirs();
                                        }
                                        if (!FileUrl.INSTANCE.getZeroTermuxData().exists()) {
                                            FileUrl.INSTANCE.getZeroTermuxData().mkdirs();
                                        }
                                        if (!FileUrl.INSTANCE.getZeroTermuxApk().exists()) {
                                            FileUrl.INSTANCE.getZeroTermuxApk().mkdirs();
                                        }
                                        if (!FileUrl.INSTANCE.getZeroTermuxWindows().exists()) {
                                            FileUrl.INSTANCE.getZeroTermuxWindows().mkdirs();
                                        }
                                        if (!FileUrl.INSTANCE.getZeroTermuxCommand().exists()) {
                                            FileUrl.INSTANCE.getZeroTermuxCommand().mkdirs();
                                        }
                                        if (!FileUrl.INSTANCE.getZeroTermuxFont().exists()) {
                                            FileUrl.INSTANCE.getZeroTermuxFont().mkdirs();
                                        }
                                        if (!FileUrl.INSTANCE.getZeroTermuxIso().exists()) {
                                            FileUrl.INSTANCE.getZeroTermuxIso().mkdirs();
                                        }
                                        if (!FileUrl.INSTANCE.getZeroTermuxMysql().exists()) {
                                            FileUrl.INSTANCE.getZeroTermuxMysql().mkdirs();
                                        }
                                        if (!FileUrl.INSTANCE.getZeroTermuxOnlineSystem().exists()) {
                                            FileUrl.INSTANCE.getZeroTermuxOnlineSystem().mkdirs();
                                        }
                                        if (!FileUrl.INSTANCE.getZeroTermuxQemu().exists()) {
                                            FileUrl.INSTANCE.getZeroTermuxQemu().mkdirs();
                                        }
                                        if (!FileUrl.INSTANCE.getZeroTermuxServer().exists()) {
                                            FileUrl.INSTANCE.getZeroTermuxServer().mkdirs();
                                        }
                                        if (!FileUrl.INSTANCE.getZeroTermuxShare().exists()) {
                                            FileUrl.INSTANCE.getZeroTermuxShare().mkdirs();
                                        }
                                        if (!FileUrl.INSTANCE.getZeroTermuxSystem().exists()) {
                                            FileUrl.INSTANCE.getZeroTermuxSystem().mkdirs();
                                        }
                                        if (!FileUrl.INSTANCE.getZeroTermuxWebConfig().exists()) {
                                            FileUrl.INSTANCE.getZeroTermuxWebConfig().mkdirs();
                                        }
                                        UUtils.showMsg("ok");
                                    } else {

                                        UUtils.showMsg("无权限");
                                    }
                                }

                                @Override
                                public void onDenied(List<String> permissions, boolean never) {
                                    if (never) {
                                        UUtils.showMsg("无权限");
                                        // 如果是被永久拒绝就跳转到应用权限系统设置页面
                                        XXPermissions.startPermissionActivity(TermuxActivity.this, permissions);
                                    } else {
                                        UUtils.showMsg("无权限");
                                    }
                                }
                            });


                    }
                });
            } else {
            }
        }
    }

    /**
     * 重新进入该窗口面板默认关闭
     */
    private void isShow() {
        title_mb.setVisibility(View.GONE);
        getDrawer().close();
    }

    /**
     * 连接到服务器
     */

    public void startHttp(String ip) {
        String ip_save = SaveData.INSTANCE.getStringOther("ip_save");
        if (ip_save == null || ip_save.isEmpty() || ip_save.equals("def")) {
            ArrayList<EditPromptBean.EditPromptData> arrayList = new ArrayList<>();
            EditPromptBean.EditPromptData editPromptData = new EditPromptBean.EditPromptData();
            editPromptData.setIp(ip);
            editPromptData.setConnection(0);
            arrayList.add(editPromptData);
            EditPromptBean editPromptBean = new EditPromptBean();
            editPromptBean.setArrayList(arrayList);
            String s = new Gson().toJson(editPromptBean);
            UUtils.showLog("编辑框存入[第一次]:" + s);
            SaveData.INSTANCE.saveStringOther("ip_save", s);
        } else {
            try {
                EditPromptBean editPromptBean = new Gson().fromJson(ip_save, EditPromptBean.class);
                ArrayList<EditPromptBean.EditPromptData> arrayList = editPromptBean.getArrayList();
                EditPromptBean.EditPromptData editPromptData = new EditPromptBean.EditPromptData();
                editPromptData.setIp(ip);
                editPromptData.setConnection(0);
                arrayList.add(editPromptData);
                ArrayList<EditPromptBean.EditPromptData> arrayList1 = UUUtils.removeDuplicate_2(arrayList);
                editPromptBean.setArrayList(arrayList1);
                String s = new Gson().toJson(editPromptBean);
                UUtils.showLog("编辑框存入[多次]:" + s);
                SaveData.INSTANCE.saveStringOther("ip_save", s);
            } catch (Exception e) {
                e.printStackTrace();
                SaveData.INSTANCE.saveStringOther("ip_save", "def");
            }
        }
        LoadingDialog loadingDialog = new LoadingDialog(TermuxActivity.this);
        loadingDialog.getMsg().setText(UUtils.getString(R.string.正在连接到自定义服务器));
        loadingDialog.show();
        new BaseHttpUtils().getUrl(ip + "/repository/main.json", new HttpResponseListenerBase() {
            @Override
            public void onSuccessful(@NotNull Message msg, int mWhat) {
                loadingDialog.dismiss();
                UUtils.showLog("连接成功:" + msg.obj);

                try {
                    ZDYDataBean zdyDataBean = new Gson().fromJson((String) msg.obj, ZDYDataBean.class);

                    DownLoadDialogBoom downLoadDialogBoom = new DownLoadDialogBoom(TermuxActivity.this);
                    downLoadDialogBoom.setIP(ip + "/repository/main.json");
                    downLoadDialogBoom.show();
                    downLoadDialogBoom.setCancelable(true);
                } catch (Exception e) {
                    e.printStackTrace();
                    UUtils.showMsg(UUtils.getString(R.string.服务器数据格式不正确));
                }

            }

            @Override
            public void onFailure(@org.jetbrains.annotations.Nullable Response<String> response, @NotNull String msg, int mWhat) {
                loadingDialog.dismiss();
                UUtils.showMsg(UUtils.getString(R.string.无法连接到自定义服务器));
            }
        }, new HashMap<>(), 5555);


    }


    /**
     * 连接到服务器
     */

    public void startHttp1(String ip) {


        XXPermissions.with(TermuxActivity.this)
            .permission(Permission.WRITE_EXTERNAL_STORAGE)
            .permission(Permission.READ_EXTERNAL_STORAGE)
            .request(new OnPermissionCallback() {

                @Override
                public void onGranted(List<String> permissions, boolean all) {
                    if (all) {

                        LoadingDialog loadingDialog = new LoadingDialog(TermuxActivity.this);

                        loadingDialog.getMsg().setText(UUtils.getString(R.string.正在连接到下载站服务器));

                        loadingDialog.show();


                        new BaseHttpUtils().getUrl(ip + "/repository/main.json", new HttpResponseListenerBase() {
                            @Override
                            public void onSuccessful(@NotNull Message msg, int mWhat) {
                                loadingDialog.dismiss();
                                UUtils.showLog("连接成功:" + msg.obj);

                                try {
                                    ZDYDataBean zdyDataBean = new Gson().fromJson((String) msg.obj, ZDYDataBean.class);

                                    DownLoadDialogBoom downLoadDialogBoom = new DownLoadDialogBoom(TermuxActivity.this);
                                    downLoadDialogBoom.setIP(ip + "/repository/main.json");
                                    downLoadDialogBoom.show();
                                    downLoadDialogBoom.setCancelable(true);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    UUtils.showMsg(UUtils.getString(R.string.服务器数据格式不正确));
                                }

                            }

                            @Override
                            public void onFailure(@org.jetbrains.annotations.Nullable Response<String> response, @NotNull String msg, int mWhat) {
                                loadingDialog.dismiss();
                                UUtils.showMsg(UUtils.getString(R.string.无法连接到下载站服务器));
                            }
                        }, new HashMap<>(), 5555);


                    } else {

                        UUtils.showMsg(UUtils.getString(R.string.没有权限));
                    }
                }

                @Override
                public void onDenied(List<String> permissions, boolean never) {
                    if (never) {
                        UUtils.showMsg(UUtils.getString(R.string.没有权限));
                        // 如果是被永久拒绝就跳转到应用权限系统设置页面
                        XXPermissions.startPermissionActivity(TermuxActivity.this, permissions);
                    } else {
                        UUtils.showMsg(UUtils.getString(R.string.没有权限));
                    }
                }
            });


    }


    /**
     * 连接服务器获取版本
     */

    private void getServiceVs() {

        ip_status.setText(UUtils.getHostIP());
        new BaseHttpUtils().getUrl(HTTPIP.IP + "/repository/main.json", new HttpResponseListenerBase() {
            @Override
            public void onSuccessful(@NotNull Message msg, int mWhat) {

                UUtils.showLog("连接成功:" + msg.obj);
                LogUtils.d(TAG, "getServiceVs onSuccessful connection succeeded");

                try {
                    ZDYDataBean zdyDataBean = new Gson().fromJson((String) msg.obj, ZDYDataBean.class);

                    service_status.setText(zdyDataBean.getVersionName());
                    service_eg.setText(zdyDataBean.getEngineName());

                    if (zdyDataBean.getMsg() == null || zdyDataBean.getMsg().isEmpty()) {

                    } else {
                        msg_tv.setText(zdyDataBean.getMsg());
                    }


                } catch (Exception e) {
                    e.printStackTrace();
                    // UUtils.showMsg(UUtils.getString(R.string.服务器数据格式不正确));
                    service_status.setText(UUtils.getString(R.string.未连接));
                    LogUtils.d(TAG, "getServiceVs connection error:" + e.toString());
                }

            }

            @Override
            public void onFailure(@org.jetbrains.annotations.Nullable Response<String> response, @NotNull String msg, int mWhat) {

                // UUtils.showMsg(UUtils.getString(R.string.无法连接到下载站服务器));
                service_status.setText(UUtils.getString(R.string.未连接));
                service_eg.setText(UUtils.getString(R.string.未连接));
                LogUtils.d(TAG, "getServiceVs data error:" + response.getException());
            }
        }, new HashMap<>(), 5555);


    }

    private Locale getLocale(String which) {


        switch (which) {


            case "0":
                return Locale.ROOT;
            case "1":
                return Locale.ENGLISH;
            case "2":
            default:
                return Locale.SIMPLIFIED_CHINESE;
        }
    }

    private void initFiles() {
        File mainFile = new File(FileUrl.INSTANCE.getMainBinUrl());
        File openLeftFile = new File(FileUrl.INSTANCE.getOpenLeft());
        File openRigthFile = new File(FileUrl.INSTANCE.getOpenRight());
        if (mainFile.exists()) {

            if (!openLeftFile.exists()) {
                UUtils.writerFile("runcommand/openleftwindow", openLeftFile);
                UUtils.chmod(openLeftFile);
            }
            if (!openRigthFile.exists()) {
                UUtils.writerFile("runcommand/openrightwindow", openRigthFile);
                UUtils.chmod(openRigthFile);
            }

        }

    }

    /**
     * OTG 设备广播  暂时无法使用
     */
    BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (mOTGManager == null) {
                mOTGManager = new OTGManager();
            }
            //  mOTGManager.initOtg(TermuxActivity.this, intent);
        }
    };

    //监听菜单键
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_MENU)) {
            if (getDrawer().isOpen()) {
                getDrawer().closeDrawers();
            } else {
                getDrawer().openDrawer(Gravity.LEFT);
            }
            return false;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void initScrollHandler() {
        mDataMessage.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                //canScrollVertically()方法为判断指定方向上是否可以滚动,参数为正数或负数,负数检查向上是否可以滚动,正数为检查向下是否可以滚动
                if (mDataMessage.canScrollVertically(1) || mDataMessage.canScrollVertically(-1)) {
                    v.getParent().requestDisallowInterceptTouchEvent(true);//requestDisallowInterceptTouchEvent();要求父类布局不在拦截触摸事件
                    if (event.getAction() == MotionEvent.ACTION_UP) { //判断是否松开
                        v.getParent().requestDisallowInterceptTouchEvent(false); //requestDisallowInterceptTouchEvent();让父类布局继续拦截触摸事件
                    }
                }
                return false;
            }
        });

    }

    //测试方法
    private void testFun() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                FileHttpUtils.Companion.get().bootHttp();
            }
        }).start();
    }

    private void indexSwitch(int index) {
        frame_file.setVisibility(View.INVISIBLE);
        session_rl.setVisibility(View.INVISIBLE);
        switch (index) {
            case 0:
                frame_file.setVisibility(View.VISIBLE);
                break;
            case 1:
                session_rl.setVisibility(View.VISIBLE);
                break;
        }
    }

    private void fileManager() {
        if (frame_file.getChildCount() != 0) {
            return;
        }
        FragmentTransaction fragmentTransaction = this.getSupportFragmentManager()
            .beginTransaction();

        if (fragmentTransaction.isEmpty()) {
            fragmentTransaction.add(R.id.frame_file, ZFileListFragment.newInstance(), "ZFileListFragment")
                .commit();

            ZTConfig.INSTANCE.setCloseListener(new CloseListener() {
                @Override
                public void close() {
                    getDrawer().closeDrawers();
                }
            });
        }
    }

    private void locaBroadcast() {
       localBroadcastManager  = LocalBroadcastManager.getInstance(this);
        IntentFilter intentFilter = new IntentFilter("localbroadcast");
        localReceiver = new LocalReceiver();
        localBroadcastManager.registerReceiver(localReceiver,intentFilter);
    }

}
