import edu.sdsu.rocket.core.io.devices.PhidgetBridgeInputStream;
import edu.sdsu.rocket.core.io.devices.PhidgetBridgeOutputStream;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class PhidgetBridgeStreamsTest {

    private static final double DELTA = 0.00001;

    @Test
    public void ioTest() throws IOException {
        long[] timestamps = new long[]   {           5L,       32947L, 32948237443L,    928472384747L };
        double[] values   = new double[] {  0.123456789,  1.234567890,         1000,  10000.123456789 };

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PhidgetBridgeOutputStream out = new PhidgetBridgeOutputStream(baos);

        // write config
        final int serialNumber = 1;
        final int deviceVersion = 2;
        final int inputCount = 3;
        final int gain = 4;
        final int dataRate = 5;
        out.writeConfig(serialNumber, deviceVersion, inputCount, gain, dataRate);

        // write values
        for (int i = 0; i < values.length; i++) {
            long timestamp = timestamps[i];
            double value = values[i];
            out.writeValue(timestamp, value);
        }

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        PhidgetBridgeInputStream in = new PhidgetBridgeInputStream(bais);

        // read config
        PhidgetBridgeInputStream.Config config = in.readConfig();
        assertEquals(serialNumber, config.serialNumber);
        assertEquals(deviceVersion, config.deviceVersion);
        assertEquals(inputCount, config.inputCount);
        assertEquals(gain, config.gain);
        assertEquals(dataRate, config.dataRate);

        // read values
        for (int i = 0; i < values.length; i++) {
            long timestamp = timestamps[i];
            double value = values[i];
            PhidgetBridgeInputStream.Reading reading = in.readValue();
            assertEquals(timestamp, reading.timestamp);
            assertEquals(value, reading.value, DELTA);
        }
    }

}
