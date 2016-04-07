package edu.sdsu.rocket.core.io;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class StatusOutputStream extends DataOutputStream {

    public StatusOutputStream(OutputStream out) {
        super(out);
    }

    public void writeValue(long timestamp, int rawCpuTemperature) throws IOException {
        writeLong(timestamp);
        writeInt(rawCpuTemperature);
    }

}
