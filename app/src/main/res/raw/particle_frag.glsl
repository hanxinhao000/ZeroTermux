precision mediump float;

uniform sampler2D u_texture;

varying vec4 v_fragmentColor;
varying vec2 v_texCoord;

void main()
{

    gl_FragColor = v_fragmentColor * texture2D(u_texture, v_texCoord) ;
}


