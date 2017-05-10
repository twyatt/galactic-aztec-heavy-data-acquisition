package edu.sdsu.rocket.core.net;

import edu.sdsu.rocket.core.helpers.RateLimitedRunnable;
import edu.sdsu.rocket.core.models.Sensors;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;

public class SensorClient {

    private static final byte SENSORS_MASK = Sensors.ALL_MASK;

    public interface SensorClientListener {
        void onSensorsUpdated(byte mask);
        void onPingResponse(long latency);
    }
    
    private static final int BUFFER_SIZE = 1024; // bytes
    private final ByteBuffer MESSAGE_BUFFER = ByteBuffer.allocate(BUFFER_SIZE);
    private final ByteBuffer PING_BUFFER = ByteBuffer.allocate(BUFFER_SIZE);

    private DatagramClient client;
    
    private Thread thread;
    private float frequency;
    private RateLimitedRunnable runnable;

    private final AtomicInteger requestNumber = new AtomicInteger(); // message request number
    private int responseNumber; // message response number

    private SensorClientListener listener;
    
    private final Sensors sensors;

    public SensorClient(Sensors sensors) {
        this.sensors = sensors;
    }
    
    public void setListener(SensorClientListener listener) {
        this.listener = listener;
    }

    public void setFrequency(float frequency) {
        this.frequency = frequency;
        if (runnable != null) {
            if (frequency == 0) {
                pause();
            } else {
                runnable.setFrequency(frequency);
                resume();
            }
        }
    }
    
    public void start(InetAddress addr, int port) throws SocketException {
        start(new InetSocketAddress(addr, port));
    }
    
    public void start(InetSocketAddress address) throws SocketException {
        if (client != null) {
            throw new SocketException("Client already started");
        }
        
        client = new DatagramClient(address);
        client.setListener(new DatagramMessageHandler() {
            @Override
            public void onMessageReceived(DatagramMessage message) {
                switch (message.id) {
                case DatagramMessage.PING:
                    onPingResponse(message);
                    break;
                case DatagramMessage.SENSORS:
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
                    sendSensorRequest(SENSORS_MASK);
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
        PING_BUFFER.putInt(requestNumber.incrementAndGet()); // ++requestNumber
        PING_BUFFER.put(DatagramMessage.PING);
        PING_BUFFER.putLong(System.nanoTime());
        
        client.send(PING_BUFFER.array(), PING_BUFFER.position());
    }

    public void sendStatusRequest() throws IOException {
        sendSensorRequest(Sensors.SYSTEM_MASK); // status request is simply a masked sensor request
    }
    
    public void sendSensorRequest(byte mask) throws IOException {
        sendMessage(DatagramMessage.SENSORS, mask);
    }
    
    public void sendMessage(byte id) throws IOException {
        sendMessage(id, null);
    }
    
    public void sendMessage(byte id, byte data) throws IOException {
        sendMessage(id, new byte[] { data });
    }
    
    synchronized public void sendMessage(byte id, byte[] data) throws IOException {
        MESSAGE_BUFFER.clear();
        MESSAGE_BUFFER.putInt(requestNumber.incrementAndGet()); // ++requestNumber
        MESSAGE_BUFFER.put(id);
        if (data != null) {
            MESSAGE_BUFFER.put(data);
        }
        
        client.send(MESSAGE_BUFFER.array(), MESSAGE_BUFFER.position());
    }
    
    protected void onPingResponse(DatagramMessage message) {
        try {
            ByteBuffer buf = ByteBuffer.wrap(message.data);
            long latency = System.nanoTime() - buf.getLong(); // round-trip time in nanoseconds
            if (listener != null) {
                listener.onPingResponse(latency);
            }
        } catch (BufferUnderflowException e) {
            System.err.println(e);
        }
    }
    
    protected void onSensorData(DatagramMessage message) {
        if (message.number != 0) {
            if (message.number < responseNumber || message.number > requestNumber.get()) {
                return; // drop packet
            } else {
                responseNumber = message.number;
            }
        }
        
        try {
            ByteBuffer buffer = ByteBuffer.wrap(message.data);
            byte mask = buffer.get();
            sensors.fromByteBuffer(buffer, mask);
            if (listener != null) {
                listener.onSensorsUpdated(mask);
            }
        } catch (BufferUnderflowException e) {
            System.err.println(e);
        }
    }
    
}
