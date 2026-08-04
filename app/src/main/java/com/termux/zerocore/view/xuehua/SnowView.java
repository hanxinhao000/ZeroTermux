package com.termux.zerocore.view.xuehua;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.View;

public class SnowView extends View {
    private static final int NUM_SNOWFLAKES = 150;
    private static final int DELAY = 5;

    private boolean run = true;
    private SnowFlake[] snowflakes;

    public SnowView(Context context) {
        super(context);
    }

    public SnowView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SnowView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    protected void resize(int width, int height) {
        if (!run || width <= 0 || height <= 0) {
            return;
        }
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.FILL);
        SnowFlake[] flakes = new SnowFlake[NUM_SNOWFLAKES];
        for (int i = 0; i < NUM_SNOWFLAKES; i++) {
            flakes[i] = SnowFlake.create(width, height, paint);
        }
        snowflakes = flakes;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (run && (w != oldw || h != oldh)) {
            resize(w, h);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (!run) {
            return;
        }
        SnowFlake[] flakes = snowflakes;
        if (flakes == null) {
            // onDraw 可能早于 onSizeChanged；有尺寸时再初始化，避免 NPE
            int w = getWidth();
            int h = getHeight();
            if (w > 0 && h > 0) {
                resize(w, h);
                flakes = snowflakes;
            }
            if (flakes == null) {
                return;
            }
        }
        for (SnowFlake snowFlake : flakes) {
            if (snowFlake != null) {
                snowFlake.draw(canvas);
            }
        }
        Handler handler = getHandler();
        if (handler != null) {
            handler.postDelayed(runnable, DELAY);
        }
    }

    private final Runnable runnable = new Runnable() {
        @Override
        public void run() {
            invalidate();
        }
    };
}
