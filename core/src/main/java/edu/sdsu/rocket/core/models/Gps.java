package edu.sdsu.rocket.core.models;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;

public class Gps {

    public class PositionData {
        private int timestamp;
        private double latitude, longitude, altitude;

        public int timestamp() {
            return timestamp;
        }

        public double latitude() {
            return latitude;
        }

        public double longitude() {
            return longitude;
        }

        public double altitude() {
            return altitude;
        }

        @Override
        public String toString() {
            return "[t=" + timestamp + ", latitude=" + latitude + ", longitude=" + longitude + ", altitude=" + altitude + "]";
        }
    }

    class PositionDataIo {
        private int timestamp;
        private double latitude, longitude, altitude;

        /** Put data into {@code buffer}. */
        void put(ByteBuffer buffer) {
            synchronized (this) {
                buffer.putInt(timestamp);
                buffer.putDouble(latitude);
                buffer.putDouble(longitude);
                buffer.putDouble(altitude);
            }
        }

        /** Get data from {@code buffer}. */
        void get(ByteBuffer buffer) {
            synchronized (this) {
                timestamp = buffer.getInt();
                latitude = buffer.getDouble();
                longitude = buffer.getDouble();
                altitude = buffer.getDouble();
            }
        }

        public PositionData read() {
            PositionData obj = new PositionData();
            synchronized (this) {
                obj.timestamp = this.timestamp;
                obj.latitude = this.latitude;
                obj.longitude = this.longitude;
                obj.altitude = this.altitude;
            }
            return obj;
        }

        @Override
        public String toString() {
            synchronized (this) {
                return "[t=" + timestamp + ", latitude="
                        + latitude + ", longitude=" + longitude + ", altitude=" + altitude + "]";
            }
        }
    }

	/**
	 * Fix Status
	 *
	 * 0 = Unknown
	 * 1 = No fix
	 * 2 = 2D fix
	 * 3 = 3D fix
	 */
	private volatile byte fix;

	private final AtomicInteger satellites = new AtomicInteger();
    private final PositionDataIo position = new PositionDataIo();

	/** Put data into {@code buffer}. */
	public void toByteBuffer(ByteBuffer buffer) {
        buffer.put(fix);
        buffer.putInt(satellites.get());
        position.put(buffer);
	}

	/** Get data from {@code buffer}. */
	public void fromByteBuffer(ByteBuffer buffer) {
        fix = buffer.get();
        satellites.set(buffer.getInt());
        position.get(buffer);
	}

	public byte fix() {
	    return fix;
    }

    public int satellites() {
	    return satellites.get();
    }

    public PositionData position() {
	    return position.read();
    }

	@Override
	public String toString() {
		synchronized (this) {
			return "{fix=" + fix + ", satellites=" + satellites.get() + ", position=" + position + "]";
		}
	}

}
