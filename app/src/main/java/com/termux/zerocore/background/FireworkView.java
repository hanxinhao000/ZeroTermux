package com.termux.zerocore.background;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static android.opengl.GLES10.glBlendFunc;
import static android.opengl.GLES20.GL_BLEND;
import static android.opengl.GLES20.GL_ONE;
import static android.opengl.GLES20.GL_SRC_ALPHA;
import static android.opengl.GLES20.glDepthMask;
import static android.opengl.GLES20.glEnable;
import com.termux.R;


/**
 * Created by qinfeng on 2016/9/27.
 */

public class FireworkView extends GLSurfaceView{
    private Context mContext;
    private Bitmap mBackground;
    private int mPlistFileResourceId;
    private int mTextureResourceId;

    public TextureLayer getTextureLayer() {
        return mTextureLayer;
    }

    public void setTextureLayer(TextureLayer textureLayer) {
        mTextureLayer = textureLayer;
    }

    public ParticleLayer getParticleSystem() {
        return mParticleSystem;
    }

    public void setParticleSystem(ParticleLayer particleSystem) {
        mParticleSystem = particleSystem;
    }

    private TextureLayer mTextureLayer;
    private ParticleLayer mParticleSystem;

    public FireworkView(Context context) {
        super(context);
        init(context,null);
    }

    public FireworkView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context,attrs);
    }

    private void handleTypedArray(Context context, AttributeSet attrs) {
        if (attrs == null) {
            return;
        }
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.FireworkView);
        mTextureResourceId = typedArray.getResourceId(R.styleable.FireworkView_texture,0);
        mPlistFileResourceId = typedArray.getResourceId(R.styleable.FireworkView_plistFile,0);
        int backgroundResourceId = typedArray.getResourceId(R.styleable.FireworkView_backgroundImage,0);
        mBackground = BitmapFactory.decodeResource(context.getResources(), backgroundResourceId);
    }
    private void init(Context context, AttributeSet attrs) {
        handleTypedArray(context,attrs);

        mContext = context;
        setEGLContextClientVersion(2);
        FireworkRenderer fireworkRenderer = new FireworkRenderer();
        setRenderer(fireworkRenderer);
    }

    public class FireworkRenderer implements Renderer {




        @Override
        public void onSurfaceCreated(GL10 unused, EGLConfig config) {
            GLES20.glClearColor ( 0.0f, 0.0f, 0.0f, 0.0f );
            mParticleSystem = new ParticleLayer(mContext, mPlistFileResourceId,mTextureResourceId);
            if (mBackground != null)
                mTextureLayer = new TextureLayer(mContext, mBackground);

        }

        @Override
        public void onDrawFrame(GL10 unused) {
            // Set the view-port
            GLES20.glViewport ( 0, 0,getWidth(),getHeight() );
            // Clear the color buffer
            GLES20.glClear ( GLES30.GL_COLOR_BUFFER_BIT );
            if (mBackground != null)
                mTextureLayer.draw();

            glDepthMask(false);
            glEnable(GL_BLEND);

            //TODO particle system support BlendFuc
            glBlendFunc(GL_SRC_ALPHA, GL_ONE);

            mParticleSystem.update(1f / 60f);
            mParticleSystem.updateBuffer();
            mParticleSystem.draw();


        }

        @Override
        public void onSurfaceChanged(GL10 unused, int width, int height) {
            // Adjust the viewport based on geometry changes,
            // such as screen rotation
            GLES20.glViewport(0, 0, width, height);

        }

    }
}
