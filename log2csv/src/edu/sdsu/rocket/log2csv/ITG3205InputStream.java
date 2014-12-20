package edu.sdsu.rocket.log2csv;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

public class ITG3205InputStream extends DataInputStream {
	
	public class ITG3205Reading {
		public long timestamp;
		public float scalingFactor = 1f;
		public short values[] = new short[3];
	}

	private float scalingFactor = 1f;

	public ITG3205InputStream(InputStream in) {
		super(in);
	}
	
	public ITG3205Reading readReading() throws IOException {
		int type = 0;
		while ((type = read()) != -1) {
			switch (type) {
			case 0: // sensor values
				ITG3205Reading reading = new ITG3205Reading();
				reading.timestamp = readLong();
				reading.values[0] = readShort();
				reading.values[1] = readShort();
				reading.values[2] = readShort();
				reading.scalingFactor = scalingFactor;
				return reading;
			case 1: // scaling factor
				scalingFactor = readFloat();
				break;
			default:
				throw new IOException("Unsupported value type: " + type);
			}
		}
		return null;
	}

}
