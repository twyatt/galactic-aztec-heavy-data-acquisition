package edu.sdsu.rocket.core.io.devices;

import java.io.IOException;
import java.io.OutputStream;

public class ADS1115OutputStream extends ADS1114OutputStream {

    public ADS1115OutputStream(OutputStream out) {
        super(out);
    }

    public long writeValue(int channel, float value) throws IOException {
        long timestamp = writeTimestamp();
        writeByte(channel);
        writeFloat(value);
        return timestamp;
    }

}
