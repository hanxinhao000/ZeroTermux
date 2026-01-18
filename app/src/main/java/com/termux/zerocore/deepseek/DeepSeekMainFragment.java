package com.termux.zerocore.deepseek;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.termux.R;
import com.termux.zerocore.deepseek.activity.ZeroTermuxDeepSeekSettingsActivity;
import com.termux.zerocore.deepseek.data.ChatDatabaseHelper;
import com.termux.zerocore.deepseek.data.ChatSession;
import com.termux.zerocore.deepseek.data.ChatSessionAdapter;

import java.util.Collections;
import java.util.List;

public class DeepSeekMainFragment extends Fragment {
    private static DeepSeekMainFragment deepSeekMainFragment;
    private RecyclerView mRecyclerView;
    private ChatSessionAdapter adapter;
    private ChatDatabaseHelper dbHelper;
    private View mView;
    private TextView mChatEmpty;
    private ImageView mAddImageView;
    private ImageView mSettingsImageView;
    private DeepSeekTransitFragment mDeepSeekTransitFragment;

    public static DeepSeekMainFragment newInstance() {
        if (deepSeekMainFragment == null) {
            synchronized (DeepSeekMainFragment.class) {
                if (deepSeekMainFragment == null) {
                    deepSeekMainFragment = new DeepSeekMainFragment();
                }
                return deepSeekMainFragment;
            }
        } else {
            return deepSeekMainFragment;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mView = View.inflate(getContext(), R.layout.fragment_deepseek_main, null);
        initView();
        return mView;
    }

    private void initView() {
        dbHelper = new ChatDatabaseHelper(getContext());
        mRecyclerView = mView.findViewById(R.id.recyclerView);
        mChatEmpty = mView.findViewById(R.id.chat_empty);
        mAddImageView = mView.findViewById(R.id.add_img);
        mSettingsImageView = mView.findViewById(R.id.settings_img);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        mAddImageView.setOnClickListener(v -> startNewChat());
        List<ChatSession> allSessions = dbHelper.getAllSessions();
        if (allSessions == null || allSessions.isEmpty()) {
            mChatEmpty.setVisibility(View.VISIBLE);
            mRecyclerView.setVisibility(View.GONE);
        } else {
            mChatEmpty.setVisibility(View.GONE);
            mRecyclerView.setVisibility(View.VISIBLE);
        }
        mSettingsImageView.setOnClickListener(view -> {
            getContext().startActivity(new Intent(getContext(), ZeroTermuxDeepSeekSettingsActivity.class));
        });
    }

    public void setDeepSeekTransitFragment(DeepSeekTransitFragment deepSeekTransitFragment) {
        mDeepSeekTransitFragment = deepSeekTransitFragment;
    }

    private void startNewChat() {
        Intent intent = new Intent();
        intent.putExtra("isNew", true);
        intent.putExtra("createdAt", System.currentTimeMillis()); // 添加当前时间戳
        mDeepSeekTransitFragment.switchFragment(1, intent);
    }

    @Override
    public void onResume() {
        super.onResume();
        adapter = new ChatSessionAdapter(getContext(), dbHelper.getAllSessions(), mDeepSeekTransitFragment);
        mRecyclerView.setAdapter(adapter);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mDeepSeekTransitFragment = null;
        adapter.release();
    }
}
