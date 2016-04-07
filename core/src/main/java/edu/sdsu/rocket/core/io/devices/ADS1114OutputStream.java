package edu.sdsu.rocket.core.io.devices;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class ADS1114OutputStream extends DataOutputStream {

    public ADS1114OutputStream(OutputStream out) {
        super(out);
    }

    public void writeConfig(int config) throws IOException {
        writeInt(config);
    }

    public void writeValue(long timestamp, float value) throws IOException {
        writeLong(timestamp);
        writeFloat(value);
    }

}
