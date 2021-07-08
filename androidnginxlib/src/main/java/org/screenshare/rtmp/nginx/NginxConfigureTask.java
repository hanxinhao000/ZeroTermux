package org.screenshare.rtmp.nginx;

import android.content.Context;
import android.os.AsyncTask;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Logger;

/**
 * Nginx configure task.
 * 
 */
public class NginxConfigureTask extends AsyncTask<String, Integer, Boolean> {

	/**
	 * Nginx config file name.
	 */
	public static final String NGINX_CONF_FILENAME = "nginx.conf";

	/**
	 * Nginx mime-type list file name.
	 */
	public static final String NGINX_MIMETYPES_FILENAME = "mime.types";

	/**
	 * Default task number.
	 */
	private static final int DEFAULT_TASK_NUMBER = 3;

	/**
	 * Percentage magic number.
	 */
	private static final int PERCENTAG_MAGIC_NUMBER = 100;

	/**
	 * Read buffer size.
	 */
	private static final int READ_BUFFER_SIZE = 1024;

	/**
	 * Task progress.
	 */
	private int taskProgress = 0;

	/**
	 * Default task number.
	 */
	private int taskNumber = DEFAULT_TASK_NUMBER;

	/**
	 * Logger.
	 */
	private static Logger logger = Logger.getLogger("nginx.android");

	/**
	 * Context.
	 */
	private Context cxt;

	/**
	 * Nginx root directory.
	 */
	private File nginxDirectory;

	/**
	 * Nginx conf directory.
	 */
	private File confDirectory;

	/**
	 * Nginx logs directory.
	 */
	private File logsDirectory;

	/**
	 * Exception.
	 */
	private Exception exception;

	/**
	 * Constructor with context.
	 * @param context context
	 */
	public NginxConfigureTask(final Context context) {
		this(context, context.getDir("nginx", Context.MODE_PRIVATE));
	}

	/**
	 * Constructor with context and root directory.
	 * @param context context
	 * @param rootDirectory root directory
	 */
	public NginxConfigureTask(final Context context, final File rootDirectory) {
		this.cxt = context;

		nginxDirectory = rootDirectory;
		logsDirectory = new File(nginxDirectory, "logs");
		confDirectory = new File(nginxDirectory, "conf");
	}

	/**
	 * Get exception.
	 * @return exception
	 */
	public Exception getException() {
		return exception;
	}

	@Override
	protected Boolean doInBackground(final String... params) {

		taskProgress = 0;
		taskNumber = DEFAULT_TASK_NUMBER;
		if (params != null) {
			taskNumber += params.length;
		}

		publishProgress(0);

		if (!makeDirectory(nginxDirectory)) {
			publishProgress(PERCENTAG_MAGIC_NUMBER);
			return false;
		}
		countUpTaskProgress();
		
		if (!makeDirectory(logsDirectory)) {
			publishProgress(PERCENTAG_MAGIC_NUMBER);
			return false;
		}
		countUpTaskProgress();
		
		if (!makeDirectory(confDirectory)) {
			publishProgress(PERCENTAG_MAGIC_NUMBER);
			return false;
		}
		countUpTaskProgress();

		if (params == null) {
			publishProgress(PERCENTAG_MAGIC_NUMBER);

			return true;
		}

		for (String file : params) {
			InputStream is = null;
			try {
				is = cxt.getAssets().open(file);
				copyNginxConf(is, new File(confDirectory, file));
			} catch (IOException e) {
				logger.severe(e.toString());
				exception = e;
				publishProgress(PERCENTAG_MAGIC_NUMBER);
				return false;
			} finally {
				if (is != null) {
					try {
						is.close();
					} catch (IOException e) {
						logger.warning(e.toString());
					}
				}
			}
			countUpTaskProgress();
		}

		publishProgress(PERCENTAG_MAGIC_NUMBER);
		return true;
	}

	/**
	 * Update task progress.
	 */
	private void countUpTaskProgress() {
		taskProgress++;
		publishProgress(PERCENTAG_MAGIC_NUMBER * taskProgress / taskNumber);
	}

	/**
	 * Make directory.
	 * @param dir directory
	 * @return create directory or it has already existed
	 */
	private boolean makeDirectory(final File dir) {
		if (dir.exists()) {
			return true;
		}

		boolean result = dir.mkdir();
		
		return result;
	}

	/**
	 * Copy nginx config file.
	 * @param source source data input stream
	 * @param destFile destination file
	 * @throws IOException I/O Error
	 */
	private void copyNginxConf(
			final InputStream source, final File destFile) throws IOException {

//		if (destFile.exists()) {
//			return;
//		}

		OutputStream os = null;
		try {
			os = new FileOutputStream(destFile);

			byte[] buf = new byte[READ_BUFFER_SIZE];
			while (true) {
				int len = source.read(buf);
				if (len < 0) {
					break;
				}
				os.write(buf, 0, len);
			}
		} catch (IOException e) {
			logger.severe(e.toString());
			throw e;
		} finally {
			if (os != null) {
				try {
					os.close();
				} catch (IOException e) {
					logger.warning(e.toString());
				}
			}
		}
		
	}

}
