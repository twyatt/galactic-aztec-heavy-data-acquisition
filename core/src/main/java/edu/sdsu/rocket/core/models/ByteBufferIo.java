package edu.sdsu.rocket.core.models;

import java.nio.ByteBuffer;

public interface ByteBufferIo {

    void toByteBuffer(ByteBuffer buffer, byte mask);
    void fromByteBuffer(ByteBuffer buffer, byte mask);

}
