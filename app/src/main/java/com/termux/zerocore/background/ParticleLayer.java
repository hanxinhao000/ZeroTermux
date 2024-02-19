package com.termux.zerocore.background;

import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLES20;


import com.termux.R;
import com.termux.zerocore.background.utils.ESShader;
import com.termux.zerocore.background.utils.RawResourceReader;
import com.termux.zerocore.background.utils.TextureHelper;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.Arrays;

import static android.opengl.GLES20.glUniformMatrix4fv;

/**
 * Created by qinfeng on 16/8/4.
 */

public class ParticleLayer extends ParticleSystem {
    private static final int POSITION_COMPONENT_COUNT = 2;
    private static final int COLOR_COMPONENT_COUNT = 4;
    private static final int COORD_COMPONENT_COUNT = 2;
    private static final int VERTEX_COMPONENT_COUNT =
            POSITION_COMPONENT_COUNT
                    + COLOR_COMPONENT_COUNT
                    + COORD_COMPONENT_COUNT;
    private static final int VERTEX_PER_QUAD = 4;
    private static final int QUAD_COMPONENT_COUNT = VERTEX_PER_QUAD * VERTEX_COMPONENT_COUNT;


    public static final int TL = 0;
    public static final int BL = 1;
    public static final int TR = 2;
    public static final int BR = 3;

    float[] _vertexs;
    short[] _indices;
    int [] _vboIds = {0,0};

    FloatBuffer _vertexsBuffer;
    ShortBuffer _indicesBuffer;
    private int mProgrammerHandle;
    private int mTextureHandle;
    private int textureUniformHandle;
    private int mPositionHandle;
    private int mColorHandle;
    private int mTexCoordHandle;
    private float[] projectionMatrix;
    private int mMatrixUniformHandle;



//    class Vec3 {
//        float x;
//        float y;
//        float z;
//    }
//    struct CC_DLL Color4B
//    {
//
//        GLubyte r;
//        GLubyte g;
//        GLubyte b;
//        GLubyte a;
//
//    };

//    struct CC_DLL Tex2F {
//        float u;
//        float v;
//    };

//    struct CC_DLL V3F_C4B_T2F
//    {
//        /// vertices (3F)
//        Vec3     vertices;            // 12 bytes
//
//        /// colors (4B)
//        Color4B      colors;              // 4 bytes
//
//        // tex coords (2F)
//        Tex2F        texCoords;           // 8 bytes
//    };
//    struct CC_DLL V3F_C4B_T2F_Quad
//    {
//        /// top left
//        V3F_C4B_T2F    tl;
//        /// bottom left
//        V3F_C4B_T2F    bl;
//        /// top right
//        V3F_C4B_T2F    tr;
//        /// bottom right
//        V3F_C4B_T2F    br;
//    };


    public ParticleLayer(Context context, int plistFile, int resourceId ) {
        super(context, plistFile,resourceId);


    }

    ParticleLayer(Context context, int numberOfParticles) {
        super(context, numberOfParticles);
    }


