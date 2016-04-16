package edu.sdsu.rocket.gateway;

import edu.sdsu.rocket.core.helpers.AtomicIntDouble;
import edu.sdsu.rocket.core.helpers.AtomicIntFloat;
import edu.sdsu.rocket.core.helpers.PressureValueTranslatorFactory;
import edu.sdsu.rocket.core.helpers.ValueTranslator;
import edu.sdsu.rocket.core.models.Sensors;
import edu.sdsu.rocket.core.net.SensorClient;
import fi.iki.elonen.NanoHTTPD;

import java.io.*;
import java.net.*;
import java.util.Arrays;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static fi.iki.elonen.NanoHTTPD.newFixedLengthResponse;

public class Application {

    private final Reader input = new InputStreamReader(System.in);

    private static final int ANALOG_PORT = 4444;
    private static final int PHIDGETS_PORT = 4445;
    private final Sensors sensors = new Sensors();
    private final SensorClient analogClient = new SensorClient(sensors, Sensors.ANALOG_MASK);
    private final SensorClient phidgetsClient = new SensorClient(sensors, Sensors.PHIDGETS_MASK);

    private NanoHTTPD httpd;
    private NanoHTTPD httpd8080;

    private static final ValueTranslator[] TRANSLATORS = {
        PressureValueTranslatorFactory.getLOX(),
        PressureValueTranslatorFactory.getKerosene(),
        PressureValueTranslatorFactory.getHelium(),
        PressureValueTranslatorFactory.getMotor(),
        PressureValueTranslatorFactory.getRcsLow(),
        PressureValueTranslatorFactory.getRcsHigh(),
        new ValueTranslator(5000f / 3f, 0f), // phidgets bridge (load cell)
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
        httpd = new NanoHTTPD(80) {
            @Override
            public Response serve(IHTTPSession session) {
                return getResponse(session);
            }
        };
        httpd.start();

        httpd8080 = new NanoHTTPD(8080) {
            @Override
            public Response serve(IHTTPSession session) {
                return getResponse(session);
            }
        };
        httpd8080.start();
    }

    private NanoHTTPD.Response getResponse(NanoHTTPD.IHTTPSession session) {
        NanoHTTPD.Response.Status status = NanoHTTPD.Response.Status.OK;
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
                status = NanoHTTPD.Response.Status.BAD_REQUEST;
                response = "Bad request<br/><br/>" + e;
            }
        }

        return newFixedLengthResponse(status, mimeType, response);
    }

    private String getFile(String file) throws URISyntaxException, IOException {
        InputStream stream = Application.class.getResourceAsStream(file);
        if (stream == null) {
            throw new IOException("File not found: " + file);
        }
        return stringFromStream(stream, "UTF-8");
    }

    private String stringFromStream(InputStream stream, String charset) {
        Scanner scanner = new Scanner(stream, charset).useDelimiter("\\A");
        return scanner.hasNext() ? scanner.next() : "";
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

        ValueTranslator forceTranslator = TRANSLATORS[TRANSLATORS.length - 1];
        AtomicIntDouble.IntDoubleValuePair raw = sensors.phidgets.get();
        float force = forceTranslator.translate((float) raw.doubleValue);
        json += ",";
        json += String.valueOf(force);

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
        analogClient.start(addr, ANALOG_PORT);
    }

    private void setupPhidgetsClient(String host) throws UnknownHostException, SocketException {
        phidgetsClient.setListener(new SensorClient.SensorClientListenerAdapter() {
            @Override
            public void onPingResponse(long latency) {
                Application.this.latency.set(latency);
            }
        });
        phidgetsClient.setFrequency(2); // Hz

        InetAddress addr = InetAddress.getByName(host);
        phidgetsClient.start(addr, PHIDGETS_PORT);
    }

    public void loop() throws IOException {
        switch (input.read()) {
            case '?':
                System.out.println();
                System.out.println("?: help");
                System.out.println("l: latency");
                System.out.println("a: analog");
                System.out.println("p: pidgets");
                System.out.println("q: quit");
                System.out.println();
                break;
            case 'l':
                System.out.println("Latency: " + TimeUnit.NANOSECONDS.toMillis(latency.get()) + " ms");
                break;
            case 'a':
                String a[] = new String[sensors.analog.length];
                for (int i = 0; i < sensors.analog.length; i++) {
                    long raw = sensors.analog[i].get();
                    a[i] = "A" + i + "=" + AtomicIntFloat.getFloatValue(raw);
                }
                System.out.println(Arrays.toString(a));
                break;
            case 'p':
                System.out.println("Bridge=" + sensors.phidgets.get().doubleValue + " mV/V");
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

        if (phidgetsClient != null) {
            System.out.println("Stopping phidgets client");
            phidgetsClient.stop();
        }

        System.out.println("Stopping server");
        httpd.stop();
        httpd8080.stop();

        System.exit(0);
    }

}
