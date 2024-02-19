package com.termux.zerocore.background;

import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLES20;

import com.termux.zerocore.background.utils.ESShader;
import com.termux.zerocore.background.utils.RawResourceReader;
import com.termux.zerocore.background.utils.TextureHelper;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import com.termux.R;

/**
 * Created by qinfeng on 16/8/2.
 */

public class TextureLayer {
    private final float[] vertexsData =
            {
                    -1f,  1f,// Position 0
                    0.0f,  0.0f,     // TexCoord 0
                    -1f, -1f,  // Position 1
                    0.0f,  1.0f,       // TexCoord 1
                    1f,  1f,  // Position 3
                    1.0f,  0.0f ,       // TexCoord 3
                    1f, -1f,  // Position 2
                    1.0f,  1.0f      // TexCoord 2

            };

    private final short[] indicesData =
            {
                    0, 1, 2, 3, 2, 1
            };
    private final FloatBuffer vertexBuffer;
    private final ShortBuffer indiceBuffer;
    private final int mProgrammerHandle;
    private final int textureUniformHandle;
    private final int mPositionHandle;
    private final int mTexCoordHandle;
    private  int mTextureHandle;

    public TextureLayer(Context context, Bitmap bitmap) {
        vertexBuffer = ByteBuffer.allocateDirect(vertexsData.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        vertexBuffer.put(vertexsData);
        vertexBuffer.position(0);

        indiceBuffer = ByteBuffer.allocateDirect ( indicesData.length * 2 )
                .order ( ByteOrder.nativeOrder() ).asShortBuffer();
        indiceBuffer.put (indicesData).position ( 0 );


        mProgrammerHandle = ESShader.loadProgram(RawResourceReader.readTextFileFromRawResource(context, R.raw.simple_vertex_shader), RawResourceReader.readTextFileFromRawResource(context,R.raw.simpe_fragment_shader));
        mPositionHandle = GLES20.glGetAttribLocation(mProgrammerHandle, "a_position");
        mTexCoordHandle = GLES20.glGetAttribLocation(mProgrammerHandle, "a_texCoord");
  ;

        mTextureHandle = TextureHelper.loadTexture(bitmap);


        textureUniformHandle = GLES20.glGetUniformLocation(mProgrammerHandle, "u_texture");


    }

    public  void draw() {

        GLES20.glUseProgram ( mProgrammerHandle );

        // Load the vertex position
        vertexBuffer.position ( 0 );
        GLES20.glVertexAttribPointer ( mPositionHandle, 2, GLES20.GL_FLOAT,
                false,
                4 * 4, vertexBuffer );

        vertexBuffer.position (2);
        GLES20.glVertexAttribPointer (mTexCoordHandle, 2, GLES20.GL_FLOAT,
                false,
                4 * 4,
                vertexBuffer);

        GLES20.glEnableVertexAttribArray (mPositionHandle);
        GLES20.glEnableVertexAttribArray (mTexCoordHandle);



        // Bind the base map
        GLES20.glActiveTexture ( GLES20.GL_TEXTURE0);
        GLES20.glBindTexture ( GLES20.GL_TEXTURE_2D, mTextureHandle);
        // Set the base map sampler to texture unit to 0
        GLES20.glUniform1i (textureUniformHandle, 0);

        GLES20.glDrawElements ( GLES20.GL_TRIANGLES, 6, GLES20.GL_UNSIGNED_SHORT, indiceBuffer);

    }
}
