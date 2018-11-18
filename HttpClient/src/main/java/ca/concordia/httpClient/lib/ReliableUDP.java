package ca.concordia.httpClient.lib;

import static java.nio.channels.SelectionKey.OP_READ;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ReliableUDP {

	private static final Logger logger = LoggerFactory.getLogger(ReliableUDP.class);

	public static boolean handShakeDone = false;

	private static int[] acks;

	private static Packet[] spackets;

	private static Packet[] rpackets;

	private static int packetnumber;

	public static int byteSize(String payload) {
		return payload.getBytes().length;
	}

	private static int packetNumber(int totalPayloadSize) {
		return totalPayloadSize / Packet.PAYLOAD_LEN + 1;
	}

	private static int packetNumber(byte[] payload) {
		return payload.length / Packet.PAYLOAD_LEN + 1;
	}

	public static String sendAndReceiveForPost(String payload, SocketAddress routerAddr, InetSocketAddress serverAddr) {
		try {
			byte[] payloadBytes = payload.getBytes("UTF-8");
			int payloadLen = payloadBytes.length;
			int packetnumber = packetNumber(payloadBytes); // including HandShake
			
			
			acks = new int[packetnumber];
			rpackets = new Packet[packetnumber];
			spackets = new Packet[packetnumber];
			
			
			createPackets(payload, packetnumber, routerAddr, serverAddr);
			
			handShake(packetnumber, routerAddr, serverAddr);
			
			if(!handShakeDone) {
				return null;
			}
			
			Selector selector = Selector.open();
			for(int i = 0; i < packetnumber; i++) {
				DatagramChannel channel = DatagramChannel.open();
				channel.configureBlocking(false);
				channel.connect(routerAddr);
				channel.register(selector, OP_READ);
				
				SenderThread sender = new SenderThread(spackets[i], channel, acks);
				sender.start();
				logger.info("Sending \"{}\" to router at {}", payload, routerAddr);				
			}

			logger.info("Waiting for the response");
			
			int packetCounter = 0;
			int timeoutTimes = 0;
			while(true) {
				selector.select(5000);
				Set<SelectionKey> keys = selector.selectedKeys();
				if (keys.isEmpty() && timeoutTimes == 3) {
					logger.error("No response after timeout");
					keys.clear();
					break;
				}
				if(keys.isEmpty()) {
					timeoutTimes++;
					keys.clear();
				} else {
					SelectionKey[] keyArr = (SelectionKey[]) keys.toArray();
					ByteBuffer buf = null;
					Packet resp = null;
					DatagramChannel channel = null;
					Object o = new Object();
					for(int i = 0; i < keyArr.length; i++) {
						channel = (DatagramChannel) keyArr[i].channel();
						buf = ByteBuffer.allocate(Packet.MAX_LEN);
						SocketAddress router = channel.receive(buf);
						buf.flip();
						resp = Packet.fromBuffer(buf);
						synchronized (o) {
							acks[(int) (resp.getSequenceNumber() - 1)] = 1;
						}
						rpackets[(int) (resp.getSequenceNumber() - 1)] = resp;
						packetCounter++;
						if(packetCounter == packetnumber) {
							break;
						}
					}
					keys.clear();
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return compileResponse();
	}

	private static String compileResponse() {
		return rpackets.toString();
	}
	
	private static void createPackets(String payload, int packetNumber,SocketAddress routerAddr, InetSocketAddress serverAddr) {
		Packet p = null;
		String singlePayload = "";
		for(int i = 0; i < packetNumber; i++) {
			if(i != packetNumber - 1) {
				singlePayload = payload.substring(i * 1013, (i+1)*1013);				
			}
			else {
				singlePayload = payload.substring(i * 1013);
			}
			try {
				p = new Packet.Builder().setType(0).setSequenceNumber(i+1).setPortNumber(serverAddr.getPort())
						.setPeerAddress(serverAddr.getAddress()).setPayload(singlePayload.getBytes("UTF-8")).create();
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			spackets[i] = p;	
		}
	}
	
	//SYN(client) --> SYN-ACK(server) --> ACK(client) --> ACK-ACK(server)
	private static void handShake(int packetNumber, SocketAddress routerAddr, InetSocketAddress serverAddr) throws IOException {
		Selector selector = Selector.open();
		DatagramChannel channel = DatagramChannel.open();
		
		Packet p = new Packet.Builder().setType(1).setSequenceNumber(1L).setPortNumber(serverAddr.getPort())
		.setPeerAddress(serverAddr.getAddress()).setPayload(("SYN"+ packetNumber).getBytes("UTF-8")).create();
		channel.configureBlocking(false);
		SelectionKey key = channel.register(selector, OP_READ);
		
		String payload = "";
		while(true) {
			channel.send(p.toBuffer(), routerAddr);
			logger.info("Sending \"{}\" to router at {}", "SYN", routerAddr);
			
			
			logger.info("Waiting for the response for first handshake request");
			selector.select(5000);
			
			Set<SelectionKey> keys = selector.selectedKeys();
			if(keys.isEmpty()){
				logger.error("No response for first handshakes after timeout. Will resend again");
				continue;
			}
			
			// We just want a single response.
			ByteBuffer buf = ByteBuffer.allocate(Packet.MAX_LEN);
			SocketAddress router = channel.receive(buf);
			buf.flip();
			Packet resp = Packet.fromBuffer(buf);
			logger.info("Packet: {}", resp);
			logger.info("Router: {}", router);
			payload = new String(resp.getPayload(), StandardCharsets.UTF_8);
			logger.info("Payload: {}",  payload);
			
			keys.clear();
			
			if(payload.trim().equals("SYN-ACK")) {
				break;
			}
			
		}
		

		if(payload.trim().equals("SYN-ACK")) {
			p = new Packet.Builder().setType(2).setSequenceNumber(1L).setPortNumber(serverAddr.getPort())
					.setPeerAddress(serverAddr.getAddress()).setPayload("ACK".getBytes("UTF-8")).create();
			
			logger.info("Sending \"{}\" to router at {}", "SYN", routerAddr);
			channel.send(p.toBuffer(), routerAddr);
			
			
	        logger.info("Waiting for the response for first handshake request");
	        selector.select(5000);

	        Set<SelectionKey> keys = selector.selectedKeys();
	        if(keys.isEmpty()){
	        	handShakeDone = true;
	            logger.error("No response for second handshakes after timeout");
	            return;
	        }

	        // We just want a single response.
	        ByteBuffer buf = ByteBuffer.allocate(Packet.MAX_LEN);
	        SocketAddress router = channel.receive(buf);
	        buf.flip();
	        Packet resp = Packet.fromBuffer(buf);
	        logger.info("Packet: {}", resp);
	        logger.info("Router: {}", router);
	        payload = new String(resp.getPayload(), StandardCharsets.UTF_8);
	        logger.info("Payload: {}",  payload);

	        keys.clear();
	        
	        if(payload.trim().equals("ACK-ACK") || payload.trim().equals("SYN-ACK")) {
	        	handShakeDone = true;
	        }
		}
		
		key.cancel();
		channel.close();
		selector.close();
	}

}
