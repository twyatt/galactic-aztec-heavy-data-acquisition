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
        long[] timestamps = new long[] {   0L, 13L, 1333L, 129383L };
        int[] values      = new int[]  { 1123,   0,  -455,    2836 };

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        StatusOutputStream out = new StatusOutputStream(baos);

        // write values
        for (int i = 0; i < values.length; i++) {
            long timestamp = timestamps[i];
            int value = values[i];
            out.writeValue(timestamp, value);
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
