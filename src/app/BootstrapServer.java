package app;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

public class BootstrapServer {

	private volatile boolean working = true;
	private List<ServentInfoBootstrap> activeServents;
	private int currId = 0;
	
	private class CLIWorker implements Runnable {
		@Override
		public void run() {
			Scanner sc = new Scanner(System.in);
			
			String line;
			while(true) {
				line = sc.nextLine();
				
				if (line.equals("stop")) {
					working = false;
					break;
				}
			}
			
			sc.close();
		}
	}
	
	public BootstrapServer() {
		activeServents = new ArrayList<>();
	}
	
	public void doBootstrap(int bsPort) {
		Thread cliThread = new Thread(new CLIWorker());
		cliThread.start();
		
		ServerSocket listenerSocket = null;
		try {
			listenerSocket = new ServerSocket(bsPort);
			listenerSocket.setSoTimeout(1000);
		} catch (IOException e1) {
			AppConfig.timestampedErrorPrint("Problem while opening listener socket.");
			System.exit(0);
		}
		
		Random rand = new Random(System.currentTimeMillis());
		
		while (working) {
			try {
				Socket newServentSocket = listenerSocket.accept();
				
				 /* 
				 * Handling these messages is intentionally sequential, to avoid problems with
				 * concurrent initial starts.
				 * 
				 * In practice, we would have an always-active backbone of servents to avoid this problem.
				 */
				
				Scanner socketScanner = new Scanner(newServentSocket.getInputStream());
				String message = socketScanner.nextLine();
				
				/*
				 * New servent has hailed us. He is sending us his own listener port.
				 * He wants to get a listener port from a random active servent,
				 * or -1 if he is the first one.
				 */
				if (message.equals("Hail")) {
					String[] reply = socketScanner.nextLine().split(",");
					String ipAddress = reply[0];
					int newServentPort = Integer.parseInt(reply[1]);
					
					System.out.println("got " + ipAddress + ":" + newServentPort);
					PrintWriter socketWriter = new PrintWriter(newServentSocket.getOutputStream());
					
					if (activeServents.size() == 0) {
						socketWriter.write("-1," + String.valueOf(-1) + "," + currId + "\n");
//						activeServents.add(newServentPort);
						activeServents.add(new ServentInfoBootstrap(ipAddress, newServentPort)); //first one doesn't need to confirm
					} else {
//						int randServent = activeServents.get(rand.nextInt(activeServents.size()));
						ServentInfoBootstrap randServent = activeServents.get(rand.nextInt(activeServents.size()));
//						ServentInfoBootstrap randServent = activeServents.get(0);
						String randServentIp = randServent.getIpAddress();
						int randServentPort = randServent.getListenerPort();
						currId++;
						socketWriter.write(randServentIp + "," + String.valueOf(randServentPort) + "," + currId + "\n");
					}
					
					socketWriter.flush();
					newServentSocket.close();
				} else if (message.equals("New")) {
					/**
					 * When a servent is confirmed not to be a collider, we add him to the list.
					 */
					String[] reply = socketScanner.nextLine().split(",");
					String ipAddress = reply[0];
					int newServentPort = Integer.parseInt(reply[1]);
//					int newServentPort = socketScanner.nextInt();
					
					System.out.println("adding " + ipAddress + " : " + newServentPort);

//					activeServents.add(newServentPort);
					activeServents.add(new ServentInfoBootstrap(ipAddress, newServentPort));
					AppConfig.timestampedStandardPrint(activeServents.toString());
					newServentSocket.close();
				} else if (message.equals("Left")) {
					String[] reply = socketScanner.nextLine().split(",");
					String ipAddress = reply[0];
					int newServentPort = Integer.parseInt(reply[1]);
//					String ip = newServentSocket.getLocalAddress().getHostAddress();
//					int port = socketScanner.nextInt();

					AppConfig.timestampedStandardPrint(ipAddress + " : " + newServentPort + " leaving");

					activeServents.remove(new ServentInfoBootstrap(ipAddress, newServentPort));
					AppConfig.timestampedStandardPrint(activeServents.toString());
					newServentSocket.close();
				}
				
			} catch (SocketTimeoutException e) {
				
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Expects one command line argument - the port to listen on.
	 */
	public static void main(String[] args) {
		if (args.length != 1) {
			AppConfig.timestampedErrorPrint("Bootstrap started without port argument.");
		}
		
		int bsPort = 0;
		try {
			bsPort = Integer.parseInt(args[0]);
		} catch (NumberFormatException e) {
			AppConfig.timestampedErrorPrint("Bootstrap port not valid: " + args[0]);
			System.exit(0);
		}
		
		AppConfig.timestampedStandardPrint("Bootstrap server started on port: " + bsPort);
		
		BootstrapServer bs = new BootstrapServer();
		bs.doBootstrap(bsPort);
	}
}
