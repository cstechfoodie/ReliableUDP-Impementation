package ca.concordia.httpServer;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import org.comp445.httpReliableUDP.ReliableUDP;


public class Https {
	// request

	private ServerSocket socket;

	private ClientHttpRequest req;
	
	private ClientHttpResponse res;

	private boolean hasDebuggingMessage;

	private int port = 80;

	private String host = "localhost";

	private String pathToDir = "/";

	private boolean isConnected = false;

	public boolean isConnected() {
		return isConnected;
	}

	private void processRequest(String reqMsgFirstLine) {
		String[] args = reqMsgFirstLine.split(" ");
		sanitizeArgs(args);

		req = new ClientHttpRequest();

		if (args.length == 3 && args[0].equals("GET")) {
			req.setMethod(HttpMethod.GET);
			req.setURI(args[1]);
			req.setVersion(args[2]);
		}

		if (args.length == 3 && args[0].equals("POST")) {
			req.setMethod(HttpMethod.POST);
			req.setURI(args[1]);
			req.setVersion(args[2]);
		}
	}

	private void sanitizeArgs(String[] args) {
		for (int i = 0; i < args.length; i++) {
			args[i] = args[i].trim();
		}
	}

//	public void setupServer(String cmd) {
//		String[] args = cmd.split(" ");
//		sanitizeArgs(args);
//
//		List<String> argsList = Arrays.asList(args);
//
//		if (cmd.contains("-v")) {
//			hasDebuggingMessage = true;
//		}
//
//		if (cmd.contains("-p")) {
//			int index = argsList.indexOf("-p");
//			String h = argsList.get(index + 1);
//			port = Integer.parseInt(h);
//		}
//
//		if (cmd.contains("-d")) {
//			int index = argsList.indexOf("-d");
//			pathToDir = argsList.get(index + 1);
//			File f = new File(pathToDir);
//			if(!f.exists()) {
//				new File(pathToDir).mkdirs();				
//			}
//		}
//		connect();
//	}
//
//	private void connect() {
//		try {
//			socket = new ServerSocket(port);
//			this.isConnected = true;
//			System.out.println("Server Created Successfully with on "+ port);
//		} catch (Exception e) {
//			if (hasDebuggingMessage) {
//				System.out.println("Connection Failed with Exception.");
//				e.printStackTrace();
//			}
//		}
//	}

	public void receiveAndReply() throws IOException {
		while (true) {
			
			ReliableUDP udp = new ReliableUDP();
			BufferedReader in = null;
			String request = null;
			try {
				request = udp.receive(8007);
				InputStream replyStream = new ByteArrayInputStream(request.getBytes(StandardCharsets.UTF_8));
				in = new BufferedReader(new InputStreamReader(replyStream));
			} catch (Exception e) {
				if (hasDebuggingMessage) {
					System.out.println("Failed to create socket connection and input/output stream reader");
					e.printStackTrace();
				}
			}
			if (request != null && in != null) {
				try {
					StringBuilder bld = new StringBuilder();
					String line = null;
					int lines = 0;
					int lineCount = 0;
					while (true) {
						line = in.readLine();
					    if (line == null) break;
						lineCount++;
						if (lineCount == 1) {
							processRequest(line);
							if(req.getMethod().toString().equals("GET")) {
								//in.
								break;								
							}
						}
						 if (line.trim().length() == 0) {
							 if(lines == 0) {
								 bld = new StringBuilder();
								 lines = 1;
								 continue;								 
							 }
							 else {
								 break;
							 }
						 }
						 	bld.append(line + "\r\n");
					}
					req.setBody(bld.toString());
					
					if(req.getURI().substring(1).contains("/")) {
						res = new ClientHttpResponse();
						res.setStatusCode("401");
						res.setDescription("Unauthorized");
						res.setBody("You are not authorized to work on this directory");
						udp.send(res.toString(), new InetSocketAddress("localhost", 3000), new InetSocketAddress("localhost", 12345));
//						out.writeBytes(res.toString());
//						out.flush();
//						out.close();
					} else {
						if (req.getMethod().toString().equals("GET")) {
							if (req.getURI().length() == 1 && req.getURI().equals("/")) {
								
								File folder = new File(pathToDir);
								File[] listOfFiles = folder.listFiles();
								
								bld = new StringBuilder();
								for (int i = 0; i < listOfFiles.length; i++) {
									if (listOfFiles[i].isFile()) {
										bld.append(listOfFiles[i].getName() + ",");
									}
								}
								
								res = new ClientHttpResponse();
								res.setStatusCode("200");
								res.setDescription("OK");
								res.setBody(bld.toString());
								udp.send(res.toString(), new InetSocketAddress("localhost", 3000), new InetSocketAddress("localhost", 12345));
//								out.writeBytes(res.toString());
//								out.flush();
//								out.close();
								
							} else {
								String fileName = req.getURI().substring(1);
								
								
								File f = null;
								
								if (fileName.contains(".txt")) {
									f = new File(pathToDir + "\\" + fileName);
								} else {
									f = new File(pathToDir + "\\" + fileName + ".txt");// C:\Users\ya_hao\Downloads\foo.txt								
								}
								// .\foo.txt
								// PrintWriter filewriter = new PrintWriter(f);
								try {
									Scanner fileR = new Scanner(f);
									bld = new StringBuilder();
									while (fileR.hasNextLine() && (line = fileR.nextLine()) != null) {
										bld.append(line + "\r\n");
									}
									res = new ClientHttpResponse();
									res.setBody(bld.toString());
									udp.send(res.toString(), new InetSocketAddress("localhost", 3000), new InetSocketAddress("localhost", 12345));
//									out.writeBytes(res.toString());
//									out.flush();
//									out.close();
								} catch (Exception e) {
									res = new ClientHttpResponse();
									res.setStatusCode("404");
									res.setDescription("Not Found");
									res.setBody("Failed to read the file due to file not found");
									udp.send(res.toString(), new InetSocketAddress("localhost", 3000), new InetSocketAddress("localhost", 12345));
//									out.writeBytes(res.toString());
//									out.flush();
//									out.close();
								}
							}
						}
						
						if (req.getMethod().toString().equals("POST")) {
							String fileName = req.getURI().substring(1);
							File f = null;
							if(fileName.contains(".txt")) {
								f = new File(pathToDir + "\\" + fileName);
							}
							else {
								f = new File(pathToDir + "\\" + fileName + ".txt");// C:\Users\ya_hao\Downloads\foo.txt							
							}
							// .\foo.txt
							try {
								PrintWriter filewriter = new PrintWriter(f);
								filewriter.print(req.getBody());
								filewriter.flush();
								filewriter.close();
								System.out.print(req.getBody());
								res = new ClientHttpResponse();
								res.setStatusCode("200");
								res.setDescription("OK");
								res.setBody("Sucessfully create and write into the file");
								udp.send(res.toString(), new InetSocketAddress("localhost", 3000), new InetSocketAddress("localhost", 12345));
//								out.writeBytes(res.toString());
//								out.flush();
//								out.close();
							} catch (Exception e) {
								res = new ClientHttpResponse();
								res.setStatusCode("500");
								res.setDescription("Internal Server Error");
								res.setBody("Failed to create the file");
								udp.send(res.toString(), new InetSocketAddress("localhost", 3000), new InetSocketAddress("localhost", 12345));
//								out.writeBytes(res.toString());
//								out.flush();
//								out.close();
							}
						}
					}


				} catch (IOException e) {
					e.printStackTrace();
				} finally {
					in.close();
				}
			}
		}

	}
}
