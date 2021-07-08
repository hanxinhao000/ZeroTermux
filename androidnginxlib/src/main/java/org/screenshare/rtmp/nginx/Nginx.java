package org.screenshare.rtmp.nginx;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Nginx HTTP Server.
 */
public class Nginx {

	/**
	 * nginx instance.
	 */
	private static Nginx nginx = null;

	/**
	 * address.
	 */
	private InetSocketAddress address;

	/**
	 * nginx executor.
	 */
	private NginxExecutor executor = null;

	static {
		System.loadLibrary("nginx");
	}

	/**
	 * Constructor.
	 */
	protected Nginx() {
		executor = new NginxExecutor();
	}

	/**
	 * Create Nginx instance.
	 * @return Nginx instance
	 */
	public static Nginx create() {
		if (nginx == null) {
			nginx = new Nginx();
		}
		return nginx;
	}

	/**
	 * Create Nginx instance with address and back log.
	 * @param addr address
	 * @param backlog back log
	 * @return Nginx instance
	 */
	public static Nginx create(final InetSocketAddress addr, final int backlog) {
		Nginx instance = create();
		instance.bind(addr, backlog);
		return instance;
	}

	/**
	 * Get address.
	 * @return address
	 */
	public InetSocketAddress getAddress() {
		return address;
	}

	/**
	 * Bind address to nginx server.
	 * @param addr address
	 * @param backlog back log
	 */
	public void bind(final InetSocketAddress addr, final int backlog) {
		this.address = addr;
		setPort(addr.getPort());
		setHostName(addr.getHostName());
	}

	/**
	 * Start nginx.
	 */
	public void start() {
		start((String) null, (String) null);
	}

	/**
	 * Start nginx with cnofig and prefix path.
	 * @param config config file
	 * @param prefix prefix directory
	 */
	public void start(final File config, final File prefix) {
		String configPath = null;
		if (config != null) {
			configPath = config.getAbsolutePath(); 
		}
		String prefixPath = null;
		if (config != null) {
			prefixPath = prefix.getAbsolutePath(); 
		}
		start(configPath, prefixPath);
	}

	/**
	 * Start nginx with cnofig and prefix path.
	 * @param config config file path
	 * @param prefix prefix directory path
	 */
	public void start(final String config, final String prefix) {
		if (executor.isTerminated()) {
//			if (config != null) {
//				setConfigPath(config);
//			}
//
//			if (prefix != null) {
//				setConfigPath(prefix);
//			}

			setConfigPath("/sdcard/nginx/conf/nginx.conf");

			executor.execute(new Runnable() {
				@Override
				public void run() {
					executor.running = true;
					startNative();
					executor.running = false;
				}
			});
		}
	}

	/**
	 * Stop nginx.
	 * @param delay delay time(unit second)
	 */
	public void stop(final int delay) {
		if (!executor.isTerminated()) {
			try {
				executor.awaitTermination(delay, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Get nginx executor.
	 * @return exxecutor
	 */
	public Executor getExecutor() {
		return executor;
	}

	/**
	 * Set executor.
	 * @deprecated not supported
	 */
	public void setExecutor() {
		// not support custom executor
		throw new UnsupportedOperationException("not support custom executor");
	}

	/**
	 * Set prefix directory.
	 * @param prefix prefix
	 */
	public void setPrefixPath(final File prefix) {
		setPrefixPath(prefix.getAbsolutePath());
	}

	/**
	 * Get prefix directory.
	 * @return prefix directory
	 */
	public File getPrefix() {
		return new File(getPrefixPath());
	}

	/**
	 * Get prefix directory path.
	 * @return prefix directory path
	 */
	public native String getPrefixPath();

	/**
	 * Set prefix directory path.
	 * @param prefix prefix directory path
	 */
	public native void setPrefixPath(final String prefix);

	/**
	 * Set config file path.
	 * @param config config file path
	 */
	public native void setConfigPath(final String config);

	/**
	 * Start nginx in native.
	 */
	private native void startNative();

	/**
	 * Stop nginx in native.
	 */
	private native void stopNative();

	/**
	 * Set port.
	 * @param port port number
	 * @return result
	 */
	private native int setPort(final int port);

	/**
	 * Set host name.
	 * @param host host name
	 * @return result
	 */
	private native int setHostName(final String host);

	/**
	 * Nginx Executor.
	 */
	private class NginxExecutor implements ExecutorService {

		/**
		 * Wait polling term.
		 */
		private static final int WAIT_TERM = 50;

		/**
		 * Running flag.
		 */
		private boolean running;

		@Override
		public boolean isShutdown() {
			return !running;
		}

		@Override
		public boolean isTerminated() {
			return !running;
		}

		@Override
		public void execute(final Runnable command) {
			(new Thread(command)).start();
		}

		@Override
		public void shutdown() {
			stopNative();
		}

		@Override
		public List<Runnable> shutdownNow() {
			shutdown();
			return new ArrayList<Runnable>();
		}

		@Override
		public boolean awaitTermination(final long timeout, final TimeUnit unit)
				throws InterruptedException {

			// stop server
			shutdown();

			// create await time
			long msTimeout = TimeUnit.MILLISECONDS.convert(timeout, unit);  
			long current = System.currentTimeMillis();

			// wait stop or timeout
			while (executor != null) {
				if (msTimeout <= System.currentTimeMillis() - current) {
					break;
				}

				try {
					Thread.sleep(WAIT_TERM);
				} catch (InterruptedException e) {
					break;
				}
			}
			return false;
		}

		@Override
		public <T> List<Future<T>> invokeAll(
				final Collection<? extends Callable<T>> arg0)
				throws InterruptedException {
			throw new UnsupportedOperationException();
		}

		@Override
		public <T> List<Future<T>> invokeAll(
				final Collection<? extends Callable<T>> arg0,
				final long arg1, final TimeUnit arg2)
				throws InterruptedException {
			throw new UnsupportedOperationException();
		}

		@Override
		public <T> T invokeAny(final Collection<? extends Callable<T>> arg0)
				throws InterruptedException, ExecutionException {
			throw new UnsupportedOperationException();
		}

		@Override
		public <T> T invokeAny(final Collection<? extends Callable<T>> arg0,
				final long arg1, final TimeUnit arg2) throws InterruptedException,
				ExecutionException, TimeoutException {
			throw new UnsupportedOperationException();
		}

		@Override
		public <T> Future<T> submit(final Callable<T> arg0) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Future<?> submit(final Runnable task) {
			throw new UnsupportedOperationException();
		}

		@Override
		public <T> Future<T> submit(final Runnable task, final T result) {
			throw new UnsupportedOperationException();
		}
		
	}

}
