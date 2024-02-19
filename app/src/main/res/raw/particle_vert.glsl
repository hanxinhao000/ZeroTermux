precision mediump float;

uniform mat4 u_matrix;

attribute vec2 a_position;
attribute vec4 a_color;
attribute vec2 a_texCoord;


varying vec4 v_fragmentColor;
varying vec2 v_texCoord;

void main()
{
    gl_Position = u_matrix * vec4(a_position,0.0,1.0);
    v_fragmentColor = a_color;
    v_texCoord = a_texCoord;
}

