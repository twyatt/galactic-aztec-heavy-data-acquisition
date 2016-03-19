package edu.sdsu.rocket.core.io;

import edu.sdsu.rocket.core.helpers.Stopwatch;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class ADS1100OutputStream extends DataOutputStream {

	public static final byte SENSOR_VALUE = 0x0;

	private final Stopwatch stopwatch = new Stopwatch();

	public ADS1100OutputStream(OutputStream out) {
		super(out);
	}

	public void writeValue(float value) throws IOException {
		write(SENSOR_VALUE);
		writeLong(stopwatch.nanoSecondsElapsed());
		writeFloat(value);
	}

}
