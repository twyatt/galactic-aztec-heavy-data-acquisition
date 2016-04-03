package edu.sdsu.rocket.core.io;

import java.io.IOException;
import java.io.OutputStream;

public class StatusOutputStream extends TimestampOutputStream {

    public StatusOutputStream(OutputStream out) {
        super(out);
    }

    public long writeValue(int rawCpuTemperature) throws IOException {
        long timestamp = writeTimestamp();
        writeInt(rawCpuTemperature);
        return timestamp;
    }

}
