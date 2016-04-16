package edu.sdsu.rocket.core.helpers;

public class AtomicIntDouble {

    public class IntDoubleValuePair {
        public final int intValue;
        public final double doubleValue;
        public IntDoubleValuePair(int intValue, double doubleValue) {
            this.intValue = intValue;
            this.doubleValue = doubleValue;
        }
    }

    private final Object lock = new Object();

    private int intValue;
    private double doubleValue;

    public void set(int intValue, double doubleValue) {
        synchronized (lock) {
            this.intValue = intValue;
            this.doubleValue = doubleValue;
        }
    }

    public IntDoubleValuePair get() {
        synchronized (lock) {
            return new IntDoubleValuePair(this.intValue, this.doubleValue);
        }
    }

}
