package edu.sdsu.rocket.core.io.devices;

import edu.sdsu.rocket.core.io.DeviceOutputStream;

import java.io.IOException;
import java.io.OutputStream;

public class ADS1100OutputStream extends DeviceOutputStream {

    public ADS1100OutputStream(OutputStream out) {
        super(out);
    }

    public void writeConfig(int config, float Vdd) throws IOException {
        writeInt(config);
        writeFloat(Vdd);
    }

    public long writeValue(float value) throws IOException {
        long timestamp = writeTimestamp();
        writeFloat(value);
        return timestamp;
    }

}
