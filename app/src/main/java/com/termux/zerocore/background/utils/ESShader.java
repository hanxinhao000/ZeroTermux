// The MIT License (MIT)
//
// Copyright (c) 2013 Dan Ginsburg, Budirijanto Purnomo
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.

//
// Book:      OpenGL(R) ES 3.0 Programming Guide, 2nd Edition
// Authors:   Dan Ginsburg, Budirijanto Purnomo, Dave Shreiner, Aaftab Munshi
// ISBN-10:   0-321-93388-5
// ISBN-13:   978-0-321-93388-1
// Publisher: Addison-Wesley Professional
// URLs:      http://www.opengles-book.com
//            http://my.safaribooksonline.com/book/animation-and-3d/9780133440133
//

// ESShader
//
//    Utility functions for loading GLSL ES 3.0 shaders and creating program objects.
//

package com.termux.zerocore.background.utils;

import android.content.Context;
import android.opengl.GLES20;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class ESShader
{
   //
   ///
   /// \brief Read a shader source into a String
   /// \param context Application context
   /// \param fileName Name of shader file
   /// \return A String object containing shader source, otherwise null
   //
   private static String readShader (Context context, String fileName )
   {
      String shaderSource = null;

      if ( fileName == null )
      {
         return shaderSource;
      }

      // Read the shader file from assets
      InputStream is = null;
      byte [] buffer;

      try
      {
         is =  context.getAssets().open ( fileName );

         // Create a buffer that has the same diameter as the InputStream
         buffer = new byte[is.available()];

         // Read the text file as a stream, into the buffer
         is.read ( buffer );

         ByteArrayOutputStream os = new ByteArrayOutputStream();

         // Write this buffer to the output stream
         os.write ( buffer );

         // Close input and output streams
         os.close();
         is.close();

         shaderSource = os.toString();
      }
      catch ( IOException ioe )
      {
         is = null;
      }

      if ( is == null )
      {
         return shaderSource;
      }

      return shaderSource;
   }

   //
   ///
   /// \brief Load a shader, check for compile errors, print error messages to
   /// output log
   /// \param type Type of shader (GL_VERTEX_SHADER or GL_FRAGMENT_SHADER)
   /// \param shaderSrc Shader source string
   /// \return A new shader object on success, 0 on failure
   //
   public static int loadShader ( int type, String shaderSrc )
   {
      int shader;
      int[] compiled = new int[1];

      // Create the shader object
      shader = GLES20.glCreateShader ( type );

      if ( shader == 0 )
      {
         return 0;
      }

      // Load the shader source
      GLES20.glShaderSource ( shader, shaderSrc );

      // Compile the shader
      GLES20.glCompileShader ( shader );

      // Check the compile status
      GLES20.glGetShaderiv ( shader, GLES20.GL_COMPILE_STATUS, compiled, 0 );

      if ( compiled[0] == 0 )
      {
         Log.e ( "ESShader", GLES20.glGetShaderInfoLog ( shader ) );
         GLES20.glDeleteShader ( shader );
         return 0;
      }

      return shader;
   }

   //
   ///
   /// \brief Load a vertex and fragment shader, create a program object, link
   ///    program.
   /// Errors output to log.
   /// \param vertShaderSrc Vertex shader source code
   /// \param fragShaderSrc Fragment shader source code
   /// \return A new program object linked with the vertex/fragment shader
   ///    pair, 0 on failure
   //
   public static int loadProgram (String vertShaderSrc, String fragShaderSrc )
   {
      int vertexShader;
      int fragmentShader;
      int programObject;
      int[] linked = new int[1];

      // Load the vertex/fragment shaders
      vertexShader = loadShader ( GLES20.GL_VERTEX_SHADER, vertShaderSrc );

      if ( vertexShader == 0 )
      {
         return 0;
      }

      fragmentShader = loadShader ( GLES20.GL_FRAGMENT_SHADER, fragShaderSrc );

      if ( fragmentShader == 0 )
      {
         GLES20.glDeleteShader ( vertexShader );
         return 0;
      }

      // Create the program object
      programObject = GLES20.glCreateProgram();

      if ( programObject == 0 )
      {
         return 0;
      }

      GLES20.glAttachShader ( programObject, vertexShader );
      GLES20.glAttachShader ( programObject, fragmentShader );

      // Link the program
      GLES20.glLinkProgram ( programObject );

      // Check the link status
      GLES20.glGetProgramiv ( programObject, GLES20.GL_LINK_STATUS, linked, 0 );

      if ( linked[0] == 0 )
      {
         Log.e ( "ESShader", "Error linking program:" );
         Log.e ( "ESShader", GLES20.glGetProgramInfoLog ( programObject ) );
         GLES20.glDeleteProgram ( programObject );
         return 0;
      }

      // Free up no longer needed shader resources
      GLES20.glDeleteShader ( vertexShader );
      GLES20.glDeleteShader ( fragmentShader );

      return programObject;
   }

   //
   ///
   /// \brief Load a vertex and fragment shader from "assets", create a program object, link
   ///    program.
   /// Errors output to log.
   /// \param vertShaderFileName Vertex shader source file name
   /// \param fragShaderFileName Fragment shader source file name
   /// \return A new program object linked with the vertex/fragment shader
   ///    pair, 0 on failure
   //
   public static int loadProgramFromAsset (Context context, String vertexShaderFileName, String fragShaderFileName )
   {
      int vertexShader;
      int fragmentShader;
      int programObject;
      int[] linked = new int[1];

      String vertShaderSrc = null;
      String fragShaderSrc = null;

      // Read vertex shader from assets
      vertShaderSrc = readShader ( context, vertexShaderFileName );

      if ( vertShaderSrc == null )
      {
         return 0;
      }

      // Read fragment shader from assets
      fragShaderSrc = readShader ( context, fragShaderFileName );

      if ( fragShaderSrc == null )
      {
         return 0;
      }

      // Load the vertex shader
      vertexShader = loadShader ( GLES20.GL_VERTEX_SHADER, vertShaderSrc );

      if ( vertexShader == 0 )
      {
         return 0;
      }

      // Load the fragment shader
      fragmentShader = loadShader ( GLES20.GL_FRAGMENT_SHADER, fragShaderSrc );

      if ( fragmentShader == 0 )
      {
         GLES20.glDeleteShader ( vertexShader );
         return 0;
      }

      // Create the program object
      programObject = GLES20.glCreateProgram();

      if ( programObject == 0 )
      {
         return 0;
      }

      GLES20.glAttachShader ( programObject, vertexShader );
      GLES20.glAttachShader ( programObject, fragmentShader );

      // Link the program
      GLES20.glLinkProgram ( programObject );

      // Check the link status
      GLES20.glGetProgramiv ( programObject, GLES20.GL_LINK_STATUS, linked, 0 );

      if ( linked[0] == 0 )
      {
         Log.e ( "ESShader", "Error linking program:" );
         Log.e ( "ESShader", GLES20.glGetProgramInfoLog ( programObject ) );
         GLES20.glDeleteProgram ( programObject );
         return 0;
      }

      // Free up no longer needed shader resources
      GLES20.glDeleteShader ( vertexShader );
      GLES20.glDeleteShader ( fragmentShader );

      return programObject;
   }
}
