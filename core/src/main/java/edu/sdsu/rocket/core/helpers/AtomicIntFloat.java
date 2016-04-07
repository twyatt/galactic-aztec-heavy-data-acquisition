package edu.sdsu.rocket.core.helpers;

import java.util.concurrent.atomic.AtomicLong;

import static java.lang.Float.floatToIntBits;
import static java.lang.Float.intBitsToFloat;

/**
 * An {@code int} and {@code float} value pair that may be updated atomically.
 */
public class AtomicIntFloat extends AtomicLong {

    public AtomicIntFloat() {
        this(0, 0f);
    }

    public AtomicIntFloat(int intValue, float floatValue) {
        super(raw(intValue, floatValue));
    }

    public static int getIntValue(long raw) {
        return (int) (raw >> 32);
    }

    public static float getFloatValue(long raw) {
        return intBitsToFloat((int) raw);
    }

    public void set(int intValue, float floatValue) {
        set(raw(intValue, floatValue));
    }

    public static long raw(int intValue, float floatValue) {
        return (intValue & 0xFFFFFFFFL) << 32 | floatToIntBits(floatValue) & 0xFFFFFFFFL;
    }

    @Override
    public String toString() {
        long raw = get();
        return "[" + getIntValue(raw) + "," + getFloatValue(raw) + "]";
    }
}
