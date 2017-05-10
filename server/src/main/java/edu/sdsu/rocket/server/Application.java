package edu.sdsu.rocket.server;

import com.pi4j.io.i2c.I2CFactory;
import com.pi4j.io.serial.Serial;
import com.pi4j.io.serial.SerialFactory;
import edu.sdsu.rocket.core.helpers.AtomicIntFloat;
import edu.sdsu.rocket.core.helpers.Logger;
import edu.sdsu.rocket.core.helpers.RateLimitedRunnable;
import edu.sdsu.rocket.core.helpers.Stopwatch;
import edu.sdsu.rocket.core.io.OutputStreamMultiplexer;
import edu.sdsu.rocket.core.io.StatusOutputStream;
import edu.sdsu.rocket.core.io.devices.ADS11xxOutputStream;
import edu.sdsu.rocket.core.models.Gps;
import edu.sdsu.rocket.core.models.ProxyData;
import edu.sdsu.rocket.core.models.Sensors;
import edu.sdsu.rocket.core.models.Stim300;
import edu.sdsu.rocket.core.net.DatagramPacketListener;
import edu.sdsu.rocket.core.net.DatagramServer;
import edu.sdsu.rocket.core.net.SensorServer;
import edu.sdsu.rocket.server.devices.ADS1100;
import edu.sdsu.rocket.server.devices.ADS1115;
import edu.sdsu.rocket.server.devices.DeviceManager;
import edu.sdsu.rocket.server.devices.DeviceManager.DeviceRunnable;
import edu.sdsu.rocket.server.devices.mock.MockADS1100;
import edu.sdsu.rocket.server.devices.mock.MockADS1115;
import edu.sdsu.rocket.server.io.radio.*;
import edu.sdsu.rocket.server.io.radio.Watchdog.WatchdogListener;
import edu.sdsu.rocket.server.io.radio.XTend900.XTend900Listener;
import edu.sdsu.rocket.server.io.radio.api.RFModuleStatus;
import edu.sdsu.rocket.server.io.radio.api.RXPacket;
import edu.sdsu.rocket.server.io.radio.api.TXStatus;

