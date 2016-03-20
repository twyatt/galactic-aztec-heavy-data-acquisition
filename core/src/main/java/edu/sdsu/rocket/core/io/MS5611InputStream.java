package edu.sdsu.rocket.core.io;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

public class MS5611InputStream extends DataInputStream {
	
	public class MS5611Reading {
		public long timestamp;
		public int values[] = new int[2]; // C * 100, mbar * 100
	}

	public MS5611InputStream(InputStream in) {
		super(in);
	}
	
	public MS5611Reading readReading() throws IOException {
		int type = 0;
		while ((type = read()) != -1) {
			switch (type) {
			case MS5611OutputStream.SENSOR_VALUES:
				MS5611Reading reading = new MS5611Reading();
				reading.timestamp = readLong();
				reading.values[0] = readInt();
				reading.values[1] = readInt();
				return reading;
			case MS5611OutputStream.FAULT:
				readLong(); // timestamp
				System.err.println("Fault: " + readInt());
				break;
			default:
				throw new IOException("Unsupported value type: " + type);
			}
		}
		return null;
	}

}
