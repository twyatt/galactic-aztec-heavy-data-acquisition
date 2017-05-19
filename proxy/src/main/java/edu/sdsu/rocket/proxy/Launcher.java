package edu.sdsu.rocket.proxy;

import edu.sdsu.rocket.core.BuildConfig;

import java.io.IOException;

public class Launcher {

    public static void main(String[] args) throws Exception {
        final Config config = new Config(args);

        if (config.help) {
            printUsage();
            System.exit(0);
        }

        if (config.version) {
            System.out.println(BuildConfig.VERSION);
            System.exit(0);
        }

        System.out.println("Starting application (" + BuildConfig.VERSION + ")");
        start(config);
    }

    private static void start(Config config) throws IOException, InterruptedException {
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
        System.out.println("usage: proxy [OPTIONS]");
        System.out.println();
        System.out.println("  --version                 Display version and quit");
        System.out.println("  --debug                   Enable debug mode");
        System.out.println();
    }

}
