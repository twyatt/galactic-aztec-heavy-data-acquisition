package edu.sdsu.rocket.server;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.badlogic.gdx.math.Vector3;
import com.pi4j.io.i2c.I2CBus;

import edu.sdsu.rocket.helpers.Stopwatch;
import edu.sdsu.rocket.io.Message;
import edu.sdsu.rocket.models.Sensors;
import edu.sdsu.rocket.server.devices.ADS1115;
import edu.sdsu.rocket.server.devices.ADS1115Wrapper;
import edu.sdsu.rocket.server.devices.ADXL345;
import edu.sdsu.rocket.server.devices.ITG3205;
import edu.sdsu.rocket.server.devices.MS5611;
import edu.sdsu.rocket.server.devices.MS5611Wrapper;
import edu.sdsu.rocket.server.io.DataLogger;
import edu.sdsu.rocket.server.io.DatagramServer;
import edu.sdsu.rocket.server.io.TextLogger;

public class Application {
	
	public static final String FILE_SEPARATOR = System.getProperty("file.separator");
	private static final long SECONDS_TO_NANOSECONDS = 1000000000L;
	
	private static final int SERVER_PORT = 4444;
	private static final int BUFFER_SIZE = 64; // bytes
	private static final long FREQUENCY_CHECK_INTERVAL = 10L * SECONDS_TO_NANOSECONDS; // nanoseconds
	
	protected static final String EVENT_LOG   = "event.log";
	protected static final String ADXL345_LOG = "adxl345.log";
	protected static final String ITG3205_LOG = "itg3205.log";
	protected static final String MS5611_LOG  = "ms5611.log";
	protected static final String ADS1115_LOG = "ads1115.log";
	private static final byte SCALING_FACTOR = 0x1;
	
	// http://pi.gadgetoid.com/pinout
	ADXL345 adxl345 = new ADXL345(I2CBus.BUS_1);
	ITG3205 itg3205 = new ITG3205(I2CBus.BUS_1);
	MS5611Wrapper ms5611 = new MS5611Wrapper(new MS5611(I2CBus.BUS_1));
	ADS1115Wrapper ads1115 = new ADS1115Wrapper(new ADS1115(I2CBus.BUS_1));

	protected TextLogger log;
	protected DataLogger logger;
	private DatagramServer server;
	private int messageNumber;
	
	private final Reader input = new InputStreamReader(System.in);
	protected final Sensors sensors;
	
	private final Stopwatch stopwatch = new Stopwatch();
	private long loops;
	private float frequency; // Hz
	
	private final ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
	private final Vector3 tmpVec = new Vector3();
	
	public Application(Sensors sensors) {
		if (sensors == null) {
			throw new NullPointerException();
		}
		this.sensors = sensors;
	}
	
	public void setup() throws IOException {
		loggerSetup();
		sensorSetup();
		serverSetup();
	}
	
	public void loop() throws IOException {
		sensorLoop();
		serverLoop();
		inputLoop();
		
		loops++;
		if (stopwatch.nanoSecondsElapsed() >= FREQUENCY_CHECK_INTERVAL) {
			float seconds = stopwatch.nanoSecondsElapsed() / SECONDS_TO_NANOSECONDS;
			frequency = (float) loops / seconds;
			loops = 0;
			stopwatch.reset();
		}
	}
	
	protected void loggerSetup() throws IOException {
		File userDir = new File(System.getProperty("user.dir", "~"));
		if (!userDir.exists()) {
			throw new IOException("Directory does not exist: " + userDir);
		}
		
		DateFormat dirDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
		String timestamp = dirDateFormat.format(new Date());
		
		File logDir = new File(userDir + FILE_SEPARATOR + "logs" + FILE_SEPARATOR + timestamp);
		if (!logDir.exists()) {
			logDir.mkdirs();
		}
		
		long now = System.currentTimeMillis();
		DateFormat logDateFormat = DateFormat.getDateInstance(DateFormat.LONG);
		log = new TextLogger(logDir + FILE_SEPARATOR + EVENT_LOG);
		log.message("Logging started at " + logDateFormat.format(new Date(now)) + " (" + now + " ms since Unix Epoch).");
		
		logger = new DataLogger(logDir);
	}
	
