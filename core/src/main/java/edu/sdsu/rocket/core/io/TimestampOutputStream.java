package edu.sdsu.rocket.core.io;

import edu.sdsu.rocket.core.helpers.Stopwatch;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public abstract class TimestampOutputStream extends DataOutputStream {

    private final Stopwatch stopwatch = new Stopwatch();

    public TimestampOutputStream(OutputStream out) {
        super(out);
    }

    public long writeTimestamp() throws IOException {
        long timestamp = stopwatch.nanoSecondsElapsed();
        writeLong(timestamp);
        return timestamp;
    }

}
