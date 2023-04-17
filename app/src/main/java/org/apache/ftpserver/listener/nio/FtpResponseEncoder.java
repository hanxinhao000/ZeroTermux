package org.apache.ftpserver.listener.nio;



import com.termux.zerocore.ftp.new_ftp.services.FtpService;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolEncoderAdapter;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;

import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;

public class FtpResponseEncoder extends ProtocolEncoderAdapter {
    private final CharsetEncoder ENCODER = Charset.forName(FtpService.getCharsetFromSharedPreferences()).newEncoder();

    public FtpResponseEncoder() {
        //Log.d(getClass().getName(),"the charset is "+FtpService.getCharsetFromSharedPreferences());
    }

    public void encode(IoSession session, Object message, ProtocolEncoderOutput out) throws Exception {
        String value = message.toString();
        IoBuffer buf = IoBuffer.allocate(value.length()).setAutoExpand(true);
        buf.putString(value, ENCODER);
        //buf.putString(value,Charset.forName(FtpService.getCharsetFromSharedPreferences()).newEncoder());
        buf.flip();
        out.write(buf);
    }
}
