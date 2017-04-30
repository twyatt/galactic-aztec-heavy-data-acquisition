package edu.sdsu.rocket.core.models;

import java.nio.ByteBuffer;

public class ProxyData {

    private Stim300 stim300 = new Stim300();
    private Gps gps = new Gps();

    public void fromByteBuffer(ByteBuffer buffer) {
        stim300.fromByteBuffer(buffer);
        gps.fromByteBuffer(buffer);
    }

    public void toByteBuffer(ByteBuffer buffer) {
        stim300.toByteBuffer(buffer);
        gps.toByteBuffer(buffer);
    }

    @Override
    public String toString() {
        return "(stim300=" + stim300 + ", gps=" + gps + ")";
    }

}
