package edu.sdsu.rocket.core.models;

import java.nio.ByteBuffer;

/**
 * @see <a href="http://www.sensonor.com/media/99614/ts1524.r20%20datasheet%20stim300.pdf">
 *     STIM300 Datasheet</a>
 */
public class Stim300 {

    public class AxesData {
        private int timestamp;
        private float x, y, z;

        public int timestamp() {
            return timestamp;
        }

        public float x() {
            return x;
        }

        public float y() {
            return y;
        }

        public float z() {
            return z;
        }

        @Override
        public String toString() {
            return "[t=" + timestamp + ", x=" + x + ", y=" + y + ", z=" + z + "]";
        }
    }

    class AxesDataIo {
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

        public AxesData read() {
            AxesData obj = new AxesData();
            synchronized (this) {
                obj.timestamp = this.timestamp;
                obj.x = this.x;
                obj.y = this.y;
                obj.z = this.z;
            }
            return obj;
        }

        @Override
        public String toString() {
            synchronized (this) {
                return "[t=" + timestamp + ", x=" + x + ", y=" + y + ", z=" + z + "]";
            }
        }
    }

    private final AxesDataIo gyro = new AxesDataIo(); // accelerometer x, y, z
    private final AxesDataIo acc  = new AxesDataIo(); // gyro x, y, z
    private final AxesDataIo incl = new AxesDataIo(); // inclination x, y, z

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

    public AxesData gyroscope() {
        return gyro.read();
    }

    public AxesData accelerometer() {
        return acc.read();
    }

    public AxesData inclinometer() {
        return incl.read();
    }

    @Override
    public String toString() {
        return "{gyro=" + gyro + ", acc=" + acc + ", incl=" + incl + "}";
    }

}
