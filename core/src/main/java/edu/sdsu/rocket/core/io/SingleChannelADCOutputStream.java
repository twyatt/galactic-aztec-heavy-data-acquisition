package edu.sdsu.rocket.core.io;

import edu.sdsu.rocket.core.helpers.Stopwatch;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class SingleChannelADCOutputStream extends DataOutputStream {

    private final Stopwatch stopwatch = new Stopwatch();

    public SingleChannelADCOutputStream(OutputStream out) {
        super(out);
    }

    public long writeValue(float value) throws IOException {
        long timestamp = stopwatch.nanoSecondsElapsed();
        writeLong(timestamp);
        writeFloat(value);
        return timestamp;
    }

}
