package edu.sdsu.rocket.gateway;

import edu.sdsu.rocket.core.BuildConfig;

import java.io.IOException;

public class Launcher {

    public static void main(String[] args) throws Exception {
        if (args.length == 2) {
            System.out.println("Starting application (" + BuildConfig.VERSION + ")");
            start(args[0], args[1]);
        } else if (args.length == 1) {
            System.out.println("Starting application (" + BuildConfig.VERSION + ")");
            start(args[0], null);
        } else {
            printUsage();
            System.exit(0);
        }
    }

    private static void start(String analogHost, String phidgetsHost) throws IOException {
        final Application app = new Application();
        app.setup(analogHost, phidgetsHost);
        while (true) {
            app.loop();
        }
    }

    private static void printUsage() {
        System.out.println("usage: server <ANALOG HOST> [<PHIDGETS HOST>]");
        System.out.println();
    }

}