    public void draw(){
        // Use the program object
        GLES20.glUseProgram ( mProgrammerHandle );

//        if ( _vboIds[0] == 0 && _vboIds[1] == 0 ) {
//            // Only allocate on the first draw
//            GLES20.glGenBuffers(2, _vboIds, 0);
//
//            _vertexsBuffer.position(0);
//            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, _vboIds[0]);
//            GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, VERTEX_COMPONENT_COUNT * 4 * _totalParticles,
//                    _vertexsBuffer, GLES20.GL_DYNAMIC_DRAW);
//
//            _indicesBuffer.position(0);
//            GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, _vboIds[1]);
//            GLES20.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER, _indices.length * 2,
//                    _indicesBuffer, GLES20.GL_STATIC_DRAW);
//        }
//        else {
//            _vertexsBuffer.position ( 0 );
//            GLES20.glBindBuffer ( GLES20.GL_ARRAY_BUFFER, _vboIds[0] );
//            GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, 0,VERTEX_COMPONENT_COUNT * 4 *_totalParticles,
//                    _vertexsBuffer);
//        }



//        GLES20.glBindBuffer ( GLES20.GL_ARRAY_BUFFER, _vboIds[0] );
//        GLES20.glBindBuffer ( GLES20.GL_ELEMENT_ARRAY_BUFFER, _vboIds[1] );

        GLES20.glEnableVertexAttribArray ( mPositionHandle );
        GLES20.glEnableVertexAttribArray ( mColorHandle );
        GLES20.glEnableVertexAttribArray ( mTexCoordHandle );

        // Load the vertex position
        int dataOffset = 0;
        _vertexsBuffer.position (dataOffset);
        GLES20.glVertexAttribPointer ( mPositionHandle, POSITION_COMPONENT_COUNT, GLES20.GL_FLOAT,
                false,
                VERTEX_COMPONENT_COUNT * 4, _vertexsBuffer);//dataOffset * 4

        // Load the color
        dataOffset += POSITION_COMPONENT_COUNT;
        _vertexsBuffer.position (dataOffset);
        GLES20.glVertexAttribPointer ( mColorHandle, COLOR_COMPONENT_COUNT, GLES20.GL_FLOAT,
                false,
                VERTEX_COMPONENT_COUNT * 4,
                _vertexsBuffer);
        // Load the texture coordinate
        dataOffset +=  COLOR_COMPONENT_COUNT;
        _vertexsBuffer.position (dataOffset);
        GLES20.glVertexAttribPointer ( mTexCoordHandle, COORD_COMPONENT_COUNT, GLES20.GL_FLOAT,
                false,
                VERTEX_COMPONENT_COUNT * 4,
                _vertexsBuffer );


        glUniformMatrix4fv(mMatrixUniformHandle, 1, false, projectionMatrix,0);

        // Bind the base map
        GLES20.glActiveTexture ( GLES20.GL_TEXTURE0 );
        GLES20.glBindTexture ( GLES20.GL_TEXTURE_2D, mTextureHandle);

        // Set the base map sampler to texture unit to 0
        GLES20.glUniform1i ( textureUniformHandle, 0 );

        GLES20.glDrawElements ( GLES20.GL_TRIANGLES, _indices.length, GLES20.GL_UNSIGNED_SHORT, _indicesBuffer);
        GLES20.glDisableVertexAttribArray ( mPositionHandle );
        GLES20.glDisableVertexAttribArray ( mColorHandle );
        GLES20.glDisableVertexAttribArray ( mTexCoordHandle );

//        GLES20.glBindBuffer ( GLES20.GL_ARRAY_BUFFER, 0 );
//        GLES20.glBindBuffer ( GLES20.GL_ELEMENT_ARRAY_BUFFER, 0 );

    }



