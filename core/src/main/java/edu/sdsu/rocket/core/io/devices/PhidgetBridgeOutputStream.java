package edu.sdsu.rocket.core.io.devices;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class PhidgetBridgeOutputStream extends DataOutputStream {

    public PhidgetBridgeOutputStream(OutputStream out) {
        super(out);
    }

    public void writeConfig(int serialNumber, int deviceVersion, int inputCount, int gain, int dataRate) throws IOException {
        writeInt(serialNumber);
        writeInt(deviceVersion);
        writeInt(inputCount);
        writeInt(gain);
        writeInt(dataRate);
    }

    public void writeValue(long timestamp, double value) throws IOException {
        writeLong(timestamp);
        writeDouble(value);
    }

}
