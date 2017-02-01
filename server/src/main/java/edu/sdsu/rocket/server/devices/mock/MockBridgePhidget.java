package edu.sdsu.rocket.server.devices.mock;

import com.phidgets.event.BridgeDataEvent;
import com.phidgets.event.BridgeDataListener;
import edu.sdsu.rocket.server.devices.DeviceManager;

import java.io.IOException;

public class MockBridgePhidget implements DeviceManager.Device {

    private float x;

    private BridgeDataListener bridgeDataListener;

    public void addBridgeDataListener(BridgeDataListener bridgeDataListener) {
        this.bridgeDataListener = bridgeDataListener;
    }

    @Override
    public void loop() throws IOException, InterruptedException {
        if ((x += 0.01f) > 1000f) x = 0f; // 0 to 1000
        float s = (float) Math.sin(x); // -1 to 1
        float sp = (s / 2f) + 0.5f; // 0 to 1
        float ratedOutput = sp * 2.3176f; // 0 to 2.3176 (which is full calibrated mV/V range of Futek load cell)

        if (bridgeDataListener != null) {
            BridgeDataEvent event = new BridgeDataEvent(null, 0, ratedOutput);
            bridgeDataListener.bridgeData(event);
        }
    }

}
