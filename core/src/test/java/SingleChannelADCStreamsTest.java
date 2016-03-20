import edu.sdsu.rocket.core.io.SingleChannelADCInputStream;
import edu.sdsu.rocket.core.io.SingleChannelADCOutputStream;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class SingleChannelADCStreamsTest {

    @Test
    public void ioTest() throws IOException {
        float[] values = new float[]{
                0.123456789f,
                1.234567890f,
                1000f,
                10000.123456789f
        };
        long[] timestamps = new long[values.length];

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        SingleChannelADCOutputStream out = new SingleChannelADCOutputStream(baos);
        for (int i = 0; i < values.length; i++) {
            float value = values[i];
            timestamps[i] = out.writeValue(value);
        }

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        SingleChannelADCInputStream in = new SingleChannelADCInputStream(bais);
        for (int i = 0; i < values.length; i++) {
            long timestamp = timestamps[i];
            float value = values[i];
            SingleChannelADCInputStream.Reading reading = in.readReading();
            assertEquals(timestamp, reading.timestamp);
            assertEquals(value, reading.value, 0.00001f);
        }
    }

}
