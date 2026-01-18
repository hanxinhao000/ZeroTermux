package com.termux.zerocore.deepseek;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.xh_lib.utils.LogUtils;
import com.example.xh_lib.utils.UUtils;
import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.zerocore.deepseek.data.ChatDatabaseHelper;
import com.termux.zerocore.deepseek.data.ChatMessage;
import com.termux.zerocore.deepseek.data.ChatMessageAdapter;
import com.termux.zerocore.deepseek.data.ChatSession;
import com.termux.zerocore.deepseek.model.Config;
import com.termux.zerocore.deepseek.model.DeepSeekClient;
import com.termux.zerocore.deepseek.model.RequestMessageItem;
import com.termux.zerocore.ftp.utils.UserSetManage;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ChatFragment extends Fragment {
    private static final String TAG = ChatFragment.class.getSimpleName();
    private RecyclerView chatRecyclerView;
    private ImageView mCancel;
    private EditText messageInput;
    private TextView sendButton;
    private ChatSession currentSession;
    private View mView;
    private Intent mIntent;

    private DeepSeekClient deepSeekClient = new DeepSeekClient();
    private List<RequestMessageItem> requestMessageItemList = new ArrayList<>();
    private static ChatFragment chatFragment;

    private TextView testText;

    private ChatDatabaseHelper dbHelper;
    private List<ChatMessage> messages = new ArrayList<>();
    private ChatMessageAdapter adapter;
    private DeepSeekTransitFragment mDeepSeekTransitFragment;

    private String sessionId;

    private boolean newMsg = true;

    private boolean createS = false;

    public static ChatFragment newInstance() {
        if (chatFragment == null) {
            synchronized (ChatFragment.class) {
                if (chatFragment == null) {
                    chatFragment = new ChatFragment();
                }
                return chatFragment;
            }
        } else {
            return chatFragment;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mView = View.inflate(getContext(), R.layout.activity_chat, null);
        initView();
        return mView;
    }

    public void setIntent(Intent intent) {
        this.mIntent = intent;
    }

    public void setDeepSeekTransitFragment(DeepSeekTransitFragment deepSeekTransitFragment) {
        mDeepSeekTransitFragment = deepSeekTransitFragment;
    }

    private void initView() {
        dbHelper = new ChatDatabaseHelper(getContext());

        chatRecyclerView = mView.findViewById(R.id.chatRecyclerView);
        mCancel = mView.findViewById(R.id.cancel);
        messageInput = mView.findViewById(R.id.messageInput);
        sendButton = mView.findViewById(R.id.sendButton);
        testText = mView.findViewById(R.id.testText);

        if (mIntent == null) {
            LogUtils.e(TAG, "initView intent is null return.");
            return;
        }

        boolean isNew = mIntent.getBooleanExtra("isNew", false);
        if (isNew) {
            createS = true;
            sessionId = UUID.randomUUID().toString();
        } else {
            sessionId = mIntent.getStringExtra("sessionId");
            messages.addAll(dbHelper.getMessagesForSession(sessionId));
        }
        mCancel.setOnClickListener(view -> {
            mDeepSeekTransitFragment.switchFragment(0, null);
            messages.clear();
        });
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ChatMessageAdapter(getContext(), messages);
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        chatRecyclerView.setAdapter(adapter);
        adapter.notifyItemInserted(messages.size() - 1);

        sendButton.setOnClickListener(v -> sendMessage());
       // scrollToBottom();
        chatRecyclerView.scrollToPosition(adapter.getItemCount() - 1);
    }

    private void sendMessage() {
        hideSoftInput(messageInput);
        String message = messageInput.getText().toString();
        if (!message.isEmpty()) {
            up(message);

            dbHelper.insertMessage(sessionId, message, true, System.currentTimeMillis(), 0);
            messages.add(new ChatMessage(message, true, System.currentTimeMillis(), 0));
            adapter.notifyDataSetChanged();
            adapter.notifyItemInserted(messages.size() - 1);
            messageInput.setText("");
            creareS(message);
        }
        scrollToBottom();
    }

    // 新增滚动到底部的方法
    private void scrollToBottom() {
        if (chatRecyclerView != null && adapter != null) {
            // 使用 post 确保在 UI 线程执行，并且在布局更新后执行
            chatRecyclerView.post(() -> {
                if (adapter.getItemCount() > 0) {
                    chatRecyclerView.scrollToPosition(adapter.getItemCount() - 1);
                }
            });
        }
    }

    private void hideSoftInput(EditText editText) {
        if (editText == null)
            return;
        InputMethodManager inputMethodManager = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (inputMethodManager == null)
            return;
        editText.clearFocus();
        inputMethodManager.hideSoftInputFromWindow(editText.getWindowToken(), 0);
    }

    private void creareS(String name) {
        if (createS) {
            currentSession = new ChatSession(sessionId, name, System.currentTimeMillis());
            dbHelper.insertSession(currentSession.getSessionId(), currentSession.getSessionName());
        }
        createS = false;
    }

    private void up(String message) {
        testText.setText("");
        sendButton.setText("...");
        mCancel.setVisibility(View.GONE);
        sendButton.setEnabled(false);
        reqModel(message);
    }

    StringBuilder mText = new StringBuilder();

    private void reqModel(String text) {

        requestMessageItemList.add(new RequestMessageItem("user", text));
        Log.i(TAG, "reqModelxxxxxxxxx: " + getPrompt());
        requestMessageItemList.add(new RequestMessageItem("system", getPrompt()));

        deepSeekClient.ask(requestMessageItemList, true, new DeepSeekClient.Lis() {
            @Override
            public void error() {
                System.out.println("\n处理失败（服务器响应超时）");
                input();
            }

            @Override
            public void msg(String msg, boolean isError) {
                String mMsg = deepSeekClient.getMsg(msg);
                LogUtils.e(TAG, "end insertMessage msg mMsg: " + mMsg);
                mText.append(mMsg);
                UUtils.runOnUIThread(() -> {
                    LogUtils.e(TAG, "end insertMessage msg mText: " + mText);
                    localProcessingMessage(mMsg, isError);
                });
            }

            @Override
            public void end() {
                input();
                LogUtils.e(TAG, "end insertMessage mText: " + mText);
                if (mText.length() > 0) {
                    UUtils.runOnUIThread(() -> {
                        String msg = mText.toString();
                        LogUtils.e(TAG, "end insertMessage sessionId: " + sessionId + " ,msg: " + msg);
                        dbHelper.insertMessage(sessionId, msg, false, System.currentTimeMillis(), 1);
                        mText.delete(0, mText.length());
                        newMsg = true;
                    });
                }

            }
        });
    }


    private void input() {
        UUtils.runOnUIThread(() -> {
            sendButton.setText("send");
            mCancel.setVisibility(View.VISIBLE);
            sendButton.setEnabled(true);
        });
    }

    private void updateM(String msg, boolean isError) {
        LogUtils.e(TAG, "updateM newMsg: " + newMsg
            + " ,isError: " + isError
            + " ,msg: " + msg
        );
        if (newMsg || isError) {
            messages.add(new ChatMessage(msg, false, System.currentTimeMillis(), 1));
            adapter.notifyItemInserted(messages.size() - 1);
            newMsg = false;
        } else {
            adapter.updateMessageText(messages.size() - 1, msg);
            scrollToBottom();
        }
    }

    private void localProcessingMessage(String msg, boolean isError) {
        updateM(msg, isError);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mDeepSeekTransitFragment = null;
    }

    // 获取终端助手提示语
    private String getPrompt() {
        boolean isDeepSeekVisibleTerminal = UserSetManage.Companion.get().getZTUserBean().isIsDeepSeekVisibleTerminal();
        String terminalCommands = TermuxActivity.mTerminalView.getText555()
            .replace("$", "")
            .replace("~", "")
            .replace("\n", "")
            .trim();
        if (terminalCommands.length() > Config.MAX_VISIBLE) {
            terminalCommands = terminalCommands.substring(terminalCommands.length() - Config.MAX_VISIBLE);
        }
        LogUtils.e(TAG, "getPrompt: " + terminalCommands);
        if (isDeepSeekVisibleTerminal) {
            return UUtils.getString(R.string.deepseek_zs) + UUtils.getString(R.string.deepseek_zs_command) + terminalCommands;
        } else {
            return UUtils.getString(R.string.deepseek_zs);
        }
    }
}
