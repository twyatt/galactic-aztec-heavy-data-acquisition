package edu.sdsu.rocket.server;

import com.badlogic.gdx.math.Vector3;
import com.pi4j.io.gpio.*;
import com.pi4j.io.serial.Serial;
import com.pi4j.io.serial.SerialFactory;
import edu.sdsu.rocket.core.helpers.RateLimitedRunnable;
import edu.sdsu.rocket.core.io.*;
import edu.sdsu.rocket.core.models.Sensors;
import edu.sdsu.rocket.core.net.SensorServer;
import edu.sdsu.rocket.server.devices.*;
import edu.sdsu.rocket.server.devices.DeviceManager.DeviceRunnable;
import edu.sdsu.rocket.server.devices.HMC5883L.DataOutputRate;
import edu.sdsu.rocket.server.devices.HMC5883L.MagnetometerListener;
import edu.sdsu.rocket.server.devices.HMC5883L.OperatingMode;
import edu.sdsu.rocket.server.devices.ITG3205.GyroscopeListener;
import edu.sdsu.rocket.server.devices.mock.*;
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
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

public class Application {
    
    private static final long NANOSECONDS_PER_SECOND = 1000000000L;

    private final Config config;
    private LogManager log;
    private final DeviceManager manager = new DeviceManager();
    private final Reader input = new InputStreamReader(System.in);
    
    /**
     * Provides storage of local sensor values. By local, we are referring to
     * the sensors physically attached to the system this application is running
     * on.
     */
    private final Sensors local = new Sensors();
    
    /**
     * Provides storage of the remote sensor values. Whereas remote refers to
     * the sensors that are located remotely from the system this application is
     * running on. This is generally only used for receiving sensor data from
     * the rocket over radio so this will not be used when this application is
     * running on a system located on the rocket.
     */
    private final Sensors remote = new Sensors();
    
    private final SensorServer server = new SensorServer(local, remote);
    
    private XTend900 radio;
    private DeviceRunnable transmitter;
    private Watchdog watchdog;
    private Thread statusThread;
    
    private final Vector3 tmpVec = new Vector3();

    public Application(Config config) {
        this.config = config;
    }
    
    public void setup() throws IOException, InterruptedException {
        setupLogging();
        setupDevices();
//        setupStatusMonitor();
        setupServer(4444);
    }

    protected void setupLogging() throws IOException {
        System.out.println("Setup Logging.");
        log = new LogManager(config.logDirs);

        OutputStream logStream = log.create("log.txt");
        OutputStream out = new OutputStreamMultiplexer(System.out, logStream);
        OutputStream err = new OutputStreamMultiplexer(System.err, logStream);

        // http://stackoverflow.com/a/18669284/196486
        System.setOut(new PrintStream(out));
        System.setErr(new PrintStream(err));

        System.out.println("Logging started at " + System.nanoTime() + ".");
    }
    
    protected void setupDevices() throws IOException, InterruptedException {
        setupADC();
//        setupAccelerometer();
//        setupGyroscope();
//        setupMagnetometer();
//        setupBarometer();
//        setupGPS();
//        setupRadio();
//        setupWatchdog();
    }
    
    private void setupAccelerometer() throws IOException {
        System.out.println("Setup Accelerometer [ADXL345].");
        final ADXL345OutputStream adxl345log = new ADXL345OutputStream(log.create("ADXL345.log"));
        
        ADXL345 adxl345 = config.test ? new MockADXL345() : new ADXL345();
        adxl345.setup();
        if (!adxl345.verifyDeviceID()) {
            throw new IOException("Failed to verify ADXL345 device ID.");
        }
        adxl345.writeRange(ADXL345.ADXL345_RANGE_16G);
        adxl345.writeFullResolution(true);
        adxl345.writeRate(ADXL345.ADXL345_RATE_400);
        
        float scalingFactor = adxl345.getScalingFactor();
        local.accelerometer.setScalingFactor(scalingFactor);
        adxl345log.writeScalingFactor(scalingFactor);
        System.out.println("Scaling Factor: " + scalingFactor);

        adxl345.setListener(new ADXL345.AccelerometerListener() {
            @Override
            public void onValues(short x, short y, short z) {
                local.accelerometer.setRawX(x);
                local.accelerometer.setRawY(y);
                local.accelerometer.setRawZ(z);
                try {
                    adxl345log.writeValues(x, y, z);
                } catch (IOException e) {
                    System.err.println(e);
                }
            }
        });
        
        manager.add(adxl345);
    }

