package edu.sdsu.rocket.server;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Config {

    public boolean help;
    public boolean test;
    public boolean debug;
    public boolean allowNoLogs;
    public final List<File> logDirs = new ArrayList<>();

    public Config(String[] args) throws FileNotFoundException {
        for (String arg : args) {
            if (arg.startsWith("--")) {
                if (arg.equalsIgnoreCase("--help")) {
                    help = true;
                } else if (arg.equalsIgnoreCase("--test")) {
                    test = true;
                } else if (arg.equalsIgnoreCase("--debug")) {
                    debug = true;
                } else if (arg.equalsIgnoreCase("--allow-no-logs")) {
                    allowNoLogs = true;
                }
            } else {
                File dir = new File(arg);
                if (!dir.exists()) {
                    throw new FileNotFoundException(dir.getAbsolutePath());
                }
                logDirs.add(dir);
            }
        }
    }

    @Override
    public String toString() {
        return super.toString() + ": "
                + "help=" + help
                + ", test=" + test
                + ", debug=" + debug
                + ", allow-no-logs=" + allowNoLogs
                + ", logDirs=" + Arrays.toString(logDirs.toArray());
    }
}
