package ca.concordia.httpClient;

import java.util.Scanner;

import ca.concordia.httpClient.lib.Httpc;

/**
 * Hello world!
 *
 */
public class ConsoleApp 
{
    public static void main( String[] args )
    {
        
        System.out.println("Welcome to use Httpc. Enter 'httpc help' to check usage.");
		Scanner scanner = new Scanner(System.in);
		Httpc httpc = new Httpc();
		
		
		boolean inProcess = true;
		
		while(inProcess) {
			try {
				String cmd = scanner.nextLine().trim();
				httpc.commandLineParser(cmd);
				if(httpc.isConnected()) {
					httpc.displayResult();
				}
			} catch (Exception e) {
				System.out.println("Error cmd message. Please check 'httpc help'");
			}
			
		}
		
		scanner.close();
    }
}