    private void setupGyroscope() throws IOException {
        System.out.println("Setup Gyroscope [ITG3205].");
        final ITG3205OutputStream itg3205log = new ITG3205OutputStream(log.create("ITG3205.log"));
        
        ITG3205 itg3205 = config.test ? new MockITG3205() : new ITG3205();
        itg3205.setup();
        if (!itg3205.verifyDeviceID()) {
            throw new IOException("Failed to verify ITG3205 device ID.");
        }
        // F_sample = F_internal / (divider + 1)
        // divider = F_internal / F_sample - 1
        itg3205.writeSampleRateDivider(2); // 2667 Hz
        itg3205.writeDLPFBandwidth(ITG3205.ITG3205_DLPF_BW_256);
        
        local.gyroscope.setScalingFactor(1f / ITG3205.ITG3205_SENSITIVITY_SCALE_FACTOR);
        itg3205log.writeScalingFactor(local.gyroscope.getScalingFactor());
        
        itg3205.setListener(new GyroscopeListener() {
            @Override
            public void onValues(short x, short y, short z) {
                local.gyroscope.setRawX(x);
                local.gyroscope.setRawY(y);
                local.gyroscope.setRawZ(z);
                try {
                    itg3205log.writeValues(x, y, z);
                } catch (IOException e) {
                    System.err.println(e);
                }
            }
            
        });
        
        manager.add(itg3205);
    }
    
    private void setupMagnetometer() throws IOException, FileNotFoundException {
        System.out.println("Setup Magnetometer [HMC5883L].");
        final HMC5883LOutputStream hmc5883llog = new HMC5883LOutputStream(log.create("HMC588L.log"));

        HMC5883L hmc5883l = config.test ? new MockHMC5883L() : new HMC5883L();
        hmc5883l
            .setDataOutputRate(DataOutputRate.RATE_75)
            .setOperatingMode(OperatingMode.CONTINUOUS)
            .setup();
        
        if (!hmc5883l.verifyIdentification()) {
            throw new IOException("Failed to verify HMC5883L identification: " + Integer.toHexString(hmc5883l.getIdentification()));
        }
        
        float scalingFactor = hmc5883l.getGain().getResolution();
        local.magnetometer.setScalingFactor(scalingFactor);
        hmc5883llog.writeScalingFactor(scalingFactor);
        System.out.println("Scaling Factor: " + scalingFactor);
        
        hmc5883l.setListener(new MagnetometerListener() {
            @Override
            public void onValues(short x, short y, short z) {
                local.magnetometer.setRawX(x);
                local.magnetometer.setRawY(y);
                local.magnetometer.setRawZ(z);
                try {
                    hmc5883llog.writeValues(x, y, z);
                } catch (IOException e) {
                    System.err.println(e);
                }
            }
            
        });
        
        manager
            .add(hmc5883l)
            .setSleep(hmc5883l.getDataOutputRate().getDelay());
    }
    
