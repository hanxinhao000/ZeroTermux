package org.apache.ftpserver.impl;



import com.termux.zerocore.ftp.new_ftp.services.FtpService;

import org.apache.ftpserver.ftplet.DataConnection;
import org.apache.ftpserver.ftplet.DataType;
import org.apache.ftpserver.ftplet.FtpSession;
import org.apache.ftpserver.usermanager.impl.TransferRateRequest;
import org.apache.ftpserver.util.IoUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

public class IODataConnection implements DataConnection {
    private static final byte[] EOL = System.getProperty("line.separator").getBytes();
    private final Logger LOG = LoggerFactory.getLogger(IODataConnection.class);
    private final FtpIoSession session;
    private final Socket socket;
    private final ServerDataConnectionFactory factory;

    public IODataConnection(Socket socket, FtpIoSession session, ServerDataConnectionFactory factory) {
        this.session = session;
        this.socket = socket;
        this.factory = factory;
    }

    private InputStream getDataInputStream() throws IOException {
        try {
            Socket dataSoc = this.socket;
            if (dataSoc == null) {
                throw new IOException("Cannot open data connection.");
            } else {
                InputStream is = dataSoc.getInputStream();
                if (this.factory.isZipMode()) {
                    is = new InflaterInputStream((InputStream) is);
                }

                return (InputStream) is;
            }
        } catch (IOException var3) {
            this.factory.closeDataConnection();
            throw var3;
        }
    }

    private OutputStream getDataOutputStream() throws IOException {
        try {
            Socket dataSoc = this.socket;
            if (dataSoc == null) {
                throw new IOException("Cannot open data connection.");
            } else {
                OutputStream os = dataSoc.getOutputStream();
                if (this.factory.isZipMode()) {
                    os = new DeflaterOutputStream((OutputStream) os);
                }

                return (OutputStream) os;
            }
        } catch (IOException var3) {
            this.factory.closeDataConnection();
            throw var3;
        }
    }

    public final long transferFromClient(FtpSession session, OutputStream out) throws IOException {
        TransferRateRequest transferRateRequest = new TransferRateRequest();
        transferRateRequest = (TransferRateRequest) session.getUser().authorize(transferRateRequest);
        int maxRate = 0;
        if (transferRateRequest != null) {
            maxRate = transferRateRequest.getMaxUploadRate();
        }

        InputStream is = this.getDataInputStream();

        long var6;
        try {
            var6 = this.transfer(session, false, is, out, maxRate);
        } finally {
            IoUtils.close(is);
        }

        return var6;
    }

    public final long transferToClient(FtpSession session, InputStream in) throws IOException {
        TransferRateRequest transferRateRequest = new TransferRateRequest();
        transferRateRequest = (TransferRateRequest) session.getUser().authorize(transferRateRequest);
        int maxRate = 0;
        if (transferRateRequest != null) {
            maxRate = transferRateRequest.getMaxDownloadRate();
        }

        OutputStream out = this.getDataOutputStream();

        long var6;
        try {
            var6 = this.transfer(session, true, in, out, maxRate);
        } finally {
            IoUtils.close(out);
        }

        return var6;
    }

    public final void transferToClient(FtpSession session, String str) throws IOException {
        OutputStream out = this.getDataOutputStream();
        OutputStreamWriter writer = null;

        try {
            writer = new OutputStreamWriter(out, FtpService.getCharsetFromSharedPreferences());
            //Log.d(getClass().getName(),"the charset is "+FtpService.getCharsetFromSharedPreferences());
            writer.write(str);
            if (session instanceof DefaultFtpSession) {
                ((DefaultFtpSession) session).increaseWrittenDataBytes(str.getBytes(FtpService.getCharsetFromSharedPreferences()).length);
            }
        } finally {
            if (writer != null) {
                writer.flush();
            }

            IoUtils.close(writer);
        }

    }

    private final long transfer(FtpSession session, boolean isWrite, InputStream in, OutputStream out, int maxRate) throws IOException {
        long transferredSize = 0L;
        boolean isAscii = session.getDataType() == DataType.ASCII;
        long startTime = System.currentTimeMillis();
        byte[] buff = new byte[4096];
        BufferedInputStream bis = null;
        BufferedOutputStream bos = null;

        try {
            bis = IoUtils.getBufferedInputStream(in);
            bos = IoUtils.getBufferedOutputStream(out);
            DefaultFtpSession defaultFtpSession = null;
            if (session instanceof DefaultFtpSession) {
                defaultFtpSession = (DefaultFtpSession) session;
            }

            byte lastByte = 0;

            while (true) {
                while (true) {
                    if (maxRate > 0) {
                        long interval = System.currentTimeMillis() - startTime;
                        if (interval == 0L) {
                            interval = 1L;
                        }

                        long currRate = transferredSize * 1000L / interval;
                        if (currRate > (long) maxRate) {
                            try {
                                Thread.sleep(50L);
                                continue;
                            } catch (InterruptedException var26) {
                                return transferredSize;
                            }
                        }
                    }

                    int count = bis.read(buff);
                    if (count == -1) {
                        return transferredSize;
                    }

                    if (defaultFtpSession != null) {
                        if (isWrite) {
                            defaultFtpSession.increaseWrittenDataBytes(count);
                        } else {
                            defaultFtpSession.increaseReadDataBytes(count);
                        }
                    }

                    if (isAscii) {
                        for (int i = 0; i < count; ++i) {
                            byte b = buff[i];
                            if (isWrite) {
                                if (b == 10 && lastByte != 13) {
                                    bos.write(13);
                                }

                                bos.write(b);
                            } else if (b == 10) {
                                if (lastByte != 13) {
                                    bos.write(EOL);
                                }
                            } else if (b == 13) {
                                bos.write(EOL);
                            } else {
                                bos.write(b);
                            }

                            lastByte = b;
                        }
                    } else {
                        bos.write(buff, 0, count);
                    }

                    transferredSize += (long) count;
                    this.notifyObserver();
                }
            }
        } catch (IOException var27) {
            this.LOG.warn("Exception during data transfer, closing data connection socket", var27);
            this.factory.closeDataConnection();
            throw var27;
        } catch (RuntimeException var28) {
            this.LOG.warn("Exception during data transfer, closing data connection socket", var28);
            this.factory.closeDataConnection();
            throw var28;
        } finally {
            if (bos != null) {
                bos.flush();
            }

        }
    }

    protected void notifyObserver() {
        this.session.updateLastAccessTime();
    }
}
