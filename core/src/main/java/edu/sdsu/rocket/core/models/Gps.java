package edu.sdsu.rocket.core.models;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;

public class Gps {

    class PositionData {
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
    private final PositionData position = new PositionData();

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

	@Override
	public String toString() {
		synchronized (this) {
			return "{fix=" + fix + ", satellites=" + satellites.get() + ", position=" + position + "]";
		}
	}

}