    @Override
    boolean initWithTotalParticles(int maxParticles) {
        // base initialization
        if(super.initWithTotalParticles(maxParticles) ) {
//            _vertexs =new float[]
//                    {
//                            -1f,  1f,// Position 0
//                            1.0f,0.0f,0.0f,1.0f,
//                            0.0f,  0.0f,     // TexCoord 0
//                            -1f, -1f,  // Position 1
//                            1.0f,0.0f,0.0f,1.0f,
//                            0.0f,  1.0f,       // TexCoord 1
//                            1f,  1f,  // Position 3
//                            1.0f,0.0f,0.0f,1.0f,
//                            1.0f,  0.0f ,       // TexCoord 3
//                            1f, -1f,  // Position 2
//                            1.0f,0.0f,0.0f,1.0f,
//                            1.0f,  1.0f      // TexCoord 2
//
//                    };

//            _indices = new short[]
//                    {
//                            0, 1, 2, 3, 2, 1
//                    };
            _vertexs = new float[_totalParticles * QUAD_COMPONENT_COUNT];
            _vertexsBuffer = ByteBuffer.allocateDirect(_vertexs.length * 4).order(ByteOrder.nativeOrder())
                    .asFloatBuffer()
                    .put(_vertexs);
            _vertexsBuffer.position(0);


            _indices = new short[_totalParticles * 6];
            initIndices();
            _indicesBuffer = ByteBuffer.allocateDirect(_indices.length * 2).order(ByteOrder.nativeOrder())
                    .asShortBuffer()
                    .put(_indices);
            _indicesBuffer.position(0);





            projectionMatrix = new float[16];

            createOrthographicOffCenter(0f,(float) _width,(float) _height,0f,-1024,1024,projectionMatrix);

//            MatrixHelper.perspectiveM(projectionMatrix, 45, (float) width
//                    / (float) height, 1f, 10f);

            mProgrammerHandle = ESShader.loadProgram(RawResourceReader.readTextFileFromRawResource(context, R.raw.particle_vert), RawResourceReader.readTextFileFromRawResource(context,R.raw.particle_frag));

            mPositionHandle = GLES20.glGetAttribLocation(mProgrammerHandle, "a_position");
            mColorHandle = GLES20.glGetAttribLocation(mProgrammerHandle, "a_color");
            mTexCoordHandle = GLES20.glGetAttribLocation(mProgrammerHandle, "a_texCoord");

            mMatrixUniformHandle = GLES20.glGetUniformLocation(mProgrammerHandle, "u_matrix");

            textureUniformHandle = GLES20.glGetUniformLocation(mProgrammerHandle, "u_texture");


            return true;
        }
        return false;
    }

    @Override
    void setTexture(int resourceId) {
        mTextureHandle = TextureHelper.loadTexture(context,resourceId);
        initTexCoord();

    }
    @Override
    void setTexture(Bitmap texture) {
        mTextureHandle = TextureHelper.loadTexture(texture);
        initTexCoord();

    }

    @Override
    void updateQuadWithParticle(Particle particle, Vec2 newPosition) {

        updatePos(particle, newPosition);
        updateColor(particle);
//        initTexCoord();

    }

    @Override
    void clearData() {
        Arrays.fill(_vertexs,0);
        initTexCoord();
    }

    @Override
    public void updateBuffer() {
        //udpate _vertexsBuffer
        _vertexsBuffer.position(0);
        _vertexsBuffer.put(_vertexs);
        _vertexsBuffer.position(0);

        _indicesBuffer.position(0);
        _indicesBuffer.put(_indices);
        _indicesBuffer.position(0);
    }
    private void createOrthographicOffCenter(float left, float right, float bottom, float top,
                                           float zNearPlane, float zFarPlane, float[] dst) {
        dst[0] = 2 / (right - left);
        dst[5] = 2 / (top - bottom);
        dst[10] = 2 / (zNearPlane - zFarPlane);

        dst[12] = (left + right) / (left - right);
        dst[13] = (top + bottom) / (bottom - top);
        dst[14] = (zNearPlane + zFarPlane) / (zNearPlane - zFarPlane);
        dst[15] = 1;
    }

