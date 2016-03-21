package edu.sdsu.rocket.core.io;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

public abstract class DeviceInputStream extends DataInputStream {

    public DeviceInputStream(InputStream in) {
        super(in);
    }

    public long readTimestamp() throws IOException {
        return readLong();
    }

}
