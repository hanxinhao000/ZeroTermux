package com.termux.zerocore.http_service;

import static android.os.Build.VERSION_CODES.R;

import android.os.Environment;
import android.util.Log;

import com.example.xh_lib.utils.UUtils;
import com.termux.R;
import com.termux.shared.termux.TermuxConstants;
import com.termux.zerocore.url.FileUrl;
import com.termux.zerocore.utils.FileIOUtils;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.ProgressListener;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.util.Streams;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

/**
 * Created by wangmingxing on 2017/6/20.
 */

public class HttpServer extends NanoHTTPD {
    private static final String TAG = "HttpServer";
    private NanoFileUpload mFileUpload;
    private OnStatusUpdateListener mStatusUpdateListener;

    interface OnStatusUpdateListener {
        void onUploadingProgressUpdate(int progress);
        void onUploadingFile(File file, boolean done);
        void onDownloadingFile(File file, boolean done);
    }

    class DownloadResponse extends Response {
        private File downloadFile;

        DownloadResponse(File downloadFile, InputStream stream) {
            super(Response.Status.OK, "application/octet-stream", stream, downloadFile.length());
            this.downloadFile = downloadFile;
        }

        @Override
        protected void send(OutputStream outputStream) {
            super.send(outputStream);
            if (mStatusUpdateListener != null) {
                mStatusUpdateListener.onDownloadingFile(downloadFile, true);
            }
        }
    }

    public HttpServer(int port) {
        super(port);
        mFileUpload = new NanoFileUpload(new DiskFileItemFactory());
        mFileUpload.setProgressListener(new ProgressListener() {
            int progress = 0;
            @Override
            public void update(long pBytesRead, long pContentLength, int pItems) {
                //Log.d(TAG, pBytesRead + " bytes has been read, totol " + pContentLength + " bytes");
                if (mStatusUpdateListener != null) {
                    int p = (int) (pBytesRead * 100 / pContentLength);
                    if (p != progress) {
                        progress = p;
                        mStatusUpdateListener.onUploadingProgressUpdate(progress);
                    }
                }
            }
        });
    }

    @Override
    public Response serve(IHTTPSession session) {
        String uri = session.getUri();
        Method method = session.getMethod();
        Map<String, String> header = session.getHeaders();
        Map<String, String> parms = session.getParms();
        String answer = "Success!";
        Log.d(TAG, "uri=" + uri);
        Log.d(TAG, "method=" + method);
        Log.d(TAG, "header=" + header);
        Log.d(TAG, "params=" + parms);

        // for file upload
        if (NanoFileUpload.isMultipartContent(session)) {
            try {
                FileItemIterator iterator = mFileUpload.getItemIterator(session);
                while (iterator.hasNext()) {
                    FileItemStream item = iterator.next();
                    String name = item.getFieldName();
                    InputStream inputStream = item.openStream();
                    if (item.isFormField()) {
                        Log.d(TAG, "Item is form filed, name=" +
                            name + ",value=" + Streams.asString(inputStream));
                    } else {
                        String fileName = item.getName();
                        Log.d(TAG, "Item is file field, name=" + name + ",fileName=" + fileName);

                        File file = new File(Environment.getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_DOWNLOADS), fileName);
                        String path = file.getAbsolutePath();
                        Log.d(TAG, "Save file to " + path);
                        if (mStatusUpdateListener != null) {
                            mStatusUpdateListener.onUploadingFile(file, false);
                        }

                        FileOutputStream fos = new FileOutputStream(file);
                        Streams.copy(inputStream, fos, true);
                        if (mStatusUpdateListener != null) {
                            mStatusUpdateListener.onUploadingFile(file, true);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (method.equals(Method.GET)) {
            // for file browse and download
            File rootFile = new File(FileUrl.INSTANCE.getMainFilesUrl());
            uri = uri.replace(rootFile.getAbsolutePath(), "");
            rootFile = new File(rootFile + uri);
            if (!rootFile.exists()) {
                return newFixedLengthResponse("Error! No such file or dirctory");
            }

            if (rootFile.isDirectory()) {
                // list directory files
                Log.d(TAG, "list " + rootFile.getPath());
                File[] files = rootFile.listFiles();
                File file1 = new File(FileUrl.INSTANCE.getMainFilesUrl(),  "css1.css");
                answer = "<html><head><meta http-equiv=\"Content-Type\" content=\"text/html; " +
                    "charset=utf-8\"><title> ZeroTermux File Browser</title><link rel=\"stylesheet\" href=\"" + file1.getAbsolutePath() + "\"  type=\"text/css\"/>" +
                    "<div id=\"title111\">ZeroTermux File Browser</div>";

                if (files != null && files.length > 0) {
                    Arrays.sort(
                        files,
                        new FileComparator()
                    );
                }
              /*  answer += "<a id=\"btn2\" href=\"" + rootFile.getParentFile().getAbsolutePath()
                    + "\" alt = \"\">" + UUtils.getString(com.termux.R.string.open_ttyd_http)
                    + "</a>";
                answer += "<a id=\"btn2\" href=\"" + rootFile.getParentFile().getAbsolutePath()
                    + "\" alt = \"\">" + UUtils.getString(com.termux.R.string.open_file_http)
                    + "</a>";*/
                answer += "<a id=\"btn2\" href=\"" + rootFile.getParentFile().getAbsolutePath()
                    + "\" alt = \"\">..."
                    + "</a>";
                for (File file : files) {

                    String isFile = UUtils.getString(com.termux.R.string.http_folder);
                    String isFileLink = "";
                    if (file.isDirectory()) {
                        isFile = UUtils.getString(com.termux.R.string.http_folder);
                        isFileLink = file.getName() + "/";
                    } else {
                        isFile = UUtils.getString(com.termux.R.string.http_file);
                        isFileLink = file.getName();
                    }
                    answer += "<a id=\"btn2\" href=\"" + file.getAbsolutePath()
                        + "\" alt = \"\">" + isFileLink
                        + "</a>";
                    /**
                     *
                     * <br>[time: "
                     *                         +  UUtils.getFtpDate(file.lastModified())
                     *                         + "] [size:" + FileIOUtils.INSTANCE.formatFileSize(file.length())
                     *                         + "] [type:" + isFile
                     *                         + "]<br>
                     *
                     */
                }

                answer += "</head></html>";
            } else {
                // serve file download
                InputStream inputStream;
                Response response = null;
                Log.d(TAG, "downloading file " + rootFile.getAbsolutePath());
                if (mStatusUpdateListener != null) {
                    mStatusUpdateListener.onDownloadingFile(rootFile, false);
                }

                try {
                    inputStream = new FileInputStream(rootFile);
                    response = new DownloadResponse(rootFile, inputStream);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                if (response != null) {
                    response.addHeader(
                        "Content-Disposition", "attachment; filename=" + rootFile.getName());
                    return response;
                } else {
                    return newFixedLengthResponse("Error downloading file!");
                }
            }
        }

        return newFixedLengthResponse(answer);
    }


    public void setOnStatusUpdateListener(OnStatusUpdateListener listener) {
        mStatusUpdateListener = listener;
    }
}
