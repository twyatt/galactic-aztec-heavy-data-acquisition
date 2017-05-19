package edu.sdsu.rocket.proxy;

import java.io.FileNotFoundException;

public class Config {

    public boolean help;
    public boolean version;
    public boolean debug;

    public Config(String[] args) throws FileNotFoundException {
        for (String arg : args) {
            if (arg.startsWith("--")) {
                if (arg.equalsIgnoreCase("--help")) {
                    help = true;
                } else if (arg.equalsIgnoreCase("--version")) {
                    version = true;
                } else if (arg.equalsIgnoreCase("--debug")) {
                    debug = true;
                }
            } else {
                System.err.println("Unknown argument: " + arg);
                System.exit(1);
            }
        }
    }

    @Override
    public String toString() {
        return super.toString() + ": "
                + "help=" + help
                + ", version=" + version
                + ", debug=" + debug;
    }

}
