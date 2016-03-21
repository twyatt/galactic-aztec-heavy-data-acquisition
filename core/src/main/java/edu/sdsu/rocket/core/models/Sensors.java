package edu.sdsu.rocket.core.models;

import java.nio.ByteBuffer;

public class Sensors {
	
	public final Analog analog = new Analog(6); // mV
	public final Pressures pressures = new Pressures(analog); // PSI
	public final GPS gps = new GPS(); // degrees, m
	public final Radio radio = new Radio(); // -dBm
	public final Status system = new Status(); // C
	
	public static final int ANALOG_MASK        = 0b0000_0001;
	public static final int GPS_MASK           = 0b0010_0000;
	public static final int RADIO_MASK         = 0b0100_0000;
	public static final int SYSTEM_MASK        = 0b1000_0000;
	public static final int ALL_MASK           = 0b1111_1111;
	
	public void toByteBuffer(ByteBuffer buffer) {
		toByteBuffer(buffer, ALL_MASK);
	}
	
	public void toByteBuffer(ByteBuffer buffer, int mask) {
		if (mask == 0) mask = ALL_MASK;
		
		if ((mask & ANALOG_MASK) != 0) {
			buffer.putFloat(analog.get(0));
			buffer.putFloat(analog.get(1));
			buffer.putFloat(analog.get(2));
			buffer.putFloat(analog.get(3));
			buffer.putFloat(analog.get(4));
			buffer.putFloat(analog.get(5));
		}

		if ((mask & GPS_MASK) != 0) {
			buffer.putDouble(gps.getLatitude());
			buffer.putDouble(gps.getLongitude());
			buffer.putDouble(gps.getAltitude());
			buffer.put((byte) (gps.getFixStatus() & 0xFF));
			buffer.put((byte) (gps.getSatellites() & 0xFF));
		}
		
		if ((mask & RADIO_MASK) != 0) {
			buffer.put((byte) (radio.getSignalStrength() & 0xFF));
		}
		
		if ((mask & SYSTEM_MASK) != 0) {
			buffer.putInt(system.getRawTemperature());
			buffer.put(system.getIsPowerGood() ? (byte) 0x1 : (byte) 0x0);
		}
	}
	
	public void fromByteBuffer(ByteBuffer buffer) {
		fromByteBuffer(buffer, ALL_MASK);
	}
	
	public void fromByteBuffer(ByteBuffer buffer, int mask) {
		if (mask == 0) mask = ALL_MASK;
		
		if ((mask & ANALOG_MASK) != 0) {
			analog.set(0, buffer.getFloat());
			analog.set(1, buffer.getFloat());
			analog.set(2, buffer.getFloat());
			analog.set(3, buffer.getFloat());
			analog.set(4, buffer.getFloat());
			analog.set(5, buffer.getFloat());
		}

		if ((mask & GPS_MASK) != 0) {
			gps.setLatitude(buffer.getDouble());
			gps.setLongitude(buffer.getDouble());
			gps.setAltitude(buffer.getDouble());
			gps.setFixStatus(buffer.get());
			gps.setSatellites(buffer.get());
		}
		
		if ((mask & RADIO_MASK) != 0) {
			radio.setSignalStrength(buffer.get());
		}
		
		if ((mask & SYSTEM_MASK) != 0) {
			system.setRawTemperature(buffer.getInt());
			system.setIsPowerGood(buffer.get() != 0);
		}
	}

}
