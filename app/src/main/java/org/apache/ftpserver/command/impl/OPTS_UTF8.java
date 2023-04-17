package org.apache.ftpserver.command.impl;



import com.termux.zerocore.ftp.new_ftp.Constants;
import com.termux.zerocore.ftp.new_ftp.services.FtpService;

import org.apache.ftpserver.command.AbstractCommand;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.FtpRequest;
import org.apache.ftpserver.impl.FtpIoSession;
import org.apache.ftpserver.impl.FtpServerContext;
import org.apache.ftpserver.impl.LocalizedFtpReply;

import java.io.IOException;

public class OPTS_UTF8 extends AbstractCommand {
    public OPTS_UTF8() {
    }

    public void execute(FtpIoSession session, FtpServerContext context, FtpRequest request) throws IOException, FtpException {
        session.resetState();
        session.write(LocalizedFtpReply.translate(session, request, context,
                FtpService.getCharsetFromSharedPreferences().equals(Constants.PreferenceConsts.CHARSET_TYPE_DEFAULT) ? 200 : 502
                , "OPTS.UTF8", (String) null));
    }
}