    private void updatePos(Particle particle, Vec2 newPosition) {
        // vertices

        float size_2 = particle.size/2;
        if (particle.rotation == 0f) {
            float x1 = -size_2;
            float y1 = -size_2;

            float x2 = size_2;
            float y2 = size_2;
            float x = newPosition.x;
            float y = newPosition.y;

            float r = -CC_DEGREES_TO_RADIANS(particle.rotation);
            float cr =(float) Math.cos(r);
            float sr =(float) Math.sin(r);
            float ax = x1 * cr - y1 * sr + x;
            float ay = x1 * sr + y1 * cr + y;
            float bx = x2 * cr - y1 * sr + x;
            float by = x2 * sr + y1 * cr + y;
            float cx = x2 * cr - y2 * sr + x;
            float cy = x2 * sr + y2 * cr + y;
            float dx = x1 * cr - y2 * sr + x;
            float dy = x1 * sr + y2 * cr + y;


            ax *=_density;
            ay *= _density;
            bx *= _density;
            by *= _density;
            cx *= _density;
            cy *= _density;
            dx *= _density;
            dy *= _density;


            // bottom-left
            _vertexs[_particleIdx * QUAD_COMPONENT_COUNT
                    + (BL % VERTEX_PER_QUAD) * VERTEX_COMPONENT_COUNT
                    + 0 % POSITION_COMPONENT_COUNT] = ax;
            _vertexs[_particleIdx * QUAD_COMPONENT_COUNT
                    + (BL % VERTEX_PER_QUAD) * VERTEX_COMPONENT_COUNT
                    + 1 % POSITION_COMPONENT_COUNT] = ay;
            // bottom-right vertex:
            _vertexs[_particleIdx * QUAD_COMPONENT_COUNT
                    +(BR % VERTEX_PER_QUAD) * VERTEX_COMPONENT_COUNT
                    + 0 % POSITION_COMPONENT_COUNT] = bx;
            _vertexs[_particleIdx * QUAD_COMPONENT_COUNT
                    +(BR % VERTEX_PER_QUAD) * VERTEX_COMPONENT_COUNT
                    + 1 % POSITION_COMPONENT_COUNT] = by;
            // top-left vertex:
            _vertexs[_particleIdx * QUAD_COMPONENT_COUNT
                    + (TL % VERTEX_PER_QUAD) * VERTEX_COMPONENT_COUNT
                    + 0 % POSITION_COMPONENT_COUNT] = dx;
            _vertexs[_particleIdx * QUAD_COMPONENT_COUNT
                    + (TL % VERTEX_PER_QUAD) * VERTEX_COMPONENT_COUNT
                    + 1 % POSITION_COMPONENT_COUNT] = dy;

            // top-right vertex:
            _vertexs[_particleIdx * QUAD_COMPONENT_COUNT
                    + (TR % VERTEX_PER_QUAD) * VERTEX_COMPONENT_COUNT
                    + 0 % POSITION_COMPONENT_COUNT] = cx;
            _vertexs[_particleIdx * QUAD_COMPONENT_COUNT
                    + (TR % VERTEX_PER_QUAD) * VERTEX_COMPONENT_COUNT
                    + 1 % POSITION_COMPONENT_COUNT] = cy;
        } else {

            // bottom-left
            _vertexs[_particleIdx * QUAD_COMPONENT_COUNT
                    + (BL % VERTEX_PER_QUAD) * VERTEX_COMPONENT_COUNT
                    + 0 % POSITION_COMPONENT_COUNT] = (newPosition.x - size_2) * _density;
            _vertexs[_particleIdx * QUAD_COMPONENT_COUNT
                    + (BL % VERTEX_PER_QUAD) * VERTEX_COMPONENT_COUNT
                    + 1 % POSITION_COMPONENT_COUNT] = (newPosition.y - size_2) * _density;
            // bottom-right vertex:
            _vertexs[_particleIdx * QUAD_COMPONENT_COUNT
                    + (BR % VERTEX_PER_QUAD) * VERTEX_COMPONENT_COUNT
                    + 0 % POSITION_COMPONENT_COUNT] = (newPosition.x + size_2) * _density;
            _vertexs[_particleIdx * QUAD_COMPONENT_COUNT
                    + (BR % VERTEX_PER_QUAD) * VERTEX_COMPONENT_COUNT
                    + 1 % POSITION_COMPONENT_COUNT] = (newPosition.y - size_2) * _density;
            // top-left vertex:
            _vertexs[_particleIdx * QUAD_COMPONENT_COUNT
                    + (TL % VERTEX_PER_QUAD) * VERTEX_COMPONENT_COUNT
                    + 0 % POSITION_COMPONENT_COUNT] = (newPosition.x - size_2) * _density;
            _vertexs[_particleIdx * QUAD_COMPONENT_COUNT
                    + (TL % VERTEX_PER_QUAD) * VERTEX_COMPONENT_COUNT
                    + 1 % POSITION_COMPONENT_COUNT] = (newPosition.y + size_2) * _density;

            // top-right vertex:
            _vertexs[_particleIdx * QUAD_COMPONENT_COUNT
                    + (TR % VERTEX_PER_QUAD) * VERTEX_COMPONENT_COUNT
                    + 0 % POSITION_COMPONENT_COUNT] = (newPosition.x + size_2) * _density;
            _vertexs[_particleIdx * QUAD_COMPONENT_COUNT
                    + (TR % VERTEX_PER_QUAD) * VERTEX_COMPONENT_COUNT
                    + 1 % POSITION_COMPONENT_COUNT] = (newPosition.y + size_2) * _density;
        }
    }

