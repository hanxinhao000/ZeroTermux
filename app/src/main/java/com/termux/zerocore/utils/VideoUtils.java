package com.termux.zerocore.utils;

import android.media.MediaPlayer;
import android.widget.MediaController;
import android.widget.RelativeLayout;
import android.widget.VideoView;

import com.example.xh_lib.utils.LogUtils;
import com.example.xh_lib.utils.UUtils;

import java.io.File;

public class VideoUtils implements MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener {

    private static String TAG = "VideoUtils";
    private VideoUtils() {

    }
    private VideoView mVideoView;

    private static VideoUtils mVideoUtils = null;

    public static VideoUtils getInstance() {
        if (mVideoUtils == null) {
            synchronized (VideoUtils.class) {
                if (mVideoUtils == null) {
                    mVideoUtils = new VideoUtils();
                }
                return mVideoUtils;
            }
        } else {
            return mVideoUtils;
        }
    }

    public void setVideoView(VideoView mVideoView) {
        this.mVideoView = mVideoView;
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        mVideoView.setLayoutParams(layoutParams);
    }

    public void start(File file) {
        if (mVideoView == null) {
            LogUtils.d(TAG, "start mVideoView isNull");
            return;
        }
        mVideoView.setVideoPath(file.getAbsolutePath());
        mVideoView.setOnPreparedListener(this);
        mVideoView.setOnCompletionListener(this);
    }

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        mediaPlayer.setVolume(0f, 0f);
        mediaPlayer.start();
    }

    public void pause() {
        if (mVideoView == null) {
            LogUtils.d(TAG, "pause mVideoView isNull");
            return;
        }
        if (mVideoView.isPlaying()) {
            mVideoView.pause();
        }
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        mediaPlayer.start();
        mediaPlayer.setLooping(true);
    }

    public void onResume() {
        if (mVideoView == null) {
            LogUtils.d(TAG, "onResume mVideoView isNull");
            return;
        }
        if (!mVideoView.isPlaying()) {
            mVideoView.setOnPreparedListener(this);
        }
    }

    public void onDestroy() {
        if (mVideoView == null) {
            LogUtils.d(TAG, "onDestroy mVideoView isNull");
            return;
        }
        mVideoView.stopPlayback();
    }
}
