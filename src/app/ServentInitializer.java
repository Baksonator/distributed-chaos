package app;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

import mutex.LamportClock;
import servent.message.NewNodeMessage;
import servent.message.util.MessageUtil;

public class ServentInitializer implements Runnable {

	private String getSomeServentIpAndPort() {
		int bsPort = AppConfig.BOOTSTRAP_PORT;
		String bsIp = AppConfig.BOOTSTRAP_IP;

//		int retVal = -2;

		String retVal = "";
		
		try {
			Socket bsSocket = new Socket(bsIp, bsPort);
			
			PrintWriter bsWriter = new PrintWriter(bsSocket.getOutputStream());
			bsWriter.write("Hail\n" + AppConfig.myServentInfo.getListenerPort() + "\n");
			bsWriter.flush();
			
			Scanner bsScanner = new Scanner(bsSocket.getInputStream());
//			retVal = bsScanner.nextInt();
			retVal = bsScanner.nextLine();

			bsSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return retVal;
	}
	
	@Override
	public void run() {
		String response = getSomeServentIpAndPort();
		String ipAddress = response.split(",")[0];
		int someServentPort = Integer.parseInt(response.split(",")[1]);
		int myId = Integer.parseInt(response.split(",")[2]);
//		int someServentPort = getSomeServentIpAndPort();
		
		if (someServentPort == -2) {
			AppConfig.timestampedErrorPrint("Error in contacting bootstrap. Exiting...");
			System.exit(0);
		}
		if (someServentPort == -1) { //bootstrap gave us -1 -> we are first
			AppConfig.timestampedStandardPrint("First node in Chord system.");
			AppConfig.chordState.setNodeCount(1);
			AppConfig.myServentInfo.setUuid(0);
			AppConfig.chordState.getAllNodeInfoHelper().add(AppConfig.myServentInfo);
			JobCommandHandler.fractalIds.put(0, "");
			AppConfig.lamportClock = new LamportClock();
		} else { //bootstrap gave us something else - let that node tell our successor that we are here
			AppConfig.myServentInfo.setUuid(myId);
			NewNodeMessage nnm = new NewNodeMessage(AppConfig.myServentInfo.getListenerPort(), someServentPort);
			MessageUtil.sendMessage(nnm);
		}
	}

}
