package ca.concordia.httpServer;

import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.DatagramChannel;
import java.nio.charset.StandardCharsets;

public class ReliableUDP {

	private static final Logger logger = LoggerFactory.getLogger(ReliableUDP.class);

	public static boolean handShakeDone = false;

	private static int[] acks;

	private static Packet[] rpackets;
	
	private static int packetnumber;

	private static String payload;
	
	private static int packetIndex = 0;

	// receive syn, buffer; send syn-ack; receive ack; receive packet; check
	// repeat/upload if full
	public String receiveAndReply(int port) throws IOException {

		try (DatagramChannel channel = DatagramChannel.open()) {
			channel.bind(new InetSocketAddress(port));
			logger.info("EchoServer is listening at {}", channel.getLocalAddress());
			ByteBuffer buf = ByteBuffer.allocate(Packet.MAX_LEN).order(ByteOrder.BIG_ENDIAN);

			for (;;) {
				buf.clear();
				SocketAddress router = channel.receive(buf);

				// Parse a packet from the received raw data.
				buf.flip();
				Packet packet = Packet.fromBuffer(buf);
				buf.flip();

				payload = new String(packet.getPayload(), StandardCharsets.UTF_8);
				logger.info("Packet: {}", packet);
				logger.info("Payload: {}", payload);
				logger.info("Router: {}", router);


				// SYN(client) --> SYN-ACK(server) --> ACK(client) --> ACK-ACK(server)
				if (payload.trim().substring(0, 2).equals("SYN")) {
					packetnumber = Integer.parseInt(payload.trim().substring(3));
					rpackets = new Packet[packetnumber];
					acks = new int[packetnumber];
					
					Packet resp = packet.toBuilder().setPayload(("SYN-ACK"+(packetnumber+1)).getBytes()).create();
					logger.info("Sending \"{}\" to router at {}", "SYN-ACK", router);
					channel.send(resp.toBuffer(), router);
					handShakeDone = true;
				}
				
				if (payload.trim().substring(0, 2).equals("ACK")) {
					Packet resp = packet.toBuilder().setPayload("ACK-ACK".getBytes()).create();
					logger.info("Sending \"{}\" to router at {}", "ACK-ACK", router);
					channel.send(resp.toBuffer(), router);
					handShakeDone = true;
				}
				
				if(!handShakeDone) {

					return null;
				}
				
				if(rpackets.length != packetnumber) {
					for(int i=0;i<rpackets.length;i++) {
						if(rpackets[i]==packet) {
							break;
						}
					}
					packetIndex = (int) (packet.getSequenceNumber());
					rpackets[packetIndex]=packet;
					acks[packetIndex - 1] = 1;
					Packet resp = packet.toBuilder().setPayload(("ACK"+(packetIndex+1)).getBytes()).create();
					logger.info("Sending \"{}\" to router at {}", "ACK", router);
					channel.send(resp.toBuffer(), router);
				}
				else {
					//upload packet 					
				}
				
				
				
				
				

			}

		}
		//return compileResponse;
	}
	
	private String compileResponse() {
		StringBuilder str = new StringBuilder();
		String payload = "";
		for(int i=0; i<rpackets.length;i++) {
			payload = new String(rpackets[i].getPayload(), StandardCharsets.UTF_8);
		    str.append(payload);
		}
		return str.toString();
	}
	
}
