package edu.sdsu.rocket.core.net;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

import edu.sdsu.rocket.core.helpers.RateLimitedRunnable;
import edu.sdsu.rocket.core.models.Sensors;

public class SensorClient {
	
	public enum Mode {
		LOCAL (DatagramMessage.SENSORS_LOCAL), // default
		REMOTE(DatagramMessage.SENSORS_REMOTE),
		BOTH  (DatagramMessage.SENSORS_BOTH),
		;
		byte value;
		Mode(byte value) {
			this.value = value;
		}
	}
	private Mode mode = Mode.LOCAL;
	
	public interface SensorClientListener {
		public void onSensorsUpdated();
		public void onPingResponse(long latency);
	}
	
	private static final int BUFFER_SIZE = 1024; // bytes
	private final ByteBuffer MESSAGE_BUFFER = ByteBuffer.allocate(BUFFER_SIZE);
	private final ByteBuffer PING_BUFFER = ByteBuffer.allocate(BUFFER_SIZE);
	
	private DatagramClient client;
	
	private Thread thread;
	private float frequency;
	private RateLimitedRunnable runnable;
	
	private int requestNumber; // message request number
	private int responseNumber; // message response number
	private long latency; // round-trip time in nanoseconds
	
	private SensorClientListener listener;
	
	private final Sensors local;
	private final Sensors remote;
	
	public SensorClient(Sensors local, Sensors remote) {
		this.local = local;
		this.remote = remote;
	}
	
	public void setListener(SensorClientListener listener) {
		this.listener = listener;
	}
	
	public void setMode(Mode mode) {
		this.mode = mode;
	}

	public void setFrequency(float frequency) {
		this.frequency = frequency;
		if (runnable != null) {
			if (frequency == 0) {
				runnable.pause();
			} else {
				runnable.setFrequency(frequency);
				runnable.resume();
			}
		}
	}
	
	public void start(InetAddress addr, int port) throws SocketException {
		start(new InetSocketAddress(addr, port));
	}
	
	public void start(InetSocketAddress address) throws SocketException {
		if (client != null) {
			throw new SocketException("Client already started.");
		}
		
		client = new DatagramClient(address);
		client.setListener(new DatagramMessageHandler() {
			@Override
			public void onMessageReceived(DatagramMessage message) {
				switch (message.id) {
				case DatagramMessage.PING:
					onPingResponse(message);
					break;
				case DatagramMessage.SENSORS_LOCAL:  // fall thru intentional
				case DatagramMessage.SENSORS_REMOTE: // fall thru intentional
				case DatagramMessage.SENSORS_BOTH:
					onSensorData(message);
					break;
				}
			}
		});
		client.start();
		
		runnable = new RateLimitedRunnable() {
			@Override
			public void loop() throws InterruptedException {
				try {
					sendSensorRequest();
				} catch (IOException e) {
					System.err.println(e);
				}
			}
		};
		setFrequency(frequency);
		
		thread = new Thread(runnable);
		thread.setName(getClass().getSimpleName());
		thread.start();
	}
	
	public void stop() {
		if (client != null) {
			client.stop();
			client = null;
		}
		
		if (thread != null) {
			thread.interrupt();
			try {
				thread.join();
			} catch (InterruptedException e) {
				System.err.println(e);
			}
			thread = null;
		}
		runnable = null;
	}
	
	public void pause() {
		runnable.pause();
	}
	
	public void resume() {
		runnable.resume();
	}
	
	public void sendPingRequest() throws IOException {
		PING_BUFFER.clear();
		PING_BUFFER.putInt(++requestNumber);
		PING_BUFFER.put(DatagramMessage.PING);
		PING_BUFFER.putLong(System.nanoTime());
		
		client.send(PING_BUFFER.array(), PING_BUFFER.position());
	}
	
	public void sendSensorRequest() throws IOException {
		if (Mode.BOTH.equals(mode)) {
			byte[] data = new byte[] {
					(byte) ((Sensors.GPS_MASK | Sensors.RADIO_MASK) & 0xFF),
					(byte) (Sensors.ALL_MASK & 0xFF),
			};
			sendMessage(mode.value, data);
		} else {
			sendMessage(mode.value);
		}
	}
	
	public void sendMessage(byte id) throws IOException {
		sendMessage(id, null);
	}
	
	public void sendMessage(byte id, byte data) throws IOException {
		sendMessage(id, new byte[] { data });
	}
	
	public void sendMessage(byte id, byte[] data) throws IOException {
		MESSAGE_BUFFER.clear();
		MESSAGE_BUFFER.putInt(++requestNumber);
		MESSAGE_BUFFER.put(id);
		if (data != null) {
			MESSAGE_BUFFER.put(data);
		}
		
		client.send(MESSAGE_BUFFER.array(), MESSAGE_BUFFER.position());
	}
	
	protected void onPingResponse(DatagramMessage message) {
		try {
			ByteBuffer buf = ByteBuffer.wrap(message.data);
			latency = System.nanoTime() - buf.getLong();
			if (listener != null) {
				listener.onPingResponse(latency);
			}
		} catch (BufferUnderflowException e) {
			System.err.println(e);
		}
	}
	
	protected void onSensorData(DatagramMessage message) {
		if (message.number != 0) {
			if (message.number < responseNumber || message.number > requestNumber) {
				return; // drop packet
			} else {
				responseNumber = message.number;
			}
		}
		
		Sensors sensors1;
		Sensors sensors2 = null;
		
		switch (message.id) {
		case DatagramMessage.SENSORS_REMOTE:
			sensors1 = remote;
			break;
		case DatagramMessage.SENSORS_BOTH:
			sensors2 = remote;
			// fall-thru intentional
		default: // SENSORS_LOCAL
			sensors1 = local;
			break;
		}
		
		try {
			ByteBuffer buffer = ByteBuffer.wrap(message.data);
			int mask1 = buffer.get();
			int mask2 = sensors2 != null ? buffer.get() : 0;
			
			sensors1.fromByteBuffer(buffer, mask1);
			if (sensors2 != null) {
				sensors2.fromByteBuffer(buffer, mask2);
			}
			
			if (listener != null) {
				listener.onSensorsUpdated();
			}
		} catch (BufferUnderflowException e) {
			System.err.println(e);
		}
	}
	
}
