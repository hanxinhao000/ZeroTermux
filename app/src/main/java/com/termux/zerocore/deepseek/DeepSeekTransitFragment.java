package com.termux.zerocore.deepseek;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.xh_lib.utils.LogUtils;
import com.termux.R;

public class DeepSeekTransitFragment extends Fragment {
    private static final String TAG = DeepSeekTransitFragment.class.getSimpleName();
    private View mView;
    private FrameLayout mFrameLayout;
    private static DeepSeekTransitFragment deepSeekTransitFragment;

    public static DeepSeekTransitFragment newInstance() {
        if (deepSeekTransitFragment == null) {
            synchronized (DeepSeekTransitFragment.class) {
                if (deepSeekTransitFragment == null) {
                    deepSeekTransitFragment = new DeepSeekTransitFragment();
                }
                return deepSeekTransitFragment;
            }
        } else {
            return deepSeekTransitFragment;
        }
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        LogUtils.e(TAG, "onAttach...");
        switchFragment(0, null);
    }

    @Override
    public void onResume() {
        super.onResume();
        LogUtils.e(TAG, "onResume...");
    }

    @Override
    public void onStart() {
        super.onStart();
        LogUtils.e(TAG, "onStart...");
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mView = View.inflate(getContext(), R.layout.fragment_transit_deepseek, null);
        initView();
        return mView;
    }

    private void initView() {
        mFrameLayout = mView.findViewById(R.id.frame_layout);
    }

    public void switchFragment(int index, Intent intent) {
        FragmentTransaction fragmentTransaction = this.getChildFragmentManager().beginTransaction();
        switch (index) {
            case 0:
                DeepSeekMainFragment deepSeekMainFragment = DeepSeekMainFragment.newInstance();
                deepSeekMainFragment.setDeepSeekTransitFragment(this);
                fragmentTransaction.replace(R.id.frame_layout, deepSeekMainFragment, "DeepSeekMainFragment")
                    .commitAllowingStateLoss();
                break;
            case 1:
                ChatFragment chatFragment = ChatFragment.newInstance();
                chatFragment.setIntent(intent);
                chatFragment.setDeepSeekTransitFragment(this);
                fragmentTransaction.replace(R.id.frame_layout, ChatFragment.newInstance(), "ChatFragment")
                    .commitAllowingStateLoss();
                break;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        deepSeekTransitFragment = null;
    }
}