import java.io.*;
import java.net.DatagramPacket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class Application {

    private final Stopwatch STOPWATCH = new Stopwatch();

    private final Config config;
    private Logger log;
    private final DeviceManager manager;
    private final Reader input = new InputStreamReader(System.in);

    private final Sensors sensors = new Sensors();
    private final SensorServer server = new SensorServer(sensors);

    private final DatagramServer dataProxyServer = new DatagramServer();

    private XTend900 radio;
    private DeviceRunnable transmitter;
    private Watchdog watchdog;
    private Thread statusThread;

    public Application(Config config) {
        this.config = config;
        this.manager = new DeviceManager(config.debug);
    }

    public void setup() throws IOException, InterruptedException, I2CFactory.UnsupportedBusNumberException {
        setupLogging();
        setupDevices();
        setupStatusMonitor();
        setupServer(4444);
        setupDataProxyServer(6666);
    }

    protected void setupLogging() throws IOException {
        System.out.println("Setup Logging");
        log = new Logger(config.logDirs);

        OutputStream logStream = log.create("log.txt");
        OutputStream out = new OutputStreamMultiplexer(System.out, logStream);
        OutputStream err = new OutputStreamMultiplexer(System.err, logStream);

        // http://stackoverflow.com/a/18669284/196486
        System.setOut(new PrintStream(out));
        System.setErr(new PrintStream(err));

        System.out.println("Logging started at " + System.nanoTime());
    }

    protected void setupDevices() throws IOException, InterruptedException, I2CFactory.UnsupportedBusNumberException {
        setupADC();
//        setupGPS();
//        setupRadio();
//        setupWatchdog();
    }

    private void setupADC() throws IOException, I2CFactory.UnsupportedBusNumberException {
        final ADS1115[] ads1114 = new ADS1115[] {
                config.test ? new MockADS1115() : new ADS1115(ADS1115.Address.ADDR_GND),
                config.test ? new MockADS1115() : new ADS1115(ADS1115.Address.ADDR_VDD),
                config.test ? new MockADS1115() : new ADS1115(ADS1115.Address.ADDR_SDA),
                config.test ? new MockADS1115() : new ADS1115(ADS1115.Address.ADDR_SCL),
        };
        final ADS11xxOutputStream[] ads1114log = new ADS11xxOutputStream[ads1114.length];

        for (int i = 0; i < ads1114.length; i++) {
            final String name = "A" + i;

            System.out.println("Setup ADC ADS1114 [" + name + "]");
            ads1114log[i] = new ADS11xxOutputStream(log.create(name + ".log"));

            ads1114[i].setup()
                    .setGain(ADS1115.Gain.PGA_2_3)
                    .setMode(ADS1115.Mode.MODE_CONTINUOUS)
                    .setRate(ADS1115.Rate.DR_860SPS)
                    .setComparator(ADS1115.Comparator.COMP_MODE_HYSTERESIS)
                    .setSingleEnded(ADS1115.Channel.A0)
                    .writeConfig();
            ads1114log[i].writeConfig(ads1114[i].getConfig());
            if (config.debug) {
                System.out.println(ads1114[i]);
            }

            final int index = i;
            DeviceRunnable deviceRunnable = manager.add(new DeviceManager.Device() {
                @Override
                public void loop() throws IOException, InterruptedException {
                    long timestamp = STOPWATCH.nanoSecondsElapsed();
                    int timestampMillis = (int) TimeUnit.NANOSECONDS.toMillis(timestamp);
                    float value = ads1114[index].readMillivolts();

                    sensors.analog[index].set(timestampMillis, value);
                    ads1114log[index].writeValue(timestamp, value);
                }
            });
            if (config.test) {
                int frequency = ads1114[index].getRate().getSamplesPerSecond() * 2;
                deviceRunnable.setFrequency(frequency);
            }
        }


        final ADS1100[] ads1100 = new ADS1100[] {
                config.test ? new MockADS1100() : new ADS1100(ADS1100.Address.AD4),
                config.test ? new MockADS1100() : new ADS1100(ADS1100.Address.AD5),
        };
        final ADS11xxOutputStream[] ads1100log = new ADS11xxOutputStream[ads1100.length];

        for (int i = 0; i < ads1100.length; i++) {
            final int j = i + ads1114.length;
            final String name = "A" + j;

            System.out.println("Setup ADC ADS1100 [" + name + "]");
            ads1100log[i] = new ADS11xxOutputStream(log.create(name + ".log"));

            ads1100[i].setup()
                    .setGain(ADS1100.Gain.PGA_1)
                    .setRate(ADS1100.Rate.SPS_16)
                    .setMode(ADS1100.Mode.CONTINUOUS)
                    .writeConfig();
            ads1100log[i].writeConfig(ads1100[i].getConfig());
            System.out.println("Supply Voltage: " + ads1100[i].getSupplyVoltage());
            if (config.debug) {
                System.out.println(ads1100[i]);
            }

            final int index = i;
            manager.add(new DeviceManager.Device() {
                @Override
                public void loop() throws IOException, InterruptedException {
                    long timestamp = STOPWATCH.nanoSecondsElapsed();
                    int timestampMillis = (int) TimeUnit.NANOSECONDS.toMillis(timestamp);
                    float mV = ads1100[index].readVoltage() * 1000;

                    sensors.analog[j].set(timestampMillis, mV);
                    ads1100log[index].writeValue(timestamp, mV);
                }
            }).setFrequency(ads1100[i].getRate().getSamplesPerSecond() * 2);
        }
    }

    private void setupStatusMonitor() throws FileNotFoundException {
        if (config.disableSystemStatus) {
            return;
        }
        System.out.println("Setup status monitor");
        final StatusOutputStream statusLog = new StatusOutputStream(log.create("status.log"));

        statusThread = new Thread(new RateLimitedRunnable(5000L) {
            @Override
            public void loop() throws InterruptedException {
                try {
                    long timestamp = STOPWATCH.nanoSecondsElapsed();
                    final int rawCpuTemperature = config.test
                            ? new Random().nextInt(100 * 1000)
                            : Pi.getRawCpuTemperature();

                    sensors.system.setRawTemperature(rawCpuTemperature);
                    statusLog.writeValue(timestamp, rawCpuTemperature);
                } catch (IOException e) {
                    System.err.println(e);
                }
            }
        });
        statusThread.start();
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
                .setTransmitOnly(XTend900Config.TransmitOnly.TX_ONLY)
                ;
        System.out.println("Config: " + radioConfig);

        Serial serial = SerialFactory.createInstance();

        String device = "/dev/ttyAMA0";
        serial.open(device, radioConfig.getInterfaceDataRate().getBaud());

        radio = new XTend900(serial);
        radio.setConfig(radioConfig);
        radio.setup();
        radio.setLogOutputStream(log.create("xtend900.log"));
        if (watchdog != null) {
            radio.addListener(new XTend900Listener() {
                @Override
                public void onRadioTurnedOff() {
                    System.out.println("Watchdog disabled");
                    watchdog.disable();
                }
                @Override
                public void onRadioTurnedOn() {
                    System.out.println("Watchdog enabled");
                    watchdog.enable();
                }
                @Override
                public void onDataReceived(byte[] data) {
                    // TODO Auto-generated method stub
                }
            });
        }
        radio.addAPIListener(new APIFrameListener() {
            @Override
            public void onRXPacket(RXPacket packet) {
                if (config.debug) {
                    System.out.println("Radio RX packet: Source address=" + packet.getSourceAddres() + ", Signal strengh=-" + packet.getSignalStrength() + " dBm");
                }
                sensors.radio.setSignalStrength(packet.getSignalStrength());

//                ByteBuffer buffer = ByteBuffer.wrap(packet.getRFData());
//                try {
//                    remote.fromByteBuffer(buffer);
//                } catch (BufferUnderflowException e) {
//                    System.err.println(e);
//                }
            }

            @Override
            public void onRFModuleStatus(RFModuleStatus rfModuleStatus) {
                if (config.debug) {
                    System.out.println("Radio status: Hardware reset=" + rfModuleStatus.isHardwareReset() + ", Watchdog timer reset=" + rfModuleStatus.isWatchdogTimerReset());
                }
            }

            @Override
            public void onTXStatus(TXStatus txStatus) {
                if (config.debug) {
                    System.out.println("Radio TX status: Frame ID=" + txStatus.getFrameID() + ", Success=" + (txStatus.isSuccess() ? "yes" : "no") + ", No ACK Received=" + (txStatus.isNoACKReceived() ? "yes" : "no"));
                }
            }
        });

        SensorsTransmitter transmitter = new SensorsTransmitter(radio, sensors);
        this.transmitter = manager.add(transmitter, true /* paused */);
    }

    private void setupWatchdog() {
        System.out.println("Setup watchdog for XTend 900");

        watchdog = new Watchdog(30);
        watchdog.setListener(new WatchdogListener() {
            @Override
            public void triggered() {
                if (config.debug) System.out.println("Watchdog triggered!");
                if (transmitter != null && transmitter.isPaused()) {
                    transmitter.resume();
                }
                watchdog.stop();
            }
        });
        watchdog.start();
        radio.addAPIListener(watchdog);
    }

    protected void setupServer(int port) throws IOException {
        System.out.println("Setup server");
        server.setDebug(config.debug);
        server.start(port);
    }

    /**
     * Data proxy server. Listens on specified {@code port} for data to be read into a {@link ProxyData} object. The
     * data can later be sent on request to the Data Acquisition Client.
     * <p/>
     * Per discussion with <a href="mailto:cengelbrecht91@gmail.com">Christian Engelbrecht</a>, they will be creating a
     * Python script to read data from their STIM300 and GPS units. They will then write the data over UDP to this
     * "proxy" server, so that the Data Acquisition Client request it over radio transmission (e.g. via XTend 900 units).
     */
    private void setupDataProxyServer(int port) throws SocketException {
        dataProxyServer.setListener(new DatagramPacketListener() {
            @Override
            public void onPacketReceived(DatagramPacket packet) {
                SocketAddress address = packet.getSocketAddress();
                ByteBuffer buffer = ByteBuffer.wrap(packet.getData(), 0, packet.getLength());
                try {
                    sensors.proxy.fromByteBuffer(buffer);
                } catch (BufferUnderflowException e) {
                    System.err.println("Received invalid packet from " + address);
                }
            }
        });
        dataProxyServer.start(port);
    }

    public void loop() throws IOException {
        handleInput();
    }

    private void handleInput() throws IOException {
        switch (input.read()) {
        case '?':
            System.out.println();
            System.out.println("?: help");
            System.out.println("f: loop frequency");
            System.out.println("e: runtime");
            if (watchdog != null) {
                System.out.println("w: watchdog status");
                System.out.println("W: watchdog start");
            }
            if (statusThread != null) {
                System.out.println("s: system status");
            }
            System.out.println("a: analog");
            System.out.println("g: gps");
            System.out.println("r: radio");
            if (radio != null) {
                System.out.println("d: toggle radio power (currently " + (radio.isOn() ? "ON" : "OFF") + ")");
                System.out.println("t: toggle radio transmission (currently " + (transmitter != null && !transmitter.isPaused() ? "ON" : "OFF") + ")");
            }
            System.out.println("q: quit");
            System.out.println();
            break;
        case 'f':
            System.out.println(manager.toString());
            break;
        case 'e':
            System.out.println("Runtime: " + nanosToDHMS(STOPWATCH.nanoSecondsElapsed()));
            break;
        case 'w':
            if (watchdog == null) {
                System.out.println("Watchdog disabled");
            } else {
                System.out.println("Watchdog: time until timeout=" + watchdog.getTimeoutTimeRemaining() + " s, countdown=" + watchdog.getCountdownTimeRemaining() + " s");
            }
            break;
        case 'W':
            if (watchdog != null) {
                watchdog.startCountdown(10);
                watchdog.enable();
            }
            break;
        case 's':
            if (statusThread != null) {
                System.out.println("CPU: " + sensors.system.getTemperatureC() + " °C, " + sensors.system.getTemperatureF() + " °F");
            }
            break;
        case 'a':
            String a[] = new String[sensors.analog.length];
            for (int i = 0; i < sensors.analog.length; i++) {
                long raw = sensors.analog[i].get();
                a[i] = "A" + i + "=" + AtomicIntFloat.getFloatValue(raw);
            }
            System.out.println(Arrays.toString(a));
            break;
        case 'r':
            System.out.println("Signal Strength: -" + sensors.radio.getSignalStrength() + " dBm");
            break;
        case 'd':
            if (radio != null) {
                if (radio.isOn()) {
                    radio.turnOff();
                    if (transmitter != null && !transmitter.isPaused()) {
                        transmitter.pause();
                    }
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
                System.out.println("Radio transmission is now " + (transmitter != null && !transmitter.isPaused() ? "ON" : "OFF"));
            }
            break;
        case 't':
            if (transmitter != null) {
                if (transmitter.isPaused()) {
                    transmitter.resume();
                } else {
                    transmitter.pause();
                }
                System.out.println("Radio sensor transmission is now " + (transmitter.isPaused() ? "OFF" : "ON"));
            }
            break;
        case 'q':
            shutdown();
            break;
        }
    }

    private static String nanosToDHMS(long nanoseconds) {
        long days = TimeUnit.NANOSECONDS.toDays(nanoseconds);
        long hours = TimeUnit.NANOSECONDS.toHours(nanoseconds) - TimeUnit.DAYS.toHours(days);
        long minutes = TimeUnit.NANOSECONDS.toMinutes(nanoseconds) - TimeUnit.HOURS.toMinutes(TimeUnit.NANOSECONDS.toHours(nanoseconds));
        long seconds = TimeUnit.NANOSECONDS.toSeconds(nanoseconds) - TimeUnit.MINUTES.toSeconds(TimeUnit.NANOSECONDS.toMinutes(nanoseconds));
        return String.format("%dd%02d:%02d:%02d", days, hours, minutes, seconds);
    }

    private void shutdown() {
        System.out.println("Shutting down");

        if (watchdog != null) {
            System.out.println("Stopping watchdog");
            watchdog.stop();
        }

        System.out.println("Stopping server");
        server.stop();


        System.out.println("Stopping STIM300 server");
        dataProxyServer.stop();

        if (statusThread != null) {
            System.out.println("Stopping status monitor");
            statusThread.interrupt();
            try {
                statusThread.join();
            } catch (InterruptedException e) {
                System.err.println(e);
            }
        }

        System.out.println("Stopping device manager");
        manager.clear();

        if (log != null) {
            System.out.println("Closing log streams");
            log.close();
        }

        System.exit(0);
    }

}