	protected void sensorSetup() throws IOException {
		adxl345.setup();
		if (!adxl345.verifyDeviceID()) {
			throw new IOException("Failed to verify ADXL345 device ID");
		}
		
		itg3205.setup();
		if (!itg3205.verifyDeviceID()) {
			throw new IOException("Failed to verify ITG3205 device ID");
		}
		
		adxl345.writeRange(ADXL345.ADXL345_RANGE_16G);
		adxl345.writeFullResolution(true);
		adxl345.writeRate(ADXL345.ADXL345_RATE_400);
		sensors.setAccelerometerScalingFactor(adxl345.getScalingFactor());
		logger.log(ADXL345_LOG, SCALING_FACTOR, sensors.getAccelerometerScalingFactor());
		
		// F_sample = F_internal / (divider + 1)
		// divider = F_internal / F_sample - 1
		itg3205.writeSampleRateDivider(2); // 2667 Hz
		itg3205.writeDLPFBandwidth(ITG3205.ITG3205_DLPF_BW_256);
		sensors.setGyroscopeScalingFactor(1f / ITG3205.ITG3205_SENSITIVITY_SCALE_FACTOR);
		logger.log(ITG3205_LOG, SCALING_FACTOR, sensors.getGyroscopeScalingFactor());
		
		ms5611.getDevice().setup();
		
		ads1115.getDevice().setup();
		ads1115.getDevice().writeGain(ADS1115.ADS1115_PGA_4P096); // +/- 4.096V
//		ads1115.getDevice().writeRate(ADS1115.ADS1115_RATE_64); // 64 samples/second => ~15 Hz for 4 single-ended
		ads1115.getDevice().writeRate(ADS1115.ADS1115_RATE_475); // 475 samples/second => ~83 Hz for 4 single-ended
//		ads1115.getDevice().writeRate(ADS1115.ADS1115_RATE_860); // 860 samples/second => ~119 Hz for 4 single-ended
		int sps = ADS1115.getSamplesPerSecond(ads1115.getDevice().readRate());
		long timeout = 1000000000L / sps * 5L; // 5 X expected sample duration
		ads1115.setTimeout(timeout);
	}
	
	protected void serverSetup() throws IOException {
		server = new DatagramServer(SERVER_PORT);
		server.start();
	}

	/**
	 * Sends current sensor data to specified address.
	 * 
	 * @param address
	 * @throws IOException 
	 */
	public void sendSensorData(SocketAddress address) throws IOException {
		DatagramSocket socket = server.getSocket();
		if (socket != null) {
			buffer.clear();
			buffer.putInt(++messageNumber);
			buffer.put(Message.SENSOR);
			sensors.toByteBuffer(buffer);
			
			byte[] buf = buffer.array();
			int length = buffer.position();
			DatagramPacket packet = new DatagramPacket(buf, length, address);
			
			socket.send(packet);
		}
	}

	protected void sensorLoop() throws IOException {
		adxl345.readRawAcceleration(sensors.accelerometer);
		logger.log(ADXL345_LOG, sensors.accelerometer);
		
		itg3205.readRawRotations(sensors.gyroscope);
		logger.log(ITG3205_LOG, sensors.gyroscope);
		
		int status;
		status = ms5611.read(sensors.barometer);
		if (status == 0) {
			logger.log(MS5611_LOG, sensors.barometer);
		} else if (status > 0) { // fault occurred
//			Console.error("MS5611 D" + status + " fault.");
			// TODO log barometer fault
		}
		
		status = ads1115.read(sensors.analog);
		if (status == 0) {
			logger.log(ADS1115_LOG, sensors.analog);
		} else if (status > 0) { // error
//			Console.error("ADS1115 error code " + status + ".");
			// TODO log adc error
		}
	}
	
	protected void serverLoop() {
		Message message = server.read();
		if (message != null) {
			try {
				switch (message.id) {
				case Message.SENSOR:
					sendSensorData(message.address);
					break;
				}
			} catch (IOException e) {
				System.err.println(e.getMessage());
			}
		}
	}

	protected void inputLoop() throws IOException {
		if (input.ready()) {
			int c = input.read();
			switch (c) {
			case '?':
				Console.log();
				Console.log("?: help");
				Console.log("t: cpu temperature");
				Console.log("f: loop frequency");
				Console.log("q: quit");
				Console.log("a: accelerometer");
				Console.log("b: barometer");
				Console.log("g: gyroscope");
				Console.log("c: analog");
				Console.log();
				break;
			case 't':
			case 'T':
				try {
					float tempC = Pi.getCpuTemperature();
					float tempF = tempC * 9f / 5f + 32f;
					Console.log("CPU: " + tempC + " C, " + tempF + " F");
				} catch (NumberFormatException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				break;
			case 'f':
			case 'F':
				Console.log("Loop Frequency: " + frequency + " Hz");
				break;
			case 'a':
			case 'A':
				sensors.getAccelerometer(tmpVec);
				Console.log(tmpVec.scl(9.8f) + " m/s^2");
				break;
			case 'b':
			case 'B':
				float temperature = sensors.getBarometerTemperature();
				float pressure = sensors.getBarometerPressure();
				Console.log(temperature + " C, " + pressure + " mbar");
				break;
			case 'g':
			case 'G':
				sensors.getGyroscope(tmpVec);
				Console.log(tmpVec + " deg/s");
				break;
			case 'c':
			case 'C':
				float[] a = sensors.analog;
				Console.log("A0=" + a[0] + " mV,\tA1=" + a[1] + " mV,\tA2=" + a[2] + " mV,\tA3=" + a[3] + " mV");
				break;
			case 'q':
			case 'Q':
				Console.log("Quitting.");
				System.exit(0);
			}
		}
	}
	
}