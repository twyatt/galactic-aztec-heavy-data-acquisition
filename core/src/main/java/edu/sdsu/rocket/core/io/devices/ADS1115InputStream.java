package edu.sdsu.rocket.core.io.devices;

import java.io.IOException;
import java.io.InputStream;

public class ADS1115InputStream extends ADS1114InputStream {

    public class Reading extends ADS1114InputStream.Reading {
        public int channel;
        public long timestamp;
        public float value;
    }

    public ADS1115InputStream(InputStream in) {
        super(in);
    }

    public Reading readValue() throws IOException {
        Reading reading = new Reading();
        readValue(reading);
        return reading;
    }

    public void readValue(Reading out) throws IOException {
        out.timestamp = readLong();
        out.channel = readByte();
        out.value = readFloat();
    }

}
