package edu.sdsu.rocket.log2csv;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;

import au.com.bytecode.opencsv.CSVWriter;
import edu.sdsu.rocket.core.io.ADS1115InputStream;
import edu.sdsu.rocket.core.io.ADS1115InputStream.ADS1115Reading;
import edu.sdsu.rocket.core.io.ADXL345InputStream;
import edu.sdsu.rocket.core.io.ADXL345InputStream.ADXL345Reading;
import edu.sdsu.rocket.core.io.HMC5883LInputStream;
import edu.sdsu.rocket.core.io.HMC5883LInputStream.HMC5883LReading;
import edu.sdsu.rocket.core.io.ITG3205InputStream;
import edu.sdsu.rocket.core.io.ITG3205InputStream.ITG3205Reading;
import edu.sdsu.rocket.core.io.MS5611InputStream;
import edu.sdsu.rocket.core.io.MS5611InputStream.MS5611Reading;

public class Converter {

	private File location;

	public Converter(File location) {
		this.location = location;
	}
	
	public void convert() {
		System.out.print("Converting ADC (ADS1115) ... ");
		try {
			convertADS1115();
			System.out.println("Done");
		} catch (IOException e) {
			System.err.println("ADS1115: " + e);
		}
		
		System.out.print("Converting Accelerometer (ADXL345) ... ");
		try {
			convertADXL345();
			System.out.println("Done");
		} catch (IOException e) {
			System.err.println("ADXL345: " + e);
		}
		
		System.out.print("Converting Gyroscope (ITG3205) ... ");
		try {
			convertITG3205();
			System.out.println("Done");
		} catch (IOException e) {
			System.err.println("ITG3205: " + e);
		}
		
		System.out.print("Converting Magnetometer (HMC5883L) ... ");
		try {
			convertHMC5883L();
			System.out.println("Done");
		} catch (IOException e) {
			System.err.println("HMC5883L: " + e);
		}
		
		System.out.print("Converting Barometer (MS5611) ... ");
		try {
			convertMS5611();
			System.out.println("Done");
		} catch (IOException e) {
			System.err.println("MS5611: " + e);
		}
	}

	public void convertADS1115() throws IOException {
		String name = "ads1115";
		System.out.println(location + File.separator + name + ".log");
		ADS1115InputStream in = new ADS1115InputStream(new FileInputStream(location + File.separator + name + ".log"));
		CSVWriter writer = new CSVWriter(new FileWriter(location + File.separator + name + ".csv"), CSVWriter.DEFAULT_SEPARATOR, CSVWriter.NO_QUOTE_CHARACTER);
		
		// Timestamp (ns), A0 (mV), A1 (mV), A2 (mV), A3 (mV)
		writer.writeNext("Timestamp", "A0", "A1", "A2", "A3");
		try {
			ADS1115Reading reading;
			while ((reading = in.readReading()) != null) {
				String[] entries = new String[5];
				entries[0] = String.valueOf(reading.timestamp);
				entries[1] = Float.isNaN(reading.values[0]) ? "" : String.valueOf(reading.values[0]); // A0
				entries[2] = Float.isNaN(reading.values[1]) ? "" : String.valueOf(reading.values[1]); // A1
				entries[3] = Float.isNaN(reading.values[2]) ? "" : String.valueOf(reading.values[2]); // A2
				entries[4] = Float.isNaN(reading.values[3]) ? "" : String.valueOf(reading.values[3]); // A3
				writer.writeNext(entries);
			}
		} finally {
			try {
				in.close();
			} catch (IOException e) {
				System.err.println("Failed to close " + name);
			}
			writer.close();
		}
	}
	
	public void convertADXL345() throws IOException {
		String name = "adxl345";
		System.out.println(location + File.separator + name + ".log");
		ADXL345InputStream in = new ADXL345InputStream(new FileInputStream(location + File.separator + name + ".log"));
		CSVWriter writer = new CSVWriter(new FileWriter(location + File.separator + name + ".csv"), CSVWriter.DEFAULT_SEPARATOR, CSVWriter.NO_QUOTE_CHARACTER);
		
		// Timestamp (ns), Multiplier, X*Multiplier (G), Y*Multiplier (G), Z*Multiplier (G)
		writer.writeNext("Timestamp", "Multiplier", "X", "Y", "Z");
		try {
			ADXL345Reading reading;
			while ((reading = in.readReading()) != null) {
				String[] entries = new String[5];
				entries[0] = String.valueOf(reading.timestamp);
				entries[1] = String.valueOf(reading.scalingFactor);
				entries[2] = String.valueOf(reading.values[0]); // x
				entries[3] = String.valueOf(reading.values[1]); // y
				entries[4] = String.valueOf(reading.values[2]); // z
				writer.writeNext(entries);
			}
		} finally {
			try {
				in.close();
			} catch (IOException e) {
				System.err.println("Failed to close " + name);
			}
			writer.close();
		}
	}
	