    private void updateColor(Particle particle) {
        // bottom-left vertex:
        _vertexs[_particleIdx * QUAD_COMPONENT_COUNT
                + (BL % VERTEX_PER_QUAD) * VERTEX_COMPONENT_COUNT
                + POSITION_COMPONENT_COUNT
                + 0 % COLOR_COMPONENT_COUNT] = particle.color.r;
        _vertexs[_particleIdx * QUAD_COMPONENT_COUNT
                + (BL % VERTEX_PER_QUAD) * VERTEX_COMPONENT_COUNT
                + POSITION_COMPONENT_COUNT
                + 1 % COLOR_COMPONENT_COUNT] = particle.color.g;
        _vertexs[_particleIdx * QUAD_COMPONENT_COUNT
                + (BL % VERTEX_PER_QUAD) * VERTEX_COMPONENT_COUNT
                + POSITION_COMPONENT_COUNT
                + 2 % COLOR_COMPONENT_COUNT] = particle.color.b;
        _vertexs[_particleIdx * QUAD_COMPONENT_COUNT
                + (BL % VERTEX_PER_QUAD) * VERTEX_COMPONENT_COUNT
                + POSITION_COMPONENT_COUNT
                + 3 % COLOR_COMPONENT_COUNT] = particle.color.a;
        // bottom-right vertex:
        _vertexs[_particleIdx * QUAD_COMPONENT_COUNT
                + (BR % VERTEX_PER_QUAD) * VERTEX_COMPONENT_COUNT
                + POSITION_COMPONENT_COUNT
                + 0 % COLOR_COMPONENT_COUNT] = particle.color.r;
        _vertexs[_particleIdx * QUAD_COMPONENT_COUNT
                + (BR % VERTEX_PER_QUAD ) * VERTEX_COMPONENT_COUNT
                + POSITION_COMPONENT_COUNT
                + 1 % COLOR_COMPONENT_COUNT] = particle.color.g;
        _vertexs[_particleIdx * QUAD_COMPONENT_COUNT
                + (BR % VERTEX_PER_QUAD ) * VERTEX_COMPONENT_COUNT
                + POSITION_COMPONENT_COUNT
                + 2 % COLOR_COMPONENT_COUNT] = particle.color.b;
        _vertexs[_particleIdx * QUAD_COMPONENT_COUNT
                + (BR % VERTEX_PER_QUAD ) * VERTEX_COMPONENT_COUNT
                + POSITION_COMPONENT_COUNT
                + 3 % COLOR_COMPONENT_COUNT] = particle.color.a;
        // top-left vertex:
        _vertexs[_particleIdx * QUAD_COMPONENT_COUNT
                + (TL % VERTEX_PER_QUAD ) * VERTEX_COMPONENT_COUNT
                + POSITION_COMPONENT_COUNT
                + 0 % COLOR_COMPONENT_COUNT] = particle.color.r;
        _vertexs[_particleIdx * QUAD_COMPONENT_COUNT
                + (TL % VERTEX_PER_QUAD) * VERTEX_COMPONENT_COUNT
                + POSITION_COMPONENT_COUNT
                + 1 % COLOR_COMPONENT_COUNT] = particle.color.g;
        _vertexs[_particleIdx * QUAD_COMPONENT_COUNT
                + (TL % VERTEX_PER_QUAD) * VERTEX_COMPONENT_COUNT
                + POSITION_COMPONENT_COUNT
                + 2 % COLOR_COMPONENT_COUNT] = particle.color.b;
        _vertexs[_particleIdx * QUAD_COMPONENT_COUNT
                + (TL % VERTEX_PER_QUAD ) * VERTEX_COMPONENT_COUNT
                + POSITION_COMPONENT_COUNT
                + 3 % COLOR_COMPONENT_COUNT] = particle.color.a;
        // top-right vertex:
        _vertexs[_particleIdx * QUAD_COMPONENT_COUNT
                + (TR % VERTEX_PER_QUAD ) * VERTEX_COMPONENT_COUNT
                + POSITION_COMPONENT_COUNT
                + 0 % COLOR_COMPONENT_COUNT] = particle.color.r;
        _vertexs[_particleIdx * QUAD_COMPONENT_COUNT
                + (TR % VERTEX_PER_QUAD) * VERTEX_COMPONENT_COUNT
                + POSITION_COMPONENT_COUNT
                + 1 % COLOR_COMPONENT_COUNT] = particle.color.g;
        _vertexs[_particleIdx * QUAD_COMPONENT_COUNT
                + (TR % VERTEX_PER_QUAD ) * VERTEX_COMPONENT_COUNT
                + POSITION_COMPONENT_COUNT
                + 2 % COLOR_COMPONENT_COUNT] = particle.color.b;
        _vertexs[_particleIdx * QUAD_COMPONENT_COUNT
                + (TR % VERTEX_PER_QUAD) * VERTEX_COMPONENT_COUNT
                + POSITION_COMPONENT_COUNT
                + 3 % COLOR_COMPONENT_COUNT] = particle.color.a;
    }

