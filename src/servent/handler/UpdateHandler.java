package servent.handler;

import java.util.ArrayList;
import java.util.List;

import app.AppConfig;
import app.Job;
import app.JobCommandHandler;
import app.ServentInfo;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.UpdateMessage;
import servent.message.util.MessageUtil;

public class UpdateHandler implements MessageHandler {

	private Message clientMessage;
	
	public UpdateHandler(Message clientMessage) {
		this.clientMessage = clientMessage;
	}
	
	@Override
	public void run() {
		if (clientMessage.getMessageType() == MessageType.UPDATE) {
			// TODO Mora i IP Adresa ili nesto
			if (clientMessage.getSenderPort() != AppConfig.myServentInfo.getListenerPort()) {
				ServentInfo newNodInfo = new ServentInfo("localhost", clientMessage.getSenderPort());
				newNodInfo.setUuid(AppConfig.chordState.getNodeCount());
				List<ServentInfo> newNodes = new ArrayList<>();
				newNodes.add(newNodInfo);

				AppConfig.chordState.incrementNodeCount();
				AppConfig.chordState.updateLogLevel();

				AppConfig.chordState.addNodes(newNodes);
				AppConfig.chordState.getAllNodeInfoHelper().add(newNodInfo);
				String newMessageText = "";
				if (clientMessage.getMessageText().equals("")) {
					newMessageText = String.valueOf(AppConfig.myServentInfo.getListenerPort());
				} else {
					newMessageText = clientMessage.getMessageText() + "," + AppConfig.myServentInfo.getListenerPort();
				}
				JobCommandHandler.fractalIds.put(AppConfig.chordState.getNodeCount() - 1, "");
				Message nextUpdate = new UpdateMessage(clientMessage.getSenderPort(), AppConfig.chordState.getNextNodePort(),
						newMessageText, JobCommandHandler.fractalIds, AppConfig.activeJobs);
				MessageUtil.sendMessage(nextUpdate);
			} else {
				String messageText = clientMessage.getMessageText();
				String[] ports = messageText.split(",");
				
				List<ServentInfo> allNodes = new ArrayList<>();
				int i = 0;
				for (String port : ports) {
					ServentInfo newServentInfo = new ServentInfo("localhost", Integer.parseInt(port));
					newServentInfo.setUuid(i++);
					allNodes.add(newServentInfo);
				}

				AppConfig.myServentInfo.setUuid(allNodes.size());
				AppConfig.chordState.setNodeCount(allNodes.size() + 1);
				AppConfig.chordState.getAllNodeInfoHelper().addAll(allNodes);
				AppConfig.chordState.getAllNodeInfoHelper().add(AppConfig.myServentInfo);
				AppConfig.chordState.updateLogLevel();
				AppConfig.chordState.addNodes(allNodes);
				UpdateMessage arrivedMessage = (UpdateMessage) clientMessage;
				JobCommandHandler.fractalIds = arrivedMessage.getFractalIds();
				AppConfig.activeJobs = arrivedMessage.getActiveJobs();
				if (AppConfig.activeJobs.size() > 0) {
					JobCommandHandler.restructureEntry();
				}
			}
		} else {
			AppConfig.timestampedErrorPrint("Update message handler got message that is not UPDATE");
		}
	}

}
