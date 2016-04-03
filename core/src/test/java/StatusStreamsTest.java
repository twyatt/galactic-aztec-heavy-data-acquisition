import edu.sdsu.rocket.core.io.StatusInputStream;
import edu.sdsu.rocket.core.io.StatusOutputStream;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class StatusStreamsTest {

    @Test
    public void ioTest() throws IOException {
        int[] values = new int[]{
                1123,
                0,
                -455,
                2836
        };
        long[] timestamps = new long[values.length];

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        StatusOutputStream out = new StatusOutputStream(baos);

        // write values
        for (int i = 0; i < values.length; i++) {
            int value = values[i];
            timestamps[i] = out.writeValue(value);
        }

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        StatusInputStream in = new StatusInputStream(bais);

        // read values
        for (int i = 0; i < values.length; i++) {
            long timestamp = timestamps[i];
            int value = values[i];
            StatusInputStream.Reading reading = in.readValue();
            assertEquals(timestamp, reading.timestamp);
            assertEquals(value, reading.value);
        }
    }

}
