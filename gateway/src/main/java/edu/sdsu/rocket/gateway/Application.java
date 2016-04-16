package edu.sdsu.rocket.gateway;

import edu.sdsu.rocket.core.helpers.AtomicIntFloat;
import edu.sdsu.rocket.core.helpers.PressureValueTranslatorFactory;
import edu.sdsu.rocket.core.helpers.ValueTranslator;
import edu.sdsu.rocket.core.models.Sensors;
import edu.sdsu.rocket.core.net.SensorClient;
import fi.iki.elonen.NanoHTTPD;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class Application {

    private final Reader input = new InputStreamReader(System.in);

    private static final int PORT = 4444;
    private final Sensors sensors = new Sensors();
    private final SensorClient analogClient = new SensorClient(sensors, Sensors.ANALOG_MASK);
//    private final SensorClient phidgetsClient = new SensorClient(sensors);

    private NanoHTTPD httpd;

    private static final ValueTranslator[] TRANSLATORS = {
        PressureValueTranslatorFactory.getLOX(),
        PressureValueTranslatorFactory.getKerosene(),
        PressureValueTranslatorFactory.getHelium(),
        PressureValueTranslatorFactory.getMotor(),
        PressureValueTranslatorFactory.getRcsLow(),
        PressureValueTranslatorFactory.getRcsHigh(),
    };

    private final AtomicLong latency = new AtomicLong();

    public void setup(String analogHost, String phidgetsHost) throws IOException {
        setupServer();

        setupAnalogClient(analogHost);
        if (phidgetsHost != null) {
            setupPhidgetsClient(phidgetsHost);
        }
    }

    private void setupServer() throws IOException {
        httpd = new NanoHTTPD(8080) {
            @Override
            public Response serve(IHTTPSession session) {
                Response.Status status = Response.Status.OK;
                String mimeType = NanoHTTPD.MIME_HTML;

                String response;
                if (session.getUri().equals("/json")) {
                    mimeType = "text/json";
                    response = getJson();
                } else {
                    String uri = session.getUri();
                    if (uri.equals("/")) {
                        uri = "/index.html";
                    }

                    try {
                        response = getFile(uri);
                    } catch (Exception e) {
//                        e.printStackTrace();
                        status = Response.Status.BAD_REQUEST;
                        response = "Bad request<br/><br/>" + e;
                    }
                }

                return newFixedLengthResponse(status, mimeType, response);
            }
        };
        httpd.start();
    }

    private String getFile(String file) throws URISyntaxException, IOException {
        URL url = Application.class.getResource(file);
        if (url == null) {
            throw new IOException("File not found: " + file);
        }
        Path path = Paths.get(url.toURI());
        return new String(Files.readAllBytes(path), "UTF8");
    }

    private String getJson() {
        String json = "[";
        for (int i = 0; i < sensors.analog.length; i++) {
            long raw = sensors.analog[i].get();
            float value = AtomicIntFloat.getFloatValue(raw);

            ValueTranslator translator = TRANSLATORS[i];
            float pressure = translator.translate(value);

            if (i != 0) {
                json += ",";
            }
            json += String.valueOf(pressure);
        }
        json += "]";
        return json;
    }

    private void setupAnalogClient(String host) throws UnknownHostException, SocketException {
        analogClient.setListener(new SensorClient.SensorClientListenerAdapter() {
            @Override
            public void onPingResponse(long latency) {
                Application.this.latency.set(latency);
            }
        });
        analogClient.setFrequency(2); // Hz

        InetAddress addr = InetAddress.getByName(host);
        analogClient.start(addr, PORT);
    }

    private void setupPhidgetsClient(String host) throws UnknownHostException, SocketException {
//        phidgetsClient.setListener(new SensorClient.SensorClientListenerAdapter() {
//            @Override
//            public void onPingResponse(long latency) {
//                Application.this.latency.set(latency);
//            }
//        });
//        phidgetsClient.setFrequency(2); // Hz
//
//        InetAddress addr = InetAddress.getByName(host);
//        phidgetsClient.start(addr, PORT);
    }

    public void loop() throws IOException {
        switch (input.read()) {
            case '?':
                System.out.println();
                System.out.println("?: help");
                System.out.println("l: latency");
                System.out.println("a: analog");
                System.out.println("q: quit");
                System.out.println();
                break;
            case 'l':
                System.out.println("Latency: " + TimeUnit.NANOSECONDS.toMillis(latency.get()) + " ms");
                break;
            case 'q':
                shutdown();
                break;
        }
    }

    private void shutdown() {
        System.out.println("Shutting down");

        System.out.println("Stopping analog client");
        analogClient.stop();

//        if (phidgetsClient != null) {
//            System.out.println("Stopping phidgets client");
//            phidgetsClient.stop();
//        }

        System.out.println("Stopping server");
        httpd.stop();

        System.exit(0);
    }

}
