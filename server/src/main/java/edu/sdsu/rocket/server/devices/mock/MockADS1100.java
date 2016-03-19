package edu.sdsu.rocket.server.devices.mock;

import com.badlogic.gdx.math.MathUtils;
import edu.sdsu.rocket.server.devices.ADS1100;

import java.io.IOException;

public class MockADS1100 extends ADS1100 {

	private static final int RANDOM_MIN = 0;
	private static final int RANDOM_MAX = 10;
	private float x;

    public MockADS1100() {
        super(null);
    }

    @Override
	public ADS1100 setup() throws IOException {
		x = (int)(Math.random() * (RANDOM_MAX - RANDOM_MIN) + RANDOM_MIN);
		return this;
	}

    @Override
    public ADS1100 writeConfig() throws IOException {
        return this;
    }

    @Override
	public boolean isPerformingConversion() throws IOException {
		return false;
	}

	public float readVoltageMock() throws IOException, InterruptedException {
        Thread.sleep(1L);

        x += 0.01f;
        if (x > 1000f) x = 0f;

		float s = MathUtils.sin(x); // -1 to 1
		float sp = (s / 2f) + 0.5f; // 0 to 1
		return sp * getSupplyVoltage();
	}
	
	@Override
	public void loop() throws IOException, InterruptedException {
        float value = readVoltageMock();

        if (listener != null) {
            listener.onValue(value);
        }
	}

}
