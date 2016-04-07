import edu.sdsu.rocket.core.io.devices.ADS1100InputStream;
import edu.sdsu.rocket.core.io.devices.ADS1100OutputStream;
import edu.sdsu.rocket.core.io.devices.ADS1115InputStream;
import edu.sdsu.rocket.core.io.devices.ADS1115OutputStream;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class ADS1115StreamsTest {

    private static final float DELTA = 0.00001f;

    @Test
    public void ioTest() throws IOException {
        long[] timestamps = new long[]  {          23L,       34857L, 12847238L,    838472834734L };
        int[] channels    = new int[]   {            1,            3,         0,                2 };
        float[] values    = new float[] { 0.123456789f, 1.234567890f,     1000f, 10000.123456789f };

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ADS1115OutputStream out = new ADS1115OutputStream(baos);

        // write config
        int config = 0b1000_0101_1000_0011; // default
        out.writeConfig(config);

        // write values
        for (int i = 0; i < values.length; i++) {
            long timestamp = timestamps[i];
            float value = values[i];
            out.writeValue(timestamp, channels[i], value);
        }

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        ADS1115InputStream in = new ADS1115InputStream(bais);

        // read config
        assertEquals(config, in.readConfig());

        // read values
        for (int i = 0; i < values.length; i++) {
            long timestamp = timestamps[i];
            int channel = channels[i];
            float value = values[i];

            ADS1115InputStream.Reading reading = in.readValue();
            assertEquals(timestamp, reading.timestamp);
            assertEquals(channel, reading.channel);
            assertEquals(value, reading.value, DELTA);
        }
    }

}