    private void initTexCoord() {
        for(int i=0; i<_totalParticles; i++)
        {
            // bottom-left vertex:
            _vertexs[i * QUAD_COMPONENT_COUNT
                    + (BL % VERTEX_PER_QUAD ) * VERTEX_COMPONENT_COUNT+ POSITION_COMPONENT_COUNT + COLOR_COMPONENT_COUNT
                    + 0 % COORD_COMPONENT_COUNT] = 0.0f;
            _vertexs[i * QUAD_COMPONENT_COUNT
                    + (BL % VERTEX_PER_QUAD ) * VERTEX_COMPONENT_COUNT+ POSITION_COMPONENT_COUNT + COLOR_COMPONENT_COUNT
                    + 1 % COORD_COMPONENT_COUNT] = 1.0f;
            // bottom-right vertex:
            _vertexs[i * QUAD_COMPONENT_COUNT
                    + (BR % VERTEX_PER_QUAD ) * VERTEX_COMPONENT_COUNT+ POSITION_COMPONENT_COUNT + COLOR_COMPONENT_COUNT
                    + 0 % COORD_COMPONENT_COUNT] = 1.0f;
            _vertexs[i * QUAD_COMPONENT_COUNT
                    + (BR % VERTEX_PER_QUAD ) * VERTEX_COMPONENT_COUNT+ POSITION_COMPONENT_COUNT + COLOR_COMPONENT_COUNT
                    + 1 % COORD_COMPONENT_COUNT] = 1.0f;
            // top-left vertex:
            _vertexs[i * QUAD_COMPONENT_COUNT
                    + (TL % VERTEX_PER_QUAD ) * VERTEX_COMPONENT_COUNT+ POSITION_COMPONENT_COUNT + COLOR_COMPONENT_COUNT
                    + 0 % COORD_COMPONENT_COUNT] = 0.0f;
            _vertexs[i * QUAD_COMPONENT_COUNT
                    + (TL % VERTEX_PER_QUAD ) * VERTEX_COMPONENT_COUNT+ POSITION_COMPONENT_COUNT + COLOR_COMPONENT_COUNT
                    + 1 % COORD_COMPONENT_COUNT] = 0.0f;
            // top-right vertex:
            _vertexs[i * QUAD_COMPONENT_COUNT
                    + (TR % VERTEX_PER_QUAD ) * VERTEX_COMPONENT_COUNT+ POSITION_COMPONENT_COUNT + COLOR_COMPONENT_COUNT
                    + 0 % COORD_COMPONENT_COUNT] = 1.0f;
            _vertexs[i * QUAD_COMPONENT_COUNT
                    + (TR % VERTEX_PER_QUAD ) * VERTEX_COMPONENT_COUNT+ POSITION_COMPONENT_COUNT + COLOR_COMPONENT_COUNT
                    + 1 % COORD_COMPONENT_COUNT] = 0.0f;
        }
    }

