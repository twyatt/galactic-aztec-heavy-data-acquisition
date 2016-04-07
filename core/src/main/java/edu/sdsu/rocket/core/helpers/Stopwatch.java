package edu.sdsu.rocket.core.helpers;

public class Stopwatch {

    private long start;
    
    public Stopwatch() {
        reset();
    }
    
    public void reset() {
        start = System.nanoTime();
    }

    /**
     * Returns number of nanoseconds since reset() was called.
     * 
     * @return Time elapsed since reset (nanoseconds).
     */
    public long nanoSecondsElapsed() {
        return System.nanoTime() - start;
    }
    
}
