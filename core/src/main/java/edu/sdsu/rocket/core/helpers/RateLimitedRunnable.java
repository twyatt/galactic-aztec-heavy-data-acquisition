package edu.sdsu.rocket.core.helpers;

import java.util.concurrent.TimeUnit;

public abstract class RateLimitedRunnable implements Runnable {
	
	private static final long NANOSECONDS_PER_MILLISECOND = 1000000L;

	private long sleep_ms;
	private long sleep_ns;

	private final Object lock = new Object();
	private boolean isPaused;

	private boolean isRunning = true;
	
	public RateLimitedRunnable() {
		this(0L, false);
	}
	
	public RateLimitedRunnable(boolean startPaused) {
		this(0L, startPaused);
	}
	
	public RateLimitedRunnable(long sleepMilliseconds) {
		this(sleepMilliseconds, false);
	}
	
	public RateLimitedRunnable(long sleepMilliseconds, boolean startPaused) {
		setSleep(sleepMilliseconds);
		isPaused = startPaused;
	}
	
	public void setRunning(boolean isRunning) {
		this.isRunning = isRunning;
	}

	/**
	 * Sets the running frequency.
	 * 
	 * @param frequency Hz
	 */
	public void setFrequency(float frequency) {
		if (frequency == 0f) {
			throw new IllegalArgumentException("Frequency cannot be zero.");
		}
		setSleep(Math.round(1000f / frequency));
	}
	
	/**
	 * Sets the duration runnable sleeps per loop (in milliseconds).
	 */
	public void setSleep(long milliseconds) {
		sleep_ms = milliseconds;
		sleep_ns = 0;
	}
	
	/**
	 * Sets the duration runnable sleeps per loop (in nanoseconds).
	 */
	public void setSleepNanoseconds(long nanoseconds) {
		// http://stackoverflow.com/q/4300653/196486
		if (nanoseconds > 999999) {
			sleep_ms = nanoseconds / NANOSECONDS_PER_MILLISECOND;
			sleep_ns = TimeUnit.NANOSECONDS.toMillis(nanoseconds);
		} else {
			sleep_ms = 0;
			sleep_ns = nanoseconds;
		}
	}
	
	/**
	 * Returns the duration runnable sleeps per loop (in milliseconds).
	 * 
	 * @return
	 */
	public long getSleep() {
		return sleep_ms;
	}
	
	public long getSleepNanoseconds() {
		return sleep_ms * NANOSECONDS_PER_MILLISECOND + sleep_ns;
	}
	
	public void pause() {
		synchronized (lock) {
			isPaused = true;
		}
	}
	
	public void resume() {
		synchronized (lock) {
			if (isPaused) {
				isPaused = false;
				lock.notifyAll();
			}
		}
	}
	
	public boolean isPaused() {
		return isPaused;
	}
	
	@Override
	public final void run() {
		while (!Thread.currentThread().isInterrupted() && isRunning) {
			try {
				loop();
				
				if (sleep_ms != 0 || sleep_ns != 0) {
					if (sleep_ns == 0) {
						Thread.sleep(sleep_ms);
					} else {
						Thread.sleep(sleep_ms, (int) sleep_ns);
					}
				}
				
				synchronized (lock) {
					while (isPaused) {
						lock.wait();
					}
				}
			} catch (InterruptedException e) {
				System.err.println(e);
				return;
			}
		}
	}
	
	public abstract void loop() throws InterruptedException;

}
