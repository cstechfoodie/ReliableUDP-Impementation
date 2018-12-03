package org.comp445.httpReliableUDP;

import java.io.IOException;
import java.nio.channels.DatagramChannel;
import java.nio.charset.Charset;

public class SenderThread extends Thread {
	
	private Packet sentPacket;
	
	private DatagramChannel channel;
	
	private int[] acks;

	
	public SenderThread(Packet sentPacket, DatagramChannel channel, int[] acks) {
		this.sentPacket = sentPacket;
		this.channel = channel;
		this.acks = acks;
	}
	
	public void run() {
		boolean isAcked = false;
		while(!isAcked) {
			//sent through channel
			try {
				System.out.println("---------------------------------------------------------------------------");
				System.out.println(new String(sentPacket.getPayload(), Charset.forName("UTF-8")));
				System.out.println(sentPacket.getPayload().length);
				System.out.println("---------------------------------------------------------------------------");
				channel.write(sentPacket.toBuffer());
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			if(acks[(int) sentPacket.getSequenceNumber()-1] == 1) {
				isAcked = true;
				try {
					channel.close();
					this.join();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
}
