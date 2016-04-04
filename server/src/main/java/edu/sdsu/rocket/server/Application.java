package edu.sdsu.rocket.server;

import com.pi4j.io.serial.Serial;
import com.pi4j.io.serial.SerialFactory;
import edu.sdsu.rocket.core.helpers.RateLimitedRunnable;
import edu.sdsu.rocket.core.io.OutputStreamMultiplexer;
import edu.sdsu.rocket.core.io.StatusOutputStream;
import edu.sdsu.rocket.core.io.devices.ADS1100OutputStream;
import edu.sdsu.rocket.core.io.devices.ADS1114OutputStream;
import edu.sdsu.rocket.core.models.Sensors;
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
import net.sf.marineapi.nmea.event.SentenceEvent;
import net.sf.marineapi.nmea.event.SentenceListener;
import net.sf.marineapi.nmea.io.SentenceReader;
import net.sf.marineapi.provider.PositionProvider;
import net.sf.marineapi.provider.SatelliteInfoProvider;
import net.sf.marineapi.provider.event.PositionEvent;
import net.sf.marineapi.provider.event.ProviderListener;
import net.sf.marineapi.provider.event.SatelliteInfoEvent;
import net.sf.marineapi.provider.event.SatelliteInfoListener;

import java.io.*;

public class Application {
    
    private static final long NANOSECONDS_PER_SECOND = 1000000000L;

    private final Config config;
    private Logger log;
    private final DeviceManager manager;
    private final Reader input = new InputStreamReader(System.in);
    
    private final Sensors sensors = new Sensors();
    private final SensorServer server = new SensorServer(sensors);
    
    private XTend900 radio;
    private DeviceRunnable transmitter;
    private Watchdog watchdog;
    private Thread statusThread;
    
    public Application(Config config) {
        this.config = config;
        this.manager = new DeviceManager(config.debug);
    }
    
