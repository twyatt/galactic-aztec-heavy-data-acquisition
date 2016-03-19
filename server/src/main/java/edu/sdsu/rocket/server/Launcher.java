package edu.sdsu.rocket.server;

import java.io.IOException;

public class Launcher {

    public static void main(String[] args) throws Exception {
        final Config config = new Config(args);

        if (config.help) {
            System.out.println("usage: server [OPTIONS] [LOG DIRECTORY] [LOG DIRECTORY...]");
            System.out.println("--test   Enabled testing mode");
            System.out.println("--debug  Enabled debug mode");
            System.exit(0);
        }

        if (config.test) {
            for (int i = 0; i < 3; i++) {
                System.out.println("!!! TESTING MODE !!! TESTING MODE !!! TESTING MODE !!!");
                Thread.sleep(1000L);
            }
        }

        System.out.println("Starting application.");
        start(config);
    }

    private static void start(Config config) throws IOException, InterruptedException {
        System.out.println(config);

        final Application app = new Application(config);
        app.setup();
        while (true) {
            app.loop();
        }
    }

}