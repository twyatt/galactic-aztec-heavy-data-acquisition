package edu.sdsu.rocket.proxy;

import com.pi4j.io.serial.Serial;
import com.pi4j.io.serial.SerialFactory;
import edu.sdsu.rocket.core.helpers.Stopwatch;
import edu.sdsu.rocket.core.models.Sensors;
import edu.sdsu.rocket.core.net.SensorServer;
import edu.sdsu.rocket.pi.io.radio.APIFrameListener;
import edu.sdsu.rocket.pi.io.radio.XTend900;
import edu.sdsu.rocket.pi.io.radio.XTend900Config;
import edu.sdsu.rocket.pi.io.radio.api.RFModuleStatus;
import edu.sdsu.rocket.pi.io.radio.api.RXPacket;
import edu.sdsu.rocket.pi.io.radio.api.TXStatus;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

public class Application {

    private final Stopwatch STOPWATCH = new Stopwatch();

    private final Config config;
    private final Reader input = new InputStreamReader(System.in);
    private XTend900 radio;

    private final Sensors sensors = new Sensors();
    private final SensorServer server = new SensorServer(sensors);

    public Application(Config config) {
        this.config = config;
    }

    public void setup() throws IOException, InterruptedException {
        setupRadio();
        setupServer(4444);
    }

    protected void setupServer(int port) throws IOException {
        System.out.println("Setup server");
        server.setDebug(config.debug);
        server.start(port);
    }

    public void loop() throws IOException {
        handleInput();
    }

    private void handleInput() throws IOException {
        switch (input.read()) {
            case '?':
                System.out.println();
                System.out.println("?: help");
                System.out.println("e: runtime");
                System.out.println("d: data");
                if (radio != null) {
                    System.out.println("r: toggle radio power (currently " + (radio.isOn() ? "ON" : "OFF") + ")");
                }
                System.out.println("q: quit");
                System.out.println();
                break;

            case 'e':
                System.out.println("Runtime: " + nanosToDHMS(STOPWATCH.nanoSecondsElapsed()));
                break;

            case 'd':
                StringBuilder sb = new StringBuilder();
                sb.append("Data: ").append(sensors.proxy);
                for (int i = 0; i < sensors.analog.length; i++) {
                    sb.append(", ").append(sensors.analog[i]);
                }
                System.out.println(sb.toString());
                break;

            case 'r':
                if (radio != null) {
                    if (radio.isOn()) {
                        radio.turnOff();
                    } else {
                        try {
                            radio.writeConfig();
                        } catch (IllegalStateException e) {
                            System.err.println(e);
                        } catch (InterruptedException e) {
                            System.err.println(e);
                        }
                    }
                    System.out.println("Radio power is now " + (radio.isOn() ? "ON" : "OFF"));
                }
                break;

            case 'q':
                shutdown();
                break;
        }
    }

    protected void setupRadio() throws IOException, IllegalStateException, InterruptedException {
        System.out.println("Setup Radio [XTend 900]");

        XTend900Config radioConfig = new XTend900Config()
                .setInterfaceDataRate(XTend900Config.InterfaceDataRate.BAUD_9600)
                .setRFDataRate(XTend900Config.RFDataRate.BAUD_115200)
                .setGPO2Configuration(XTend900Config.GPO2Configuration.RX_LED)
                .setNumberBase(XTend900Config.NumberBase.DEFAULT_WITH_UNITS)
                .setTXPowerLevel(XTend900Config.TXPowerLevel.TX_1000mW)
                .setRetries((short) 0)
                .setTransmitOnly(XTend900Config.TransmitOnly.TX_RX)
                ;
        System.out.println("Config: " + radioConfig);

        Serial serial = SerialFactory.createInstance();

        String device = "/dev/ttyAMA0";
        serial.open(device, radioConfig.getInterfaceDataRate().getBaud());

        radio = new XTend900(serial);
        radio.setConfig(radioConfig);
        radio.setup();
//        radio.setLogOutputStream(log.create("xtend900.log"));
        radio.addAPIListener(new APIFrameListener() {
            @Override
            public void onRXPacket(RXPacket packet) {
                if (config.debug) {
                    System.out.println("Radio RX packet: Source address=" + packet.getSourceAddres()
                            + ", Signal strengh=-" + packet.getSignalStrength() + " dBm");
                }
//                sensors.radio.setSignalStrength(packet.getSignalStrength());

                ByteBuffer buffer = ByteBuffer.wrap(packet.getRFData());
                try {
                    sensors.fromByteBuffer(buffer);
                    if (config.debug) {
                        System.out.println("Received: " + sensors.proxy);
                    }
                } catch (BufferUnderflowException e) {
                    System.err.println(e);
                }
            }

            @Override
            public void onRFModuleStatus(RFModuleStatus rfModuleStatus) {
                if (config.debug) {
                    System.out.println("Radio status: Hardware reset=" + rfModuleStatus.isHardwareReset()
                            + ", Watchdog timer reset=" + rfModuleStatus.isWatchdogTimerReset());
                }
            }

            @Override
            public void onTXStatus(TXStatus txStatus) {
                if (config.debug) {
                    System.out.println("Radio TX status: Frame ID=" + txStatus.getFrameID()
                            + ", Success=" + (txStatus.isSuccess() ? "yes" : "no") + ", No ACK Received="
                            + (txStatus.isNoACKReceived() ? "yes" : "no"));
                }
            }
        });
    }

    private void shutdown() {
        System.out.println("Shutting down");

        System.out.println("Stopping server");
        server.stop();

        System.exit(0);
    }

    private static String nanosToDHMS(long nanoseconds) {
        long days = TimeUnit.NANOSECONDS.toDays(nanoseconds);
        long hours = TimeUnit.NANOSECONDS.toHours(nanoseconds) - TimeUnit.DAYS.toHours(days);
        long minutes = TimeUnit.NANOSECONDS.toMinutes(nanoseconds)
                - TimeUnit.HOURS.toMinutes(TimeUnit.NANOSECONDS.toHours(nanoseconds));
        long seconds = TimeUnit.NANOSECONDS.toSeconds(nanoseconds)
                - TimeUnit.MINUTES.toSeconds(TimeUnit.NANOSECONDS.toMinutes(nanoseconds));

        return String.format("%dd%02d:%02d:%02d", days, hours, minutes, seconds);
    }

}
