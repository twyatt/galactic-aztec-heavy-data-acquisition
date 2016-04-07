package edu.sdsu.rocket.core.io.devices;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class ADS1100OutputStream extends DataOutputStream {

    public ADS1100OutputStream(OutputStream out) {
        super(out);
    }

    public void writeConfig(int config, float Vdd) throws IOException {
        writeInt(config);
        writeFloat(Vdd);
    }

    public void writeValue(long timestamp, float value) throws IOException {
        writeLong(timestamp);
        writeFloat(value);
    }

}
