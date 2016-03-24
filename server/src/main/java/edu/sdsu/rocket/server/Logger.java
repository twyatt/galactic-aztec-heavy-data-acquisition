package edu.sdsu.rocket.server;

import edu.sdsu.rocket.core.io.OutputStreamMultiplexer;

import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Logger {

    private static final String FILE_SEPARATOR = System.getProperty("file.separator");
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMddHHmmss");

    private final List<File> directories;
    private final List<OutputStream> streams = new ArrayList<>();

    public Logger(List<File> directories) {
        this.directories = appendTimestampDirectory(directories);
    }

    private static List<File> appendTimestampDirectory(List<File> directories) {
        String timestamp = DATE_FORMAT.format(new Date());

        final List<File> timestampDirectories = new ArrayList<>();
        for (File dir : directories) {
            File d = new File(dir + FILE_SEPARATOR + timestamp);
            if (!d.exists()) {
                System.out.println("mkdir -p " + d);
                if (!d.mkdirs()) {
                    throw new RuntimeException("Failed to create directory: " + d);
                }
            }
            timestampDirectories.add(d);
        }
        return timestampDirectories;
    }

    public OutputStream create(String filename) throws FileNotFoundException {
        final OutputStreamMultiplexer multiplexer = new OutputStreamMultiplexer();
        for (File directory : directories) {
            File f = new File(directory + FILE_SEPARATOR + filename);
            multiplexer.add(new FileOutputStream(f));
        }
        streams.add(multiplexer);
        return multiplexer;
    }

    public void close() {
        for (OutputStream stream : streams) {
            try {
                stream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
