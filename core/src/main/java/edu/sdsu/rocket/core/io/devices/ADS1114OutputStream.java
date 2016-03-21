package edu.sdsu.rocket.core.io.devices;

import edu.sdsu.rocket.core.io.DeviceOutputStream;

import java.io.IOException;
import java.io.OutputStream;

public class ADS1114OutputStream extends DeviceOutputStream {

    public ADS1114OutputStream(OutputStream out) {
        super(out);
    }

    public void writeConfig(int config) throws IOException {
        writeInt(config);
    }

    public long writeValue(float value) throws IOException {
        long timestamp = writeTimestamp();
        writeFloat(value);
        return timestamp;
    }

}
