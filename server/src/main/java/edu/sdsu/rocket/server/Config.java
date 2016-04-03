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
    public boolean disableSystemStatus;
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
                } else if (arg.equalsIgnoreCase("--disable-system-status")) {
                    disableSystemStatus = true;
                } else if (arg.equalsIgnoreCase("--allow-no-logs")) {
                    allowNoLogs = true;
                }
            } else {
                logDirs.add(new File(arg));
            }
        }
    }

    @Override
    public String toString() {
        return super.toString() + ": "
                + "help=" + help
                + ", test=" + test
                + ", debug=" + debug
                + ", disable-system-status=" + disableSystemStatus
                + ", allow-no-logs=" + allowNoLogs
                + ", logDirs=" + Arrays.toString(logDirs.toArray());
    }
}