    private void setupBarometer() throws IOException {
        System.out.println("Setup Barometer [MS5611].");
        final MS5611OutputStream ms5611log = new MS5611OutputStream(log.create("MS5611.log"));

        MS5611 ms5611 = config.test ? new MockMS5611() : new MS5611();
        ms5611.setup();
        
        ms5611.setListener(new MS5611.BarometerListener() {
            @Override
            public void onValues(int T, int P) {
                local.barometer.setRawTemperature(T);
                local.barometer.setRawPressure(P);
                try {
                    ms5611log.writeValues(T, P);
                } catch (IOException e) {
                    System.err.println(e);
                }
            }

            @Override
            public void onFault(MS5611.Fault fault) {
                try {
                    ms5611log.writeFault(fault.ordinal());
                } catch (IOException e) {
                    System.err.println(e);
                }
                
                if (config.debug) {
                    System.err.println("MS5611 fault: " + fault);
                }
            }
        });
        
        manager.add(ms5611);
    }

    private void setupADC() throws IOException {
        final ADS1115[] ads1115 = new ADS1115[] {
                config.test ? new MockADS1115() : new ADS1115(ADS1115.Address.ADDR_GND),
                config.test ? new MockADS1115() : new ADS1115(ADS1115.Address.ADDR_VDD),
                config.test ? new MockADS1115() : new ADS1115(ADS1115.Address.ADDR_SDA),
                config.test ? new MockADS1115() : new ADS1115(ADS1115.Address.ADDR_SCL),
        };
        final SingleChannelADCOutputStream[] ads1115log = new SingleChannelADCOutputStream[ads1115.length];

        for (int i = 0; i < ads1115.length; i++) {
            final String name = "ADS1115-A" + i;

            System.out.println("Setup ADC [" + name + "]");
            ads1115log[i] = new SingleChannelADCOutputStream(log.create(name + ".log"));

            ads1115[i].setup()
                    .setGain(ADS1115.Gain.PGA_2_3)
                    .setMode(ADS1115.Mode.MODE_CONTINUOUS)
                    .setRate(ADS1115.Rate.DR_860SPS)
                    .setComparator(ADS1115.Comparator.COMP_MODE_HYSTERESIS)
                    .setSingleEnded(ADS1115.Channel.A0)
                    .writeConfig();
            System.out.println(ads1115[i]);

            final int index = i;
            manager.add(new DeviceManager.Device() {
                @Override
                public void loop() throws IOException, InterruptedException {
                    float value = ads1115[index].readMillivolts();
                    local.analog.set(index, value);
                    ads1115log[index].writeValue(value);
                }
            });
        }


        final ADS1100[] ads1100 = new ADS1100[] {
                config.test ? new MockADS1100() : new ADS1100(ADS1100.Address.AD4),
                config.test ? new MockADS1100() : new ADS1100(ADS1100.Address.AD5),
        };
        final SingleChannelADCOutputStream[] ads1100log = new SingleChannelADCOutputStream[ads1100.length];

        for (int i = 0; i < ads1100.length; i++) {
            final int j = i + ads1115.length;
            final String name = "ADS1100-A" + j;

            System.out.println("Setup ADC [" + name + "]");
            ads1100log[i] = new SingleChannelADCOutputStream(log.create(name + ".log"));

            ads1100[i].setup()
                    .setGain(ADS1100.Gain.PGA_1)
                    .setRate(ADS1100.Rate.SPS_16)
                    .setMode(ADS1100.Mode.CONTINUOUS)
                    .writeConfig();
            System.out.println(ads1100[i]);

            final int index = i;
            manager.add(new DeviceManager.Device() {
                @Override
                public void loop() throws IOException, InterruptedException {
                    float value = ads1100[index].readVoltage();
                    local.analog.set(j, value * 1000f);
                    ads1100log[index].writeValue(value);
                }
            }).setFrequency(ads1100[i].getRate().getSamplesPerSecond() * 2);
        }
    }
    
