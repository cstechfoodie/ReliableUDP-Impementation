package ca.concordia.httpClient.lib;

import java.nio.channels.DatagramChannel;

public class SenderThread extends Thread {

	
	public SenderThread(Packet sentPacket, DatagramChannel c, int[] acks) {
		
	}
	
	public void run() {
		boolean isAcked = false;
		while(!isAcked) {
			//sent through channel
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			//check received - scks = 1
			//if true --  isAcked = true;
			try {
				this.join();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			
		}
	}
}