    //    @Override
//    public void setTotalParticles(int tp) {
//        super.setTotalParticles(tp);
//        // If we are setting the total number of particles to a number higher
//        // than what is allocated, we need to allocate new arrays
//        if( tp > _allocatedParticles ) {
//            // Allocate new memory
//            size_t particlesSize = tp * sizeof(tParticle);
//            size_t quadsSize = sizeof(_quads[0]) * tp * 1;
//            size_t indicesSize = sizeof(_indices[0]) * tp * 6 * 1;
//
//            tParticle* particlesNew = (tParticle*)realloc(_particles, particlesSize);
//            V3F_C4B_T2F_Quad* quadsNew = (V3F_C4B_T2F_Quad*)realloc(_quads, quadsSize);
//            GLushort* indicesNew = (GLushort*)realloc(_indices, indicesSize);
//
//            if (particlesNew && quadsNew && indicesNew)
//            {
//                // Assign pointers
//                _particles = particlesNew;
//                _quads = quadsNew;
//                _indices = indicesNew;
//
//                // Clear the memory
//                memset(_particles, 0, particlesSize);
//                memset(_quads, 0, quadsSize);
//                memset(_indices, 0, indicesSize);
//
//                _allocatedParticles = tp;
//            }
//            else
//            {
//                // Out of memory, failed to resize some array
//                if (particlesNew) _particles = particlesNew;
//                if (quadsNew) _quads = quadsNew;
//                if (indicesNew) _indices = indicesNew;
//
//                CCLOG("Particle system: out of memory");
//                return;
//            }
//
//            _totalParticles = tp;
//
//            // Init particles
//            if (_batchNode)
//            {
//                for (int i = 0; i < _totalParticles; i++)
//                {
//                    _particles[i].atlasIndex=i;
//                }
//            }
//
//            initIndices();
//            if (Configuration::getInstance().supportsShareableVAO())
//            {
//                setupVBOandVAO();
//            }
//            else
//            {
//                setupVBO();
//            }
//
//            // fixed http://www.cocos2d-x.org/issues/3990
//            // Updates texture coords.
//            updateTexCoords();
//        } else {
//            _totalParticles = tp;
//        }
//
//        // fixed issue #5762
//        // reset the emission rate
//        setEmissionRate(_totalParticles / _life);
//
//        resetSystem();
//    }


    private void initIndices()
    {
        for(short i = 0; i < _totalParticles; ++i)
        {
            short i6 = (short) (i*6);
            short i4 = (short) (i*4);
            _indices[i6+0] = (short) (i4+0);
            _indices[i6+1] = (short) (i4+1);
            _indices[i6+2] = (short) (i4+2);

            _indices[i6+5] = (short) (i4+1);
            _indices[i6+4] = (short) (i4+2);
            _indices[i6+3] = (short) (i4+3);
        }
    }
}
