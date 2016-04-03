package edu.sdsu.rocket.core.io.devices;

import edu.sdsu.rocket.core.io.TimestampInputStream;

import java.io.IOException;
import java.io.InputStream;

public class ADS1100InputStream extends TimestampInputStream {

    public class Config {
        public int config;
        public float Vdd;
    }

    public class Reading {
        public long timestamp;
        public float value;
    }

    public ADS1100InputStream(InputStream in) {
        super(in);
    }

    public Config readConfig() throws IOException {
        Config config = new Config();
        readConfig(config);
        return config;
    }

    private void readConfig(Config out) throws IOException {
        out.config = readInt();
        out.Vdd = readFloat();
    }

    public Reading readValue() throws IOException {
        Reading reading = new Reading();
        readValue(reading);
        return reading;
    }

    public void readValue(Reading out) throws IOException {
        out.timestamp = readTimestamp();
        out.value = readFloat();
    }

}
