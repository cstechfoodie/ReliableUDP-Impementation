package org.comp445.httpReliableUDP;

import static java.nio.channels.SelectionKey.OP_READ;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReliableUDP {

	private static final Logger logger = LoggerFactory.getLogger(ReliableUDP.class);

	public boolean handShakeDone = false;
	
	private Packet lastPacktReceived = null;
	
	private DatagramChannel disconnectChannel = null;

	private int[] acks;

	private Packet[] spackets;

	private Packet[] rpackets;

	private int packetnumber = -1;

	public int byteSize(String payload) {
		return payload.getBytes().length;
	}

	private int packetNumber(byte[] payload) {
		return payload.length % Packet.PAYLOAD_LEN > 0 ? payload.length / Packet.PAYLOAD_LEN + 1 : payload.length / Packet.PAYLOAD_LEN;
	}

	//localhost 3000 8007
	public void send(String payload, SocketAddress routerAddr, InetSocketAddress serverAddr) {
		try {
			byte[] payloadBytes = payload.getBytes("UTF-8");
			int packetnumber = packetNumber(payloadBytes); // including HandShake

			acks = new int[packetnumber];
			spackets = new Packet[packetnumber];
			
			if(lastPacktReceived != null) {
				InetAddress ip = lastPacktReceived.getPeerAddress();
				int port = lastPacktReceived.getPeerPort();
				InetSocketAddress addr = new InetSocketAddress(ip, port);
				serverAddr = addr;
				createPackets(payload.trim(), packetnumber, routerAddr, serverAddr);
			}
			else {
				createPackets(payload.trim(), packetnumber, routerAddr, serverAddr);
			}
			
			handShakeWithServer(packetnumber, routerAddr, serverAddr);				
			


			Selector selector = Selector.open();
			for (int i = 0; i < packetnumber; i++) {
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
			while (true) {
				selector.select(5000);
				Set<SelectionKey> keys = selector.selectedKeys();
				if (!keys.isEmpty()) {
					Iterator<SelectionKey> iterKeys = keys.iterator();
					//SelectionKey[] keyArr = (SelectionKey[]) keys.toArray();
					ByteBuffer buf = null;
					Packet resp = null;
					DatagramChannel channel = null;
					while(iterKeys.hasNext()) {
						channel = (DatagramChannel) iterKeys.next().channel();
						buf = ByteBuffer.allocate(Packet.MAX_LEN);
						channel.receive(buf);
						buf.flip();
						resp = Packet.fromBuffer(buf);
						if(resp.getType() == 0 && acks[(int) (resp.getSequenceNumber() - 1)] == 0) {
							acks[(int) (resp.getSequenceNumber() - 1)] = 1;
							packetCounter++;							
						}
						if (packetCounter == packetnumber) {
							break;
						}
						if(resp.getType() == 3) {
							fillAcks();
							break;
						}
					}
					if (packetCounter == packetnumber || resp.getType() == 3) {
						break;
					}
					keys.clear();
				}			
			}
			disconnetHandshake(routerAddr, serverAddr);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void fillAcks() {
		for(int i = 0; i < acks.length; i++) {
			acks[i] = 1;
		}
		
	}

	private void createPackets(String payload, int packetNumber, SocketAddress routerAddr,
			InetSocketAddress serverAddr) {
		Packet p = null;
		String singlePayload = "";
		for (int i = 0; i < packetNumber; i++) {
			if (i != packetNumber - 1) {
				singlePayload = payload.substring(i * 900, (i + 1) * 900);
			} else {
				singlePayload = payload.substring(i * 900);
			}
			try {
				p = new Packet.Builder().setType(0).setSequenceNumber(i + 1).setPortNumber(serverAddr.getPort())
						.setPeerAddress(serverAddr.getAddress()).setPayload(singlePayload.getBytes("UTF-8")).create();
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			spackets[i] = p;
		}
	}

	// SYN(client) --> SYN-ACK(server) --> ACK(client) --> ACK-ACK(server)
	private void handShakeWithServer(int packetNumber, SocketAddress routerAddr, InetSocketAddress serverAddr)
			throws IOException {
		Selector selector = Selector.open();
		DatagramChannel channel = DatagramChannel.open();

		Packet p = null;
		if(lastPacktReceived != null) {
			p = lastPacktReceived.toBuilder().setType(1).setSequenceNumber(1L).setPayload(("SYN" + packetNumber).getBytes("UTF-8")).create();
			lastPacktReceived = null;
		}
		else {
			p = new Packet.Builder().setType(1).setSequenceNumber(1L).setPortNumber(serverAddr.getPort())
					.setPeerAddress(serverAddr.getAddress()).setPayload(("SYN" + packetNumber).getBytes("UTF-8")).create();			
		}
		
		channel.configureBlocking(false);
		SelectionKey key = channel.register(selector, OP_READ);

		String payload = "";
		while (true) {
			
			//keep send SYN until one SYN-ACK arrives
			channel.send(p.toBuffer(), routerAddr);
			logger.info("Sending \"{}\" to router at {}", "SYN", routerAddr);

			logger.info("Waiting for the response for first handshake request");
			selector.select(5000);

			Set<SelectionKey> keys = selector.selectedKeys();
			if (keys.isEmpty()) {
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
			logger.info("Payload: {}", payload);

			keys.clear();

			if (payload.trim().contains("SYN-ACK")) {
				p = new Packet.Builder().setType(2).setSequenceNumber(1L).setPortNumber(serverAddr.getPort())
						.setPeerAddress(serverAddr.getAddress()).setPayload("ACK".getBytes("UTF-8")).create();

				logger.info("Sending \"{}\" to router at {}", "SYN", routerAddr);
				channel.send(p.toBuffer(), routerAddr);
				break;
			}
		}
		channel.close();
		selector.close();
	}
	
	private void disconnetHandshake(SocketAddress routerAddr, InetSocketAddress serverAddr) throws IOException {
		Selector selector = Selector.open();
		DatagramChannel channel = DatagramChannel.open();

		Packet p = new Packet.Builder().setType(4).setSequenceNumber(1L).setPortNumber(serverAddr.getPort())
				.setPeerAddress(serverAddr.getAddress()).setPayload(("FIN-ACK").getBytes("UTF-8")).create();
		channel.configureBlocking(false);
		SelectionKey key = channel.register(selector, OP_READ);

		String payload = "";
		int time = 0;
		while (true) {
			
			//keep send SYN until one SYN-ACK arrives
			channel.send(p.toBuffer(), routerAddr);
			logger.info("Sending \"{}\" to router at {}", "FIN-ACK", routerAddr);

			logger.info("Waiting for the response for disconnect handshake request");
			selector.select(100);

			Set<SelectionKey> keys = selector.selectedKeys();
			
			if (!keys.isEmpty()) {
				Iterator<SelectionKey> iterKeys = keys.iterator();
				ByteBuffer buf = null;
				Packet resp = null;
				DatagramChannel revchannel = null;
				while(iterKeys.hasNext()) {
					revchannel = (DatagramChannel) iterKeys.next().channel();
					buf = ByteBuffer.allocate(Packet.MAX_LEN);
					revchannel.receive(buf);
					buf.flip();
					resp = Packet.fromBuffer(buf);
					if(resp.getType() == 3) {
						channel.send(p.toBuffer(), routerAddr);
					}
					if(resp.getType() == 1) {
						disconnectChannel = channel;
						break;
					}
				}
				if(disconnectChannel != null) {
					selector.close();
					keys.clear();
					break;
				}
			} else {
				time++;
				logger.error("No response for first handshakes after timeout. Will resend again");
				if (time < 10) {
					continue;
				} else {
					disconnectChannel = channel;
					selector.close();
					keys.clear();
					break;
				}
			}

//			if (keys.isEmpty()) {
//				time++;
//				logger.error("No response for first handshakes after timeout. Will resend again");
//				if (time < 10) {
//					continue;
//				} else {
//					break;
//				}
//				continue;
//			} else {
//				
//			}
			
		}
	}
	
	

	public String receive(int port) {
		try {
			DatagramChannel channel = null;
			if(disconnectChannel != null) {
				channel = disconnectChannel;
				
				disconnectChannel = null;
			} else {
				channel = DatagramChannel.open();
				channel.bind(new InetSocketAddress("localhost", port));
			}
			
			channel.configureBlocking(true);
			String payload = "";
			int revPacketCounter = 0;
			logger.info("HttpFileServer is listening at {}", channel.getLocalAddress());
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

				if (packet.getType() == 0 && rpackets[(int) (packet.getSequenceNumber() - 1)] == null) {
					rpackets[(int) (packet.getSequenceNumber() - 1)] = packet;
					revPacketCounter++;
					Packet resp = packet.toBuilder().setPayload(("ACK").getBytes("UTF-8")).create();
					channel.send(resp.toBuffer(), router);
				} else if (packet.getType() == 0 && rpackets[(int) (packet.getSequenceNumber() - 1)] != null){
					Packet resp = packet.toBuilder().setPayload(("ACK").getBytes("UTF-8")).create();
					channel.send(resp.toBuffer(), router);
				} else if(packet.getType() == 1) {
					Packet resp = packet.toBuilder().setPayload(("SYN-ACK").getBytes("UTF-8")).create();
					if(packetnumber < 0) {
						this.packetnumber = Integer.parseInt(payload.trim().substring(3));
						this.rpackets = new Packet[packetnumber];						
					}
					logger.info("Sending \"{}\" to router at {}", "SYN-ACK", router);
					channel.send(resp.toBuffer(), router);
				}
				else {
					continue;
				}
				if (revPacketCounter == this.packetnumber) {
					while (true) {
						Packet resp = packet.toBuilder().setType(3).setPayload(("FIN").getBytes("UTF-8")).create();
						logger.info("Sending \"{}\" to router at {}", "FIN", router);
						channel.send(resp.toBuffer(), router);
						
						buf.clear();
						channel.receive(buf);

						// Parse a packet from the received raw data.
						buf.flip();
						packet = Packet.fromBuffer(buf);
						buf.flip();
						
						payload = new String(packet.getPayload(), StandardCharsets.UTF_8);
						if(packet.getType() == 4 && payload.contains("FIN-ACK")) {
							lastPacktReceived = packet;
							break;
						} else {
							continue;
						}
					}

					break;
				}
			}
			channel.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		handShakeDone = false;

		return compileResponse();
	}

	private String compileResponse() {
		StringBuilder str = new StringBuilder();
		String payload = "";
		for (int i = 0; i < rpackets.length; i++) {
			payload = new String(rpackets[i].getPayload(), StandardCharsets.UTF_8).trim();
			str.append(payload);
		}
		return str.toString();
	}

}
