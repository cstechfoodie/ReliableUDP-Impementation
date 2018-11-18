package ca.concordia.httpClient.lib;

import java.io.IOException;
import java.nio.channels.DatagramChannel;

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
				channel.write(sentPacket.toBuffer());
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			if(acks[(int) sentPacket.getSequenceNumber()] == 1) {
				isAcked = true;
				try {
					this.join();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
}
