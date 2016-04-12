package edu.sdsu.rocket.core.io.devices;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

public class PhidgetBridgeInputStream extends DataInputStream {

    public class Config {
        public int serialNumber;
        public int deviceVersion;
        public int inputCount;
        public int gain;
        public int dataRate;
    }

    public class Reading {
        public long timestamp;
        public double value;
    }

    public PhidgetBridgeInputStream(InputStream in) {
        super(in);
    }

    public Config readConfig() throws IOException {
        Config config = new Config();
        config.serialNumber = readInt();
        config.deviceVersion = readInt();
        config.inputCount = readInt();
        config.gain = readInt();
        config.dataRate = readInt();
        return config;
    }

    public Reading readValue() throws IOException {
        Reading reading = new Reading();
        readValue(reading);
        return reading;
    }

    public void readValue(Reading out) throws IOException {
        out.timestamp = readLong();
        out.value = readDouble();
    }
}
