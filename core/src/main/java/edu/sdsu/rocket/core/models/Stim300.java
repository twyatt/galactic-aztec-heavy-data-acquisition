package edu.sdsu.rocket.core.models;

import java.nio.ByteBuffer;

/**
 * @see <a href="http://www.sensonor.com/media/99614/ts1524.r20%20datasheet%20stim300.pdf">
 *     STIM300 Datasheet</a>
 */
public class Stim300 implements ByteBufferIo {

    class Data {
        int timestamp;
        float x, y, z;

        /** Put data into {@code buffer}. */
        void put(ByteBuffer buffer) {
            synchronized (this) {
                buffer.putInt(timestamp);
                buffer.putFloat(x);
                buffer.putFloat(y);
                buffer.putFloat(z);
            }
        }

        /** Get data from {@code buffer}. */
        void get(ByteBuffer buffer) {
            synchronized (this) {
                timestamp = buffer.getInt();
                x = buffer.getFloat();
                y = buffer.getFloat();
                z = buffer.getFloat();
            }
        }

        @Override
        public String toString() {
            synchronized (this) {
                return "[t=" + timestamp + ", x=" + x + ", y=" + y + ", z=" + z + "]";
            }
        }
    }

    private final Data gyro = new Data(); // accelerometer x, y, z
    private final Data acc  = new Data(); // gyro x, y, z
    private final Data incl = new Data(); // inclination x, y, z

    private static final byte GYRO_MASK = (byte) 0b0000_0001;
    private static final byte ACC_MASK  = (byte) 0b0000_0010;
    private static final byte INCL_MASK = (byte) 0b0000_0100;
    private static final byte ALL_MASK  = (byte) 0b1111_1111;

    @Override
    public void toByteBuffer(ByteBuffer buffer, byte mask) {
        if ((mask & GYRO_MASK) != 0) {
            gyro.put(buffer);
        }

        if ((mask & ACC_MASK) != 0) {
            acc.put(buffer);
        }

        if ((mask & INCL_MASK) != 0) {
            incl.put(buffer);
        }
    }

    @Override
    public void fromByteBuffer(ByteBuffer buffer, byte mask) {
        if ((mask & GYRO_MASK) != 0) {
            gyro.get(buffer);
        }

        if ((mask & ACC_MASK) != 0) {
            acc.get(buffer);
        }

        if ((mask & INCL_MASK) != 0) {
            incl.get(buffer);
        }
    }

    @Override
    public String toString() {
        return "{gyro=" + gyro + ", acc=" + acc + ", incl=" + incl + "}";
    }

}
