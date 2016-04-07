package edu.sdsu.rocket.core.io.devices;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

public class ADS1114InputStream extends DataInputStream {

    public class Reading {
        public long timestamp;
        public float value;
    }

    public ADS1114InputStream(InputStream in) {
        super(in);
    }

    public int readConfig() throws IOException {
        return readInt();
    }

    public Reading readValue() throws IOException {
        Reading reading = new Reading();
        readValue(reading);
        return reading;
    }

    public void readValue(Reading out) throws IOException {
        out.timestamp = readLong();
        out.value = readFloat();
    }

}
