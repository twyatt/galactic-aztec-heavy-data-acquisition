package edu.sdsu.rocket.log2csv;

import java.io.File;

public class Launcher {
	
//	private static final String NAME = System.getProperty("program.name");
	private static final String NAME = "log2csv";

	public static void main(String[] args) {
		if (args.length == 0) {
			usage();
			System.exit(1);
		}

		final File folder;
		final boolean skipConfig;
		if (args.length == 2 && args[0].equalsIgnoreCase("--skip-config")) {
			skipConfig = true;
			folder = new File(args[1]);
		} else {
			folder = new File(args[0]);
			skipConfig = false;
		}
		
		if (!folder.exists()) {
			System.err.println("Location not found: " + folder);
			System.exit(1);
		}
		if (!folder.isDirectory()) {
			usage();
			System.exit(1);
		}
		
		Converter converter = new Converter(folder, skipConfig);
		converter.convert();
	}

	private static void usage() {
		System.out.println("Usage:");
		System.out.println("  " + NAME + " [OPTIONS] FOLDER");
		System.out.println("Options:");
		System.out.println("  --skip-config  Skips reading the sensor config;");
		System.out.println("                 Needed for logs written by the client.");
		System.out.println();
	}
	
}
