package edu.sdsu.rocket.core.io;

import java.io.IOException;
import java.io.InputStream;

public class StatusInputStream extends TimestampInputStream {

    public class Reading {
        public long timestamp;
        public int value;
    }

    public StatusInputStream(InputStream in) {
        super(in);
    }

    public Reading readValue() throws IOException {
        Reading reading = new Reading();
        readValue(reading);
        return reading;
    }

    public void readValue(Reading out) throws IOException {
        out.timestamp = readTimestamp();
        out.value = readInt();
    }

}
