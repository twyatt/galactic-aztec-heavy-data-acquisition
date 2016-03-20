package edu.sdsu.rocket.core.io;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

public class SingleChannelADCInputStream extends DataInputStream {

    public class Reading {
        public long timestamp;
        public float value;
    }

    public SingleChannelADCInputStream(InputStream in) {
        super(in);
    }

    public Reading readReading() throws IOException {
        Reading reading = new Reading();
        reading.timestamp = readLong();
        reading.value = readFloat();
        return reading;
    }

}
