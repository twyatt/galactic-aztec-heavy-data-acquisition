package edu.sdsu.rocket.core.models;

import edu.sdsu.rocket.core.helpers.AtomicIntDouble;
import edu.sdsu.rocket.core.helpers.AtomicIntFloat;

import java.nio.ByteBuffer;

public class Sensors {
    
    public final AtomicIntFloat[] analog = new AtomicIntFloat[6]; // mV
    public final AtomicIntDouble phidgets = new AtomicIntDouble();
    public final GPS gps = new GPS(); // degrees, m
    public final Radio radio = new Radio(); // -dBm
    public final Status system = new Status(); // C
    
    public static final byte ANALOG_MASK = (byte) 0b0000_0001;
    public static final byte SYSTEM_MASK = (byte) 0b0000_0010;
    public static final byte GPS_MASK    = (byte) 0b0000_0100;
    public static final byte RADIO_MASK  = (byte) 0b0000_1000;
    public static final byte PHIDGETS_MASK = (byte) 0b0001_0000;
    public static final byte ALL_MASK    = (byte) 0b1111_1111;

    public Sensors() {
        for (int i = 0; i < analog.length; i++) {
            analog[i] = new AtomicIntFloat();
        }
    }

    public void toByteBuffer(ByteBuffer buffer) {
        toByteBuffer(buffer, ALL_MASK);
    }
    
    public void toByteBuffer(ByteBuffer buffer, byte mask) {
        if (mask == 0) mask = ALL_MASK;
        
        if ((mask & ANALOG_MASK) != 0) {
            buffer.putLong(analog[0].get());
            buffer.putLong(analog[1].get());
            buffer.putLong(analog[2].get());
            buffer.putLong(analog[3].get());
            buffer.putLong(analog[4].get());
            buffer.putLong(analog[5].get());
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
        }

        if ((mask & PHIDGETS_MASK) != 0) {
            AtomicIntDouble.IntDoubleValuePair pair = phidgets.get();
            buffer.putInt(pair.intValue);
            buffer.putDouble(pair.doubleValue);
        }
    }
    
    public void fromByteBuffer(ByteBuffer buffer) {
        fromByteBuffer(buffer, ALL_MASK);
    }
    
    public void fromByteBuffer(ByteBuffer buffer, byte mask) {
        if (mask == 0) mask = ALL_MASK;
        
        if ((mask & ANALOG_MASK) != 0) {
            analog[0].set(buffer.getLong());
            analog[1].set(buffer.getLong());
            analog[2].set(buffer.getLong());
            analog[3].set(buffer.getLong());
            analog[4].set(buffer.getLong());
            analog[5].set(buffer.getLong());
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
        }

        if ((mask & PHIDGETS_MASK) != 0) {
            phidgets.set(buffer.getInt(), buffer.getDouble());
        }
    }

}