    private void setupGPS() throws FileNotFoundException {
        if (config.test) {
            System.out.println("Test mode, skipping setup of GPS [Adafruit Ultimate GPS].");
            return;
        }
        System.out.println("Setup GPS [Adafruit Ultimate GPS].");

        FileInputStream in = new FileInputStream("/dev/ttyAMA0");
        final PrintWriter writer = new PrintWriter(log.create("gps.txt"));
        
        SentenceReader reader = new SentenceReader(in);
        reader.addSentenceListener(new SentenceListener() {
            @Override
            public void readingStarted() {
                System.out.println("GPS reading started.");
            }
            
            @Override
            public void readingStopped() {
                System.out.println("GPS reading stopped.");
            }
            
            @Override
            public void readingPaused() {
                System.out.println("GPS reading paused.");
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
                local.gps.set(latitude, longitude, altitude);
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
                local.gps.setFixStatus(fixStatus);
                local.gps.setSatellites(satellites);
            }
        });
        
        reader.start();
    }
    
    private void setupStatusMonitor() {
        if (config.test) {
            return;
        }
        System.out.println("Setup status monitor.");
        
        GpioController gpio = GpioFactory.getInstance();
        final GpioPinDigitalInput pgood = gpio.provisionDigitalInputPin(RaspiPin.GPIO_22, "PGOOD", PinPullResistance.OFF);
        
        statusThread = new Thread(new RateLimitedRunnable(1000L) {
            @Override
            public void loop() throws InterruptedException {
                try {
                    local.system.setRawTemperature(Pi.getRawCpuTemperature());
                    local.system.setIsPowerGood(pgood.isHigh());
                } catch (IOException e) {
                    System.err.println(e);
                }
            }
        });
        statusThread.start();
    }
    
    protected void setupServer(int port) throws IOException {
        System.out.println("Setup server.");
        server.setDebug(config.debug);
        server.start(port);
    }
    
    protected void setupRadio() throws IOException, IllegalStateException, InterruptedException {
        System.out.println("Setup Radio [XTend 900].");

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
                    System.out.println("Watchdog disabled.");
                    watchdog.disable();
                }
                @Override
                public void onRadioTurnedOn() {
                    System.out.println("Watchdog enabled.");
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
//                System.out.println("Radio RX packet: Source address=" + packet.getSourceAddres() + ", Signal strengh=-" + packet.getSignalStrength() + " dBm");
                local.radio.setSignalStrength(packet.getSignalStrength());
                ByteBuffer buffer = ByteBuffer.wrap(packet.getRFData());
                try {
                    remote.fromByteBuffer(buffer);
                } catch (BufferUnderflowException e) {
                    System.err.println(e);
                }
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
        
        SensorsTransmitter transmitter = new SensorsTransmitter(radio, local);
        this.transmitter = manager.add(transmitter, true /* paused */);
    }