	public void convertITG3205() throws IOException {
		String name = "itg3205";
		System.out.println(location + File.separator + name + ".log");
		ITG3205InputStream in = new ITG3205InputStream(new FileInputStream(location + File.separator + name + ".log"));
		CSVWriter writer = new CSVWriter(new FileWriter(location + File.separator + name + ".csv"), CSVWriter.DEFAULT_SEPARATOR, CSVWriter.NO_QUOTE_CHARACTER);
		
		// Timestamp (ns), Multiplier, X*Multiplier (deg/s), Y*Multiplier (deg/s), Z*Multiplier (deg/s)
		writer.writeNext("Timestamp", "Multiplier", "X", "Y", "Z");
		try {
			ITG3205Reading reading;
			while ((reading = in.readReading()) != null) {
				String[] entries = new String[5];
				entries[0] = String.valueOf(reading.timestamp);
				entries[1] = String.valueOf(reading.scalingFactor);
				entries[2] = String.valueOf(reading.values[0]); // x
				entries[3] = String.valueOf(reading.values[1]); // y
				entries[4] = String.valueOf(reading.values[2]); // z
				writer.writeNext(entries);
			}
		} finally {
			try {
				in.close();
			} catch (IOException e) {
				System.err.println("Failed to close " + name);
			}
			writer.close();
		}
	}
	
	public void convertHMC5883L() throws IOException {
		String name = "hmc5883l";
		System.out.println(location + File.separator + name + ".log");
		HMC5883LInputStream in = new HMC5883LInputStream(new FileInputStream(location + File.separator + name + ".log"));
		CSVWriter writer = new CSVWriter(new FileWriter(location + File.separator + name + ".csv"), CSVWriter.DEFAULT_SEPARATOR, CSVWriter.NO_QUOTE_CHARACTER);
		
		// Timestamp (ns), Multiplier, X*Multiplier (Gauss), Y*Multiplier (Gauss), Z*Multiplier (Gauss)
		writer.writeNext("Timestamp", "Multiplier", "X", "Y", "Z");
		try {
			HMC5883LReading reading;
			while ((reading = in.readReading()) != null) {
				String[] entries = new String[5];
				entries[0] = String.valueOf(reading.timestamp);
				entries[1] = String.valueOf(reading.scalingFactor);
				entries[2] = String.valueOf(reading.values[0]); // x
				entries[3] = String.valueOf(reading.values[1]); // y
				entries[4] = String.valueOf(reading.values[2]); // z
				writer.writeNext(entries);
			}
		} finally {
			try {
				in.close();
			} catch (IOException e) {
				System.err.println("Failed to close " + name);
			}
			writer.close();
		}
	}
	
	public void convertMS5611() throws IOException {
		String name = "ms5611";
		System.out.println(location + File.separator + name + ".log");
		MS5611InputStream in = new MS5611InputStream(new FileInputStream(location + File.separator + name + ".log"));
		CSVWriter writer = new CSVWriter(new FileWriter(location + File.separator + name + ".csv"), CSVWriter.DEFAULT_SEPARATOR, CSVWriter.NO_QUOTE_CHARACTER);
		
		// Timestamp (ns), Temperature*100 (C), Pressure*100 (mbar)
		writer.writeNext("Timestamp", "Temperature", "Pressure");
		try {
			MS5611Reading reading;
			while ((reading = in.readReading()) != null) {
				String[] entries = new String[3];
				entries[0] = String.valueOf(reading.timestamp);
				entries[1] = String.valueOf(reading.values[0]); // C * 100
				entries[2] = String.valueOf(reading.values[1]); // mbar * 100
				writer.writeNext(entries);
			}
		} finally {
			try {
				in.close();
			} catch (IOException e) {
				System.err.println("Failed to close " + name);
			}
			writer.close();
		}
	}
	
}
