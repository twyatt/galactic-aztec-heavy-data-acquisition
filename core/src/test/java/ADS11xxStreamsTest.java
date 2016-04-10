import edu.sdsu.rocket.core.io.devices.ADS11xxInputStream;
import edu.sdsu.rocket.core.io.devices.ADS11xxOutputStream;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class ADS11xxStreamsTest {

    private static final float DELTA = 0.00001f;

    @Test
    public void ioTest() throws IOException {
        long[] timestamps = new long[]  {           5L,       32947L, 32948237443L,    928472384747L };
        float[] values    = new float[] { 0.123456789f, 1.234567890f,        1000f, 10000.123456789f };

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ADS11xxOutputStream out = new ADS11xxOutputStream(baos);

        // write config
//        int writeConfig = 0b1000_1100; // ADS1100 default
        int writeConfig = 0b1000_0101_1000_0011; // ADS1114 default
        out.writeConfig(writeConfig);

        // write values
        for (int i = 0; i < values.length; i++) {
            long timestamp = timestamps[i];
            float value = values[i];
            out.writeValue(timestamp, value);
        }

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        ADS11xxInputStream in = new ADS11xxInputStream(bais);

        // read config
        int readConfig = in.readConfig();
        assertEquals(writeConfig, readConfig);

        // read values
        for (int i = 0; i < values.length; i++) {
            long timestamp = timestamps[i];
            float value = values[i];
            ADS11xxInputStream.Reading reading = in.readValue();
            assertEquals(timestamp, reading.timestamp);
            assertEquals(value, reading.value, DELTA);
        }
    }

}
