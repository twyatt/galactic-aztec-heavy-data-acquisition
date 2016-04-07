import edu.sdsu.rocket.core.helpers.AtomicIntFloat;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static java.lang.Float.floatToIntBits;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AtomicIntFloatTest {

    @Test
    public void fullIntRangeTest() {
        final AtomicIntFloat atomic = new AtomicIntFloat();
        final int INCREMENT = 10000;
        for (long i = Integer.MIN_VALUE; i <= Integer.MAX_VALUE; i += INCREMENT) {
            atomic.set((int) i, i);

            final long raw = atomic.get();
            final int intValue = AtomicIntFloat.getIntValue(raw);
            final float floatValue = AtomicIntFloat.getFloatValue(raw);

            assertEquals(i, intValue);
            assertEquals((float) i, floatValue, 0.00001f);
        }
    }

    /**
     * Test using a nanosecond timestamp that is converted to milliseconds, since nanoseconds would overflow an integer
     * at greater than ~2.147 seconds; so we accept the loss in precision so we can store a timestamp of up to ~24 days.
     */
    @Test
    public void nanosecondTimestampTest1() {
        final long nanoseconds = 2_147_040_000_000_000L; // 24.85 days
        final long milliseconds = TimeUnit.NANOSECONDS.toMillis(nanoseconds);
        final float floatValue = 1234.56789f;

        final AtomicIntFloat atomic = new AtomicIntFloat();
        atomic.set((int) milliseconds, floatValue);

        final long raw = atomic.get();
        assertEquals(milliseconds, AtomicIntFloat.getIntValue(raw));
        assertEquals(nanoseconds, TimeUnit.MILLISECONDS.toNanos(AtomicIntFloat.getIntValue(raw)));
        assertEquals(floatValue, AtomicIntFloat.getFloatValue(raw), 0.000001f);
    }

    /**
     * Test using a nanosecond timestamp that is converted to milliseconds, since nanoseconds would overflow an integer
     * at greater than ~2.147 seconds; so we accept the loss in precision so we can store a timestamp of up to ~24 days.
     */
    @Test
    public void nanosecondTimestampTest2() {
        final long nanosecondsBefore = 2_147_040_934_112_068L;
        final long milliseconds = TimeUnit.NANOSECONDS.toMillis(nanosecondsBefore);
        final float floatValue = 9876.54321f;

        final AtomicIntFloat atomic = new AtomicIntFloat();
        atomic.set((int) milliseconds, floatValue);

        final long raw = atomic.get();
        final int intValue = AtomicIntFloat.getIntValue(raw);
        final long nanosecondsAfter = TimeUnit.MILLISECONDS.toNanos(intValue);
        final long diff = Math.abs(nanosecondsAfter - nanosecondsBefore);
        assertTrue("Before/after timestamps are within 1 millisecond", diff <= TimeUnit.MILLISECONDS.toNanos(1));
        assertEquals(floatValue, AtomicIntFloat.getFloatValue(raw), 0.000001f);
    }

    @Test
    public void millisecondTimestampTest() {
        final int milliseconds = 123;
        final float floatValue = 321f;

        final AtomicIntFloat atomic = new AtomicIntFloat();
        atomic.set(milliseconds, floatValue);

        final long raw = atomic.get();
        assertEquals(milliseconds, AtomicIntFloat.getIntValue(raw));
        assertEquals(floatValue, AtomicIntFloat.getFloatValue(raw), 0.000001f);
    }

    @Test
    public void constructorTest() {
        final int intValue = 123;
        final float floatValue = 321f;
        final AtomicIntFloat atomic = new AtomicIntFloat(intValue, floatValue);

        final long raw = atomic.get();
        assertEquals(intValue, AtomicIntFloat.getIntValue(raw));
        assertEquals(floatValue, AtomicIntFloat.getFloatValue(raw), 0.000001f);
    }

}
