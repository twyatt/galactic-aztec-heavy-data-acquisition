package edu.sdsu.rocket.core.net;

import java.net.SocketAddress;

public class DatagramMessage {
	
	public static final byte PING    = 0x0;
	public static final byte SENSORS = 0x1;
	
	public SocketAddress address;
	public int number;
	public byte id;
	public byte[] data;
	
}
