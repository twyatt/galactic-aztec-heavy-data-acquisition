import edu.sdsu.rocket.core.io.devices.ADS1100InputStream;
import edu.sdsu.rocket.core.io.devices.ADS1100OutputStream;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class ADS1100StreamsTest {

    private static final float DELTA = 0.00001f;

    @Test
    public void ioTest() throws IOException {
        long[] timestamps = new long[]  {           5L,       32947L, 32948237443L,    928472384747L };
        float[] values    = new float[] { 0.123456789f, 1.234567890f,        1000f, 10000.123456789f };

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ADS1100OutputStream out = new ADS1100OutputStream(baos);

        // write config
        int configDefault = 0b1000_1100;
        float Vdd = 3.3f;
        out.writeConfig(configDefault, Vdd);

        // write values
        for (int i = 0; i < values.length; i++) {
            long timestamp = timestamps[i];
            float value = values[i];
            out.writeValue(timestamp, value);
        }

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        ADS1100InputStream in = new ADS1100InputStream(bais);

        // read config
        ADS1100InputStream.Config config = in.readConfig();
        assertEquals(configDefault, config.config);
        assertEquals(Vdd, config.Vdd, DELTA);

        // read values
        for (int i = 0; i < values.length; i++) {
            long timestamp = timestamps[i];
            float value = values[i];
            ADS1100InputStream.Reading reading = in.readValue();
            assertEquals(timestamp, reading.timestamp);
            assertEquals(value, reading.value, DELTA);
        }
    }

}
