package ca.concordia.httpClient.lib;

import static java.nio.channels.SelectionKey.OP_READ;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.SelectorProvider;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ReliableUDP {
	
  private static final Logger logger = LoggerFactory.getLogger(ReliableUDP.class);
	
  public boolean handShakeDone = false;
  
  private static int[] acks;
  
  private static Packet[] spackets;
  
  private static Packet[] rpackets;
  
  
  
  public static int byteSize(String payload) {
	  return payload.getBytes().length;
  }
  
  private static int packetNumber(int totalPayloadSize) {
	  return totalPayloadSize/Packet.PAYLOAD_LEN + 1;
  }
  
  private int packetNumber(byte[] payload) {
	  return payload.length/Packet.PAYLOAD_LEN + 1;
  }
  
  public static String sendAndReceiveForPost(String payload, SocketAddress routerAddr, InetSocketAddress serverAddr) {
	  try(DatagramChannel channel = DatagramChannel.open()){
          String msg = "Hello World";
          
          int packetnumber = packetNumber(byteSize(payload) + 2); // including HandShake
          acks = new int[packetnumber];
          rpackets = new Packet[packetnumber];
          spackets = new Packet[packetnumber];
          Selector selector  = Selector.open();
          
          
          channel.configureBlocking(false);
          channel.register(selector, OP_READ);
          
          
          Packet p = new Packet.Builder()
                  .setType(0)
                  .setSequenceNumber(1L)
                  .setPortNumber(serverAddr.getPort())
                  .setPeerAddress(serverAddr.getAddress())
                  .setPayload(msg.getBytes())
                  .create();
          channel.send(p.toBuffer(), routerAddr);

          logger.info("Sending \"{}\" to router at {}", msg, routerAddr);
          
          SenderThread a = new SenderThread(p, channel, acks);
          a.start();
          //WorkerThread d = new WorkerTread(Packet, Channel...);
          

          // Try to receive a packet within timeout.
          
          
          logger.info("Waiting for the response");
          selector.select(5000);

          Set<SelectionKey> keys = selector.selectedKeys();

          if(keys.isEmpty()){
              logger.error("No response after timeout");
              return "";
          }

          // We just want a single response.
          ByteBuffer buf = ByteBuffer.allocate(Packet.MAX_LEN);
          SocketAddress router = channel.receive(buf);
          buf.flip();
          Object o = "asda";
          Packet resp = Packet.fromBuffer(buf);
          synchronized (o){
        	  acks[(int) (resp.getSequenceNumber() -1)] = 1;
          }
          logger.info("Packet: {}", resp);
          logger.info("Router: {}", router);
          payload = new String(resp.getPayload(), StandardCharsets.UTF_8);
          logger.info("Payload: {}",  payload);

          keys.clear();
      } catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	  return "";
  }
  
  
  public String compileResponse() {
	  return rpackets.toString();
  }
  
}
