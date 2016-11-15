package edu.sdsu.rocket.log2csv;

import au.com.bytecode.opencsv.CSVWriter;
import edu.sdsu.rocket.core.helpers.ByteHelper;
import edu.sdsu.rocket.core.io.devices.ADS11xxInputStream;

import java.io.*;
import java.nio.ByteBuffer;

public class Converter {

    private File location;
    private final boolean skipConfig;

    public Converter(File location, boolean skipConfig) {
        this.location = location;
        this.skipConfig = skipConfig;
    }
    
    public void convert() {
        for (int i = 0; i < 6; i++) {
            String name = "A" + i;
            System.out.print("Converting ADC (" + name + ") ... ");
            try {
                convertADS11xx(name, skipConfig);
                System.out.println("Done");
            } catch (IOException e) {
                System.err.println(name + ": " + e);
            }
        }
    }

    private void convertADS11xx(String name, boolean skipConfig) throws IOException {
        String logFilename = name + ".log";
        String csvFilename = name + ".csv";
        String logPath = location + File.separator + logFilename;
        String csvPath = location + File.separator + csvFilename;

        System.out.println(logPath + " => " + csvPath);

        ADS11xxInputStream in = new ADS11xxInputStream(new FileInputStream(logPath));

        if (skipConfig) {
            System.out.println("Config: Skipped");
        } else {
            final int config = in.readConfig();
            byte[] bytes = ByteBuffer.allocate(4).putInt(config).array();
            System.out.println("Config: " + ByteHelper.bytesToHexString(bytes));
        }

        FileWriter fileWriter = new FileWriter(csvPath);
        CSVWriter csv = new CSVWriter(fileWriter, CSVWriter.DEFAULT_SEPARATOR, CSVWriter.NO_QUOTE_CHARACTER);

        try {
            csv.writeNext("Nanoseconds", "Millivolts");
            ADS11xxInputStream.Reading reading;
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
