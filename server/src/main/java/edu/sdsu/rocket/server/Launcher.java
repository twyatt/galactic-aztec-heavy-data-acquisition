package edu.sdsu.rocket.server;

import com.pi4j.io.i2c.I2CFactory;
import edu.sdsu.rocket.core.BuildConfig;

import java.io.IOException;

public class Launcher {

    public static void main(String[] args) throws Exception {
        final Config config = new Config(args);

        if (config.help || args.length == 0) {
            printUsage();
            System.exit(0);
        }

        if (config.version) {
            System.out.println(BuildConfig.VERSION);
            System.exit(0);
        }

        if (config.test) {
            for (int i = 0; i < 3; i++) {
                System.out.println("!!! TESTING MODE !!! TESTING MODE !!! TESTING MODE !!!");
                Thread.sleep(1000L);
            }
        } else {
            if (config.logDirs.size() == 0 && !config.allowNoLogs) {
                System.err.println("Error: No log directories specified. Use --allow-no-logs to disable this check.");
                System.exit(1);
            }
        }

        System.out.println("Starting application (" + BuildConfig.VERSION + ")");
        start(config);
    }

    private static void start(Config config) throws IOException, InterruptedException, I2CFactory.UnsupportedBusNumberException {
        if (config.debug) {
            System.out.println(config);
        }

        final Application app = new Application(config);
        app.setup();
        while (true) {
            app.loop();
        }
    }

    private static void printUsage() {
        System.out.println("usage: server [OPTIONS] <LOG DIRECTORY> [<LOG DIRECTORY> ...]");
        System.out.println();
        System.out.println("  --version                 Display version and quit");
        System.out.println("  --test                    Enable testing mode");
        System.out.println("  --debug                   Enable debug mode");
        System.out.println("  --disable-system-status   Disable reading of system status (e.g. CPU temperature)");
        System.out.println("  --allow-no-logs           Allow application to run without logging");
        System.out.println();
    }

}
