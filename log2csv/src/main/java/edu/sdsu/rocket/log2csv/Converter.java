package edu.sdsu.rocket.log2csv;

import au.com.bytecode.opencsv.CSVWriter;
import edu.sdsu.rocket.core.io.devices.PhidgetBridgeInputStream;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;

public class Converter {

    private File location;
    private final boolean skipConfig;

    public Converter(File location, boolean skipConfig) {
        this.location = location;
        this.skipConfig = skipConfig;
    }
    
    public void convert() {
        String name = "loadcell";
        System.out.print("Converting Phidgets Bridge (" + name + ") ... ");
        try {
            convertPhidgetsBridge(name);
            System.out.println("Done");
        } catch (IOException e) {
            System.err.println(name + ": " + e);
        }
    }

    private void convertPhidgetsBridge(String name) throws IOException {
        String logFilename = name + ".log";
        String csvFilename = name + ".csv";
        String logPath = location + File.separator + logFilename;
        String csvPath = location + File.separator + csvFilename;

        System.out.println(logPath + " => " + csvPath);

        PhidgetBridgeInputStream in = new PhidgetBridgeInputStream(new FileInputStream(logPath));

        if (skipConfig) {
            System.out.println("Config: Skipped");
        } else {
            final PhidgetBridgeInputStream.Config config = in.readConfig();
            System.out.println("config.serialNumber = " + config.serialNumber);
            System.out.println("config.deviceVersion = " + config.deviceVersion);
            System.out.println("config.inputCount = " + config.inputCount);
            System.out.println("config.gain = " + config.gain);
            System.out.println("config.dataRate = " + config.dataRate);
        }

        FileWriter fileWriter = new FileWriter(csvPath);
        CSVWriter csv = new CSVWriter(fileWriter, CSVWriter.DEFAULT_SEPARATOR, CSVWriter.NO_QUOTE_CHARACTER);

        try {
            csv.writeNext("Nanoseconds", "mV/V");
            PhidgetBridgeInputStream.Reading reading;
            while ((reading = in.readValue()) != null) {
                csv.writeNext(String.valueOf(reading.timestamp), String.valueOf(reading.value));
            }
        } finally {
            try {
                in.close();
            } catch (IOException e) {
                System.err.println("Failed to close " + logPath);
            }
            try {
                csv.close();
            } catch (IOException e) {
                System.err.println("Failed to close " + csvPath);
            }
        }
    }

}
