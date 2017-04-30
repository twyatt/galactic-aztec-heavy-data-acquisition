package edu.sdsu.rocket.core.models;

import java.nio.ByteBuffer;

/**
 * @see <a href="http://www.sensonor.com/media/99614/ts1524.r20%20datasheet%20stim300.pdf">
 *     STIM300 Datasheet</a>
 */
public class Stim300 {

    class AxesData {
        private int timestamp;
        private float x, y, z;

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

    private final AxesData gyro = new AxesData(); // accelerometer x, y, z
    private final AxesData acc  = new AxesData(); // gyro x, y, z
    private final AxesData incl = new AxesData(); // inclination x, y, z

    public void toByteBuffer(ByteBuffer buffer) {
        gyro.put(buffer);
        acc.put(buffer);
        incl.put(buffer);
    }

    public void fromByteBuffer(ByteBuffer buffer) {
        gyro.get(buffer);
        acc.get(buffer);
        incl.get(buffer);
    }

    @Override
    public String toString() {
        return "{gyro=" + gyro + ", acc=" + acc + ", incl=" + incl + "}";
    }

}
