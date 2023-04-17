package org.apache.ftpserver.listener.nio;


import com.termux.zerocore.ftp.new_ftp.services.FtpService;

import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.apache.mina.filter.codec.textline.TextLineDecoder;

import java.nio.charset.Charset;

public class FtpServerProtocolCodecFactory implements ProtocolCodecFactory {
    private final ProtocolDecoder decoder;
    private final ProtocolEncoder encoder = new FtpResponseEncoder();

    public FtpServerProtocolCodecFactory() {
        String charset = FtpService.getCharsetFromSharedPreferences();
        decoder = new TextLineDecoder(Charset.forName(charset));
        //Log.d(getClass().getName(),"the charset is "+charset);
    }

    public ProtocolDecoder getDecoder(IoSession session) throws Exception {
        return this.decoder;
    }

    public ProtocolEncoder getEncoder(IoSession session) throws Exception {
        return this.encoder;
    }
}