    private void setupWatchdog() {
        System.out.println("Setup watchdog for XTend 900.");
        
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
            System.out.println("s/S: system status (local/remote)");
            System.out.println("a/A: accelerometer (local/remote)");
            System.out.println("g/G: gyroscope (local/remote)");
            System.out.println("m/M: magnetometer (local/remote)");
            System.out.println("b/B: barometer (local/remote)");
            System.out.println("c/C: analog (local/remote)");
            System.out.println("p/P: gps (local/remote)");
            System.out.println("r/R: radio (local/remote)");
            if (radio != null) {
                System.out.println("d: toggle radio power (currently " + (radio.isOn() ? "ON" : "OFF") + ")");
                System.out.println("t: toggle radio transmission (currently " + (transmitter != null && !transmitter.isPaused() ? "ON" : "OFF") + ")");
            }
            System.out.println("q: quit");
            System.out.println();
            break;
        case 'w':
            if (watchdog == null) {
                System.out.println("Watchdog disabled.");
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
            System.out.println("CPU: " + local.system.getTemperatureC() + " C, " + local.system.getTemperatureF() + " F, Power " + (local.system.getIsPowerGood() ? "GOOD" : "BAD"));
            break;
        case 'S':
            System.out.println("CPU: " + remote.system.getTemperatureC() + " C, " + remote.system.getTemperatureF() + " F, Power " + (remote.system.getIsPowerGood() ? "GOOD" : "BAD"));
            break;
        case 'f':
            System.out.println(manager.toString());
            break;
        case 'a':
            local.accelerometer.get(tmpVec);
            System.out.println(tmpVec.scl(9.8f) + " m/s^2");
            break;
        case 'A':
            remote.accelerometer.get(tmpVec);
            System.out.println(tmpVec.scl(9.8f) + " m/s^2");
            break;
        case 'm':
            local.magnetometer.get(tmpVec);
            System.out.println(tmpVec + " Ga");
            break;
        case 'M':
            remote.magnetometer.get(tmpVec);
            System.out.println(tmpVec + " Ga");
            break;
        case 'b':
            System.out.println(local.barometer.getTemperature() + " C, " + local.barometer.getPressure() + " mbar");
            break;
        case 'B':
            System.out.println(remote.barometer.getTemperature() + " C, " + remote.barometer.getPressure() + " mbar");
            break;
        case 'g':
            local.gyroscope.get(tmpVec);
            System.out.println(tmpVec + " deg/s");
            break;
        case 'G':
            remote.gyroscope.get(tmpVec);
            System.out.println(tmpVec + " deg/s");
            break;
        case 'c':
            System.out.println(local.analog);
            break;
        case 'C':
            System.out.println(remote.analog);
            break;
        case 'p':
            int localFix = local.gps.getFixStatus();
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
                    "latitude="  + local.gps.getLatitude() + ",\t" +
                    "longitude=" + local.gps.getLongitude() + ",\t" +
                    "altitude="  + local.gps.getAltitude() + " m MSL,\t" +
                    "fix=" + lf + "\t" +
                    "satellites=" + local.gps.getSatellites()
                    );
            break;
        case 'P':
            int remoteFix = local.gps.getFixStatus();
            String rf;
            switch (remoteFix) {
            case 2:
                rf = "2D";
                break;
            case 3:
                rf = "3D";
                break;
            default:
                rf = "no fix";
                break;
            }
            System.out.println(
                    "latitude="  + remote.gps.getLatitude() + ",\t" +
                    "longitude=" + remote.gps.getLongitude() + ",\t" +
                    "altitude="  + remote.gps.getAltitude() + " m MSL,\t" +
                    "fix=" + rf + "\t" +
                    "satellites=" + local.gps.getSatellites()
                    );
            break;
        case 'r':
            System.out.println("Signal Strength: -" + local.radio.getSignalStrength() + " dBm");
            break;
        case 'R':
            System.out.println("Signal Strength: -" + remote.radio.getSignalStrength() + " dBm");
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
                System.out.println("Radio power is now " + (radio.isOn() ? "ON" : "OFF") + ".");
                System.out.println("Radio transmission is now " + (transmitter != null && !transmitter.isPaused() ? "ON" : "OFF") + ".");
            }
            break;
        case 't':
            if (transmitter != null) {
                if (transmitter.isPaused()) {
                    transmitter.resume();
                } else {
                    transmitter.pause();
                }
                System.out.println("Radio sensor transmission is now " + (transmitter.isPaused() ? "OFF" : "ON") + ".");
            }
            break;
        case 'q':
            shutdown();
            break;
        }
    }

    private void shutdown() {
        System.out.println("Shutting down.");
        
        if (watchdog != null) {
            System.out.println("Stopping watchdog.");
            watchdog.stop();
        }
        
        System.out.println("Stopping server.");
        server.stop();
        
        if (statusThread != null) {
            System.out.println("Stopping status monitor.");
            statusThread.interrupt();
            try {
                statusThread.join();
            } catch (InterruptedException e) {
                System.err.println(e);
            }
        }
        
        System.out.println("Stopping device manager.");
        manager.clear();

        if (log != null) {
            System.out.println("Closing log streams.");
            log.close();
        }

        System.exit(0);
    }
    
}
