package com.termux.zerocore.gui

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/** OpenGL ES 2.0 纹理渲染，由 GPU 负责缩放合成。 */
internal class ZtGuiGlRenderer : GLSurfaceView.Renderer {

    @Volatile
    private var rgb: ByteArray? = null
    @Volatile
    private var frameWidth = 1
    @Volatile
    private var frameHeight = 1
    @Volatile
    private var uploadNeeded = false

    @Volatile
    var userScale = 1f
    @Volatile
    var userTranslateX = 0f
    @Volatile
    var userTranslateY = 0f

    private var program = 0
    private var textureId = 0
    private var viewWidth = 1
    private var viewHeight = 1
    private var uMatrix = 0
    private var uTexture = 0

    private val quad: FloatBuffer = floatBuffer(
        floatArrayOf(
            -1f, -1f, 0f, 1f,
            1f, -1f, 1f, 1f,
            -1f, 1f, 0f, 0f,
            1f, 1f, 1f, 0f
        )
    )

    fun setFrame(width: Int, height: Int, rgbData: ByteArray) {
        frameWidth = width.coerceAtLeast(1)
        frameHeight = height.coerceAtLeast(1)
        rgb = rgbData
        uploadNeeded = true
    }

    fun resetTransform() {
        userScale = 1f
        userTranslateX = 0f
        userTranslateY = 0f
    }

    override fun onSurfaceCreated(unused: GL10?, config: EGLConfig?) {
        program = buildProgram(VERTEX_SHADER, FRAGMENT_SHADER)
        uMatrix = GLES20.glGetUniformLocation(program, "uMatrix")
        uTexture = GLES20.glGetUniformLocation(program, "uTexture")
        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        textureId = textures[0]
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
    }

    override fun onSurfaceChanged(unused: GL10?, width: Int, height: Int) {
        viewWidth = width.coerceAtLeast(1)
        viewHeight = height.coerceAtLeast(1)
        GLES20.glViewport(0, 0, viewWidth, viewHeight)
    }

    override fun onDrawFrame(unused: GL10?) {
        GLES20.glClearColor(0.08f, 0.08f, 0.08f, 1f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        val data = rgb ?: return
        if (uploadNeeded) {
            GLES20.glPixelStorei(GLES20.GL_UNPACK_ALIGNMENT, 1)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
            val buffer = ByteBuffer.wrap(data)
            GLES20.glTexImage2D(
                GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGB,
                frameWidth, frameHeight, 0,
                GLES20.GL_RGB, GLES20.GL_UNSIGNED_BYTE, buffer
            )
            uploadNeeded = false
        }
        GLES20.glUseProgram(program)
        GLES20.glUniformMatrix4fv(uMatrix, 1, false, computeMatrix(), 0)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glUniform1i(uTexture, 0)
        val pos = GLES20.glGetAttribLocation(program, "aPosition")
        val tex = GLES20.glGetAttribLocation(program, "aTexCoord")
        GLES20.glEnableVertexAttribArray(pos)
        GLES20.glEnableVertexAttribArray(tex)
        quad.position(0)
        GLES20.glVertexAttribPointer(pos, 2, GLES20.GL_FLOAT, false, 16, quad)
        quad.position(2)
        GLES20.glVertexAttribPointer(tex, 2, GLES20.GL_FLOAT, false, 16, quad)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        GLES20.glDisableVertexAttribArray(pos)
        GLES20.glDisableVertexAttribArray(tex)
    }

    private fun computeMatrix(): FloatArray {
        val fit = minOf(
            viewWidth.toFloat() / frameWidth,
            viewHeight.toFloat() / frameHeight
        ) * userScale
        val quadW = frameWidth * fit
        val quadH = frameHeight * fit
        val sx = quadW / viewWidth
        val sy = quadH / viewHeight
        val tx = (userTranslateX * 2f) / viewWidth
        val ty = (-userTranslateY * 2f) / viewHeight
        return floatArrayOf(
            sx, 0f, 0f, 0f,
            0f, sy, 0f, 0f,
            0f, 0f, 1f, 0f,
            tx, ty, 0f, 1f
        )
    }

    private fun buildProgram(vs: String, fs: String): Int {
        val vert = compile(GLES20.GL_VERTEX_SHADER, vs)
        val frag = compile(GLES20.GL_FRAGMENT_SHADER, fs)
        val prog = GLES20.glCreateProgram()
        GLES20.glAttachShader(prog, vert)
        GLES20.glAttachShader(prog, frag)
        GLES20.glLinkProgram(prog)
        GLES20.glDeleteShader(vert)
        GLES20.glDeleteShader(frag)
        return prog
    }

    private fun compile(type: Int, source: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, source)
        GLES20.glCompileShader(shader)
        return shader
    }

    private fun floatBuffer(values: FloatArray): FloatBuffer =
        ByteBuffer.allocateDirect(values.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(values)
            .apply { position(0) }

    companion object {
        private const val VERTEX_SHADER = """
            attribute vec2 aPosition;
            attribute vec2 aTexCoord;
            uniform mat4 uMatrix;
            varying vec2 vTexCoord;
            void main() {
                gl_Position = uMatrix * vec4(aPosition, 0.0, 1.0);
                vTexCoord = aTexCoord;
            }
        """
        private const val FRAGMENT_SHADER = """
            precision mediump float;
            varying vec2 vTexCoord;
            uniform sampler2D uTexture;
            void main() {
                gl_FragColor = texture2D(uTexture, vTexCoord);
            }
        """
    }
}
