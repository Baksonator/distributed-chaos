package servent.handler;

import app.AppConfig;
import mutex.LamportClock;
import mutex.LogicalTimestamp;
import servent.message.*;
import servent.message.util.MessageUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;

public class WelcomeHandler implements MessageHandler {

	private Message clientMessage;
	
	public WelcomeHandler(Message clientMessage) {
		this.clientMessage = clientMessage;
	}
	
	@Override
	public void run() {
		if (clientMessage.getMessageType() == MessageType.WELCOME) {
			WelcomeMessage welcomeMsg = (WelcomeMessage)clientMessage;
			AppConfig.lamportClock = new LamportClock(welcomeMsg.getLogicalTime());

			AppConfig.chordState.init(welcomeMsg);
			
			UpdateMessage um = new UpdateMessage(AppConfig.myServentInfo.getListenerPort(),
					AppConfig.chordState.getNextNodePort(), "", new HashMap<>(), new ArrayList<>());
			MessageUtil.sendMessage(um);
			
		} else {
			AppConfig.timestampedErrorPrint("Welcome handler got a message that is not WELCOME");
		}

	}

}
