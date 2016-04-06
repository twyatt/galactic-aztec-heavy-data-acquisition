package edu.sdsu.rocket.server.devices.mock;

import java.io.IOException;

import edu.sdsu.rocket.server.devices.ADS1115;

public class MockADS1115 extends ADS1115 {

    private static final int RANDOM_MIN = 0;
    private static final int RANDOM_MAX = 10;
    private float[] x;
    private int channel;

    @Override
    public ADS1115 setup() throws IOException {
        x = new float[] {
            (int)(Math.random() * (RANDOM_MAX - RANDOM_MIN) + RANDOM_MIN),
            (int)(Math.random() * (RANDOM_MAX - RANDOM_MIN) + RANDOM_MIN),
            (int)(Math.random() * (RANDOM_MAX - RANDOM_MIN) + RANDOM_MIN),
            (int)(Math.random() * (RANDOM_MAX - RANDOM_MIN) + RANDOM_MIN),
        };
        return this;
    }

    @Override
    protected void writeRegister(Register register, int value) throws IOException {
        // no-op
    }

    @Override
    protected int readRegister(Register register) throws IOException {
        return 0; // no-op
    }

    @Override
    public boolean isPerformingConversion() throws IOException {
        return false;
    }
    
    @Override
    public ADS1115 setSingleEnded(int channel) {
        this.channel = channel;
        return super.setSingleEnded(channel);
    }

    @Override
    public float readMillivolts() throws IOException {
        if ((x[channel] += 0.001f) > 1000f) x[channel] = 0f; // 0 to 1000
        float s = (float) Math.sin(x[channel]); // -1 to 1
        float sp = (s / 2f) + 0.5f; // 0 to 1
        return sp * 5000f;
    }

}
