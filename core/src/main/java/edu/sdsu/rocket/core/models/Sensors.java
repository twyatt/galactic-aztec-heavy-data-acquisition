package edu.sdsu.rocket.core.models;

import java.nio.ByteBuffer;

public class Sensors {
	
	public final Accelerometer accelerometer = new Accelerometer(); // Gs
	public final Gyroscope gyroscope = new Gyroscope(); // deg/sec
	public final Magnetometer magnetometer = new Magnetometer();
	public final Barometer barometer = new Barometer(); // C, mbar
	public final Analog analog = new Analog(6); // mV
	public final Pressures pressures = new Pressures(analog); // PSI
	public final GPS gps = new GPS(); // degrees, m
	public final Radio radio = new Radio(); // -dBm
	public final Status system = new Status(); // C
	
	public static final int ANALOG_MASK        = 0x1;
	public static final int BAROMETER_MASK     = 0x2;
	public static final int ACCELEROMETER_MASK = 0x4;
	public static final int GYROSCOPE_MASK     = 0x8;
	public static final int MAGNETOMETER_MASK  = 0x10;
	public static final int GPS_MASK           = 0x20;
	public static final int RADIO_MASK         = 0x40;
	public static final int SYSTEM_MASK        = 0x80;
	public static final int ALL_MASK           = 0xFF;
	
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
		
		if ((mask & BAROMETER_MASK) != 0) {
			buffer.putInt(barometer.getRawTemperature());
			buffer.putInt(barometer.getRawPressure());
		}
		
		if ((mask & ACCELEROMETER_MASK) != 0) {
			buffer.putFloat(accelerometer.getScalingFactor());
			buffer.putShort((short) accelerometer.getRawX());
			buffer.putShort((short) accelerometer.getRawY());
			buffer.putShort((short) accelerometer.getRawZ());
		}
		
		if ((mask & GYROSCOPE_MASK) != 0) {
			buffer.putFloat(gyroscope.getScalingFactor());
			buffer.putShort((short) gyroscope.getRawX());
			buffer.putShort((short) gyroscope.getRawY());
			buffer.putShort((short) gyroscope.getRawZ());
		}
		
		if ((mask & MAGNETOMETER_MASK) != 0) {
			buffer.putFloat(magnetometer.getScalingFactor());
			buffer.putShort((short) magnetometer.getRawX());
			buffer.putShort((short) magnetometer.getRawY());
			buffer.putShort((short) magnetometer.getRawZ());
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
		
		if ((mask & BAROMETER_MASK) != 0) {
			barometer.setRawTemperature(buffer.getInt());
			barometer.setRawPressure(buffer.getInt());
		}
		
		if ((mask & ACCELEROMETER_MASK) != 0) {
			accelerometer.setScalingFactor(buffer.getFloat());
			accelerometer.setRawX(buffer.getShort());
			accelerometer.setRawY(buffer.getShort());
			accelerometer.setRawZ(buffer.getShort());
		}
		
		if ((mask & GYROSCOPE_MASK) != 0) {
			gyroscope.setScalingFactor(buffer.getFloat());
			gyroscope.setRawX(buffer.getShort());
			gyroscope.setRawY(buffer.getShort());
			gyroscope.setRawZ(buffer.getShort());
		}
		
		if ((mask & MAGNETOMETER_MASK) != 0) {
			magnetometer.setScalingFactor(buffer.getFloat());
			magnetometer.setRawX(buffer.getShort());
			magnetometer.setRawY(buffer.getShort());
			magnetometer.setRawZ(buffer.getShort());
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
