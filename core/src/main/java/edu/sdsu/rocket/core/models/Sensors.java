package edu.sdsu.rocket.core.models;

import edu.sdsu.rocket.core.helpers.AtomicIntFloat;

import java.nio.ByteBuffer;

public class Sensors implements ByteBufferIo {

    public final AtomicIntFloat[] analog = new AtomicIntFloat[6]; // mV
    public final ProxyData proxy = new ProxyData();
    public final Radio radio = new Radio(); // -dBm
    public final Status system = new Status(); // C

    public static final byte ANALOG_MASK = (byte) 0b0000_0001;
    public static final byte PROXY_MASK  = (byte) 0b0000_0010;
    public static final byte SYSTEM_MASK = (byte) 0b0000_0100;
    public static final byte RADIO_MASK  = (byte) 0b0000_1000;
    public static final byte ALL_MASK    = (byte) 0b1111_1111;

    public Sensors() {
        for (int i = 0; i < analog.length; i++) {
            analog[i] = new AtomicIntFloat();
        }
    }

    public void toByteBuffer(ByteBuffer buffer) {
        toByteBuffer(buffer, ALL_MASK);
    }

    @Override
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

        if ((mask & PROXY_MASK) != 0) {
            proxy.toByteBuffer(buffer);
        }

        if ((mask & SYSTEM_MASK) != 0) {
            buffer.putInt(system.getRawTemperature());
        }

        if ((mask & RADIO_MASK) != 0) {
            buffer.put((byte) (radio.getSignalStrength() & 0xFF));
        }
    }

    public void fromByteBuffer(ByteBuffer buffer) {
        fromByteBuffer(buffer, ALL_MASK);
    }

    @Override
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

        if ((mask & PROXY_MASK) != 0) {
            proxy.fromByteBuffer(buffer);
        }

        if ((mask & SYSTEM_MASK) != 0) {
            system.setRawTemperature(buffer.getInt());
        }

        if ((mask & RADIO_MASK) != 0) {
            radio.setSignalStrength(buffer.get());
        }
    }

}
