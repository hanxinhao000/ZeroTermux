package com.example.xh_lib.glide_utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.BitmapResource;

import java.security.MessageDigest;

import androidx.annotation.NonNull;

/**
 * @author ZEL
 * @create By ZEL on 2020/8/13 16:01
 **/
public class CornerTransform implements Transformation<Bitmap> {
    private BitmapPool mBitmapPool;

    private float radius;

    private boolean exceptLeftTop, exceptRightTop, exceptLeftBottom, exceptRightBotoom;

    /**
     * 除了那几个角不需要圆角的
     *
     * @param leftTop
     * @param rightTop
     * @param leftBottom
     * @param rightBottom
     */
    public void setExceptCorner(boolean leftTop, boolean rightTop, boolean leftBottom, boolean rightBottom) {
        this.exceptLeftTop = leftTop;
        this.exceptRightTop = rightTop;
        this.exceptLeftBottom = leftBottom;
        this.exceptRightBotoom = rightBottom;
    }

    public CornerTransform(Context context, float radius) {
        this.mBitmapPool = Glide.get(context).getBitmapPool();
        this.radius = radius;
    }

    @NonNull
    @Override
    public Resource<Bitmap> transform(@NonNull Context context, @NonNull Resource<Bitmap> resource, int outWidth, int outHeight) {
        Bitmap source = resource.get();
        int finalWidth, finalHeight;
        float ratio; //输出目标的宽高或高宽比例
        if (outWidth > outHeight) { //输出宽度>输出高度,求高宽比
            ratio = (float) outHeight / (float) outWidth;
            finalWidth = source.getWidth();
            finalHeight = (int) ((float) source.getWidth() * ratio); //固定原图宽度,求最终高度
            if (finalHeight > source.getHeight()) { //求出的最终高度>原图高度,求宽高比
                ratio = (float) outWidth / (float) outHeight;
                finalHeight = source.getHeight();
                finalWidth = (int) ((float) source.getHeight() * ratio);//固定原图高度,求最终宽度
            }
        } else if (outWidth < outHeight) { //输出宽度 < 输出高度,求宽高比
            ratio = (float) outWidth / (float) outHeight;
            finalHeight = source.getHeight();
            finalWidth = (int) ((float) source.getHeight() * ratio);//固定原图高度,求最终宽度
            if (finalWidth > source.getWidth()) { //求出的最终宽度 > 原图宽度,求高宽比
                ratio = (float) outHeight / (float) outWidth;
                finalWidth = source.getWidth();
                finalHeight = (int) ((float) source.getWidth() * ratio);
            }
        } else { //输出宽度=输出高度
            finalHeight = source.getHeight();
            finalWidth = finalHeight;
        }

        //修正圆角
        this.radius *= (float) finalHeight / (float) outHeight;
        Bitmap outBitmap = this.mBitmapPool.get(finalWidth, finalHeight, Bitmap.Config.ARGB_8888);
        if (outBitmap == null) {
            outBitmap = Bitmap.createBitmap(finalWidth, finalHeight, Bitmap.Config.ARGB_8888);
        }

        Canvas canvas = new Canvas(outBitmap);
        Paint paint = new Paint();
        //关联画笔绘制的原图bitmap
        BitmapShader shader = new BitmapShader(source, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        //计算中心位置,进行偏移
        int width = (source.getWidth() - finalWidth) / 2;
        int height = (source.getHeight() - finalHeight) / 2;
        if (width != 0 || height != 0) {
            Matrix matrix = new Matrix();
            matrix.setTranslate((float) (-width), (float) (-height));
            shader.setLocalMatrix(matrix);
        }

        paint.setShader(shader);
        paint.setAntiAlias(true);
        RectF rectF = new RectF(0.0F, 0.0F, (float) canvas.getWidth(), (float) canvas.getHeight());
        canvas.drawRoundRect(rectF, this.radius, this.radius, paint); //先绘制圆角矩形

        if (exceptLeftTop) { //左上角不为圆角
            canvas.drawRect(0, 0, radius, radius, paint);
        }
        if (exceptRightTop) {//右上角不为圆角
            canvas.drawRect(canvas.getWidth() - radius, 0, radius, radius, paint);
        }

        if (exceptLeftBottom) {//左下角不为圆角
            canvas.drawRect(0, canvas.getHeight() - radius, radius, canvas.getHeight(), paint);
        }

        if (exceptRightBotoom) {//右下角不为圆角
            canvas.drawRect(canvas.getWidth() - radius, canvas.getHeight() - radius, canvas.getWidth(), canvas.getHeight(), paint);
        }

        return BitmapResource.obtain(outBitmap, this.mBitmapPool);
    }

    @Override
    public void updateDiskCacheKey(@NonNull MessageDigest messageDigest) {

    }
}
