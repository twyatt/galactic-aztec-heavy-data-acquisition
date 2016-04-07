import edu.sdsu.rocket.core.helpers.Stopwatch;
import edu.sdsu.rocket.core.io.devices.ADS1114InputStream;
import edu.sdsu.rocket.core.io.devices.ADS1114OutputStream;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class ADS1114StreamsTest {

    private static final float DELTA = 0.00001f;

    @Test
    public void ioTest() throws IOException {
        long[] timestamps = new long[]  {         102L,       10238L, 227633L,        83478238L };
        float[] values    = new float[] { 0.123456789f, 1.234567890f,   1000f, 10000.123456789f };

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ADS1114OutputStream out = new ADS1114OutputStream(baos);

        // write config
        int config = 0b1000_0101_1000_0011; // default
        out.writeConfig(config);

        // write values
        for (int i = 0; i < values.length; i++) {
            long timestamp = timestamps[i];
            float value = values[i];
            out.writeValue(timestamp, value);
        }

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        ADS1114InputStream in = new ADS1114InputStream(bais);

        // read config
        assertEquals(config, in.readConfig());

        // read values
        for (int i = 0; i < values.length; i++) {
            long timestamp = timestamps[i];
            float value = values[i];

            ADS1114InputStream.Reading reading = in.readValue();
            assertEquals(timestamp, reading.timestamp);
            assertEquals(value, reading.value, DELTA);
        }
    }

}