    public void setup() throws IOException, InterruptedException {
        setupLogging();
        setupDevices();
        setupStatusMonitor();
        setupServer(4444);
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
    
    protected void setupDevices() throws IOException, InterruptedException {
        setupADC();
//        setupGPS();
//        setupRadio();
//        setupWatchdog();
    }
    
    private void setupADC() throws IOException {
        final ADS1115[] ads1114 = new ADS1115[] {
                config.test ? new MockADS1115() : new ADS1115(ADS1115.Address.ADDR_GND),
                config.test ? new MockADS1115() : new ADS1115(ADS1115.Address.ADDR_VDD),
                config.test ? new MockADS1115() : new ADS1115(ADS1115.Address.ADDR_SDA),
                config.test ? new MockADS1115() : new ADS1115(ADS1115.Address.ADDR_SCL),
        };
        final ADS1114OutputStream[] ads1114log = new ADS1114OutputStream[ads1114.length];

        for (int i = 0; i < ads1114.length; i++) {
            final String name = "ADS1114-A" + i;

            System.out.println("Setup ADC [" + name + "]");
            ads1114log[i] = new ADS1114OutputStream(log.create(name + ".log"));

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
                    float value = ads1114[index].readMillivolts();
                    sensors.analog.set(index, value);
                    ads1114log[index].writeValue(value);
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
        final ADS1100OutputStream[] ads1100log = new ADS1100OutputStream[ads1100.length];

        for (int i = 0; i < ads1100.length; i++) {
            final int j = i + ads1114.length;
            final String name = "ADS1100-A" + j;

            System.out.println("Setup ADC [" + name + "]");
            ads1100log[i] = new ADS1100OutputStream(log.create(name + ".log"));

            ads1100[i].setup()
                    .setGain(ADS1100.Gain.PGA_1)
                    .setRate(ADS1100.Rate.SPS_16)
                    .setMode(ADS1100.Mode.CONTINUOUS)
                    .writeConfig();
            ads1100log[i].writeConfig(ads1100[i].getConfig(), ads1100[i].getSupplyVoltage());
            if (config.debug) {
                System.out.println(ads1100[i]);
            }

            final int index = i;
            manager.add(new DeviceManager.Device() {
                @Override
                public void loop() throws IOException, InterruptedException {
                    float value = ads1100[index].readVoltage();
                    sensors.analog.set(j, value * 1000f);
                    ads1100log[index].writeValue(value);
                }
            }).setFrequency(ads1100[i].getRate().getSamplesPerSecond() * 2);
        }
    }
    
    private void setupGPS() throws FileNotFoundException {
        if (config.test) {
            System.out.println("Test mode, skipping setup of GPS");
            return;
        }
        System.out.println("Setup GPS");

        FileInputStream in = new FileInputStream("/dev/ttyAMA0");
        final PrintWriter writer = new PrintWriter(log.create("gps.txt"));
        
        SentenceReader reader = new SentenceReader(in);
        reader.addSentenceListener(new SentenceListener() {
            @Override
            public void readingStarted() {
                System.out.println("GPS reading started");
            }
            
            @Override
            public void readingStopped() {
                System.out.println("GPS reading stopped");
            }
            
            @Override
            public void readingPaused() {
                System.out.println("GPS reading paused");
            }
            
            @Override
            public void sentenceRead(SentenceEvent event) {
                String sentence = event.getSentence().toString();
                writer.println(sentence);
            }
        });
        
        PositionProvider position = new PositionProvider(reader);
        position.addListener(new ProviderListener<PositionEvent>() {
            @Override
            public void providerUpdate(PositionEvent event) {
                double latitude  = event.getPosition().getLatitude();
                double longitude = event.getPosition().getLongitude();
                double altitude  = event.getPosition().getAltitude();
                if (config.debug) {
                    System.out.println("GPS provider update: latitude=" + latitude + ", longitude=" + longitude + ", altitude=" + altitude);
                }
                sensors.gps.set(latitude, longitude, altitude);
            }
        });
        
        SatelliteInfoProvider satelliteInfo = new SatelliteInfoProvider(reader);
        satelliteInfo.addListener(new SatelliteInfoListener() {
            @Override
            public void providerUpdate(SatelliteInfoEvent event) {
                int fixStatus  = event.getGpsFixStatus().toInt();
                int satellites = event.getSatelliteInfo().size();
                if (config.debug) {
                    System.out.println("GPS provider update: fix=" + fixStatus + ", satellites=" + satellites);
                }
                sensors.gps.setFixStatus(fixStatus);
                sensors.gps.setSatellites(satellites);
            }
        });
        
        reader.start();
    }
    
    private void setupStatusMonitor() throws FileNotFoundException {
        if (config.test || config.disableSystemStatus) {
            return;
        }
        System.out.println("Setup status monitor");
        final StatusOutputStream statusLog = new StatusOutputStream(log.create("status.log"));

        statusThread = new Thread(new RateLimitedRunnable(5000L) {
            @Override
            public void loop() throws InterruptedException {
                try {
                    int rawCpuTemperature = Pi.getRawCpuTemperature();
                    sensors.system.setRawTemperature(rawCpuTemperature);
                    statusLog.writeValue(rawCpuTemperature);
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

    public void loop() throws IOException {
        handleInput();
    }

    private void handleInput() throws IOException {
        switch (input.read()) {
        case '?':
            System.out.println();
            System.out.println("?: help");
            System.out.println("f: loop frequency");
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
        case 'f':
            System.out.println(manager.toString());
            break;
        case 'a':
            System.out.println(sensors.analog);
            break;
        case 'g':
            int localFix = sensors.gps.getFixStatus();
            String lf;
            switch (localFix) {
            case 2:
                lf = "2D";
                break;
            case 3:
                lf = "3D";
                break;
            default:
                lf = "no fix";
                break;
            }
            System.out.println(
                    "latitude="  + sensors.gps.getLatitude() + ",\t" +
                    "longitude=" + sensors.gps.getLongitude() + ",\t" +
                    "altitude="  + sensors.gps.getAltitude() + " m MSL,\t" +
                    "fix=" + lf + "\t" +
                    "satellites=" + sensors.gps.getSatellites()
                    );
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

    private void shutdown() {
        System.out.println("Shutting down");
        
        if (watchdog != null) {
            System.out.println("Stopping watchdog");
            watchdog.stop();
        }
        
        System.out.println("Stopping server");
        server.stop();
        
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
