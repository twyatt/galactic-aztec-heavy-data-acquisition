package edu.sdsu.rocket.log2csv;

import au.com.bytecode.opencsv.CSVWriter;
import edu.sdsu.rocket.core.io.devices.ADS1100InputStream;
import edu.sdsu.rocket.core.io.devices.ADS1114InputStream;
import edu.sdsu.rocket.core.io.devices.ADS1115InputStream;

import java.io.*;

public class Converter {

    private File location;

    public Converter(File location) {
        this.location = location;
    }
    
    public void convert() {
        for (int i = 0; i < 3; i++) {
            String name = "ADS1114-A" + i;
            System.out.print("Converting ADC (" + name + ") ... ");
            try {
                convertADS1114(name);
                System.out.println("Done");
            } catch (IOException e) {
                System.err.println(name + ": " + e);
            }
        }

        for (int i = 4; i < 5; i++) {
            String name = "ADS1100-A" + i;
            System.out.print("Converting ADC (" + name + ") ... ");
            try {
                convertADS1100(name);
                System.out.println("Done");
            } catch (IOException e) {
                System.err.println(name + ": " + e);
            }
        }
    }

    private void convertADS1114(String name) throws IOException {
        String logFilename = name + ".log";
        String csvFilename = name + ".csv";
        String logPath = location + File.separator + logFilename;
        String csvPath = location + File.separator + csvFilename;

        System.out.println(logPath + " => " + csvPath);

        ADS1115InputStream in = new ADS1115InputStream(new FileInputStream(logPath));
        FileWriter fileWriter = new FileWriter(csvPath);
        CSVWriter csv = new CSVWriter(fileWriter, CSVWriter.DEFAULT_SEPARATOR, CSVWriter.NO_QUOTE_CHARACTER);

        try {
            csv.writeNext("Nanoseconds", "Millivolts");
            ADS1114InputStream.Reading reading;
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

    private void convertADS1100(String name) throws IOException {
        String logFilename = name + ".log";
        String csvFilename = name + ".csv";
        String logPath = location + File.separator + logFilename;
        String csvPath = location + File.separator + csvFilename;

        System.out.println(logPath + " => " + csvPath);

        ADS1100InputStream in = new ADS1100InputStream(new FileInputStream(logPath));
        FileWriter fileWriter = new FileWriter(csvPath);
        CSVWriter csv = new CSVWriter(fileWriter, CSVWriter.DEFAULT_SEPARATOR, CSVWriter.NO_QUOTE_CHARACTER);

        try {
            csv.writeNext("Nanoseconds", "Millivolts");
            ADS1100InputStream.Reading reading;
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
