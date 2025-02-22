package servent.handler;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;

import app.AppConfig;
import app.Job;
import app.JobCommandHandler;
import app.ServentInfo;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.ReleaseEntryMessage;
import servent.message.UpdateMessage;
import servent.message.util.FifoSendWorker;
import servent.message.util.MessageUtil;

public class UpdateHandler implements MessageHandler {

	private Message clientMessage;
	
	public UpdateHandler(Message clientMessage) {
		this.clientMessage = clientMessage;
	}
	
	@Override
	public void run() {
		if (clientMessage.getMessageType() == MessageType.UPDATE) {
			if (!clientMessage.getSenderIp().equals(AppConfig.myServentInfo.getIpAddress()) || clientMessage.getSenderPort() != AppConfig.myServentInfo.getListenerPort()) {
				UpdateMessage updateMessage = (UpdateMessage) clientMessage;
				ServentInfo newNodInfo = new ServentInfo(clientMessage.getSenderIp(), clientMessage.getSenderPort());
				newNodInfo.setUuid(updateMessage.getNewId());
				List<ServentInfo> newNodes = new ArrayList<>();
				newNodes.add(newNodInfo);

				AppConfig.failureDetector.setFlag(false);
				AppConfig.failureDetector.setSavedTime(-1);

				AppConfig.chordState.incrementNodeCount();
				AppConfig.chordState.updateLogLevel();

				AppConfig.chordState.addNodes(newNodes);
				AppConfig.chordState.getAllNodeInfoHelper().add(newNodInfo);
				AppConfig.chordState.getLastHeardMap().put(updateMessage.getNewId(), new Timestamp(System.currentTimeMillis()));
				AppConfig.chordState.getSuspiciousMap().put(updateMessage.getNewId(), false);

				FifoSendWorker fifoSendWorker = new FifoSendWorker(newNodInfo.getUuid());
				AppConfig.fifoSendWorkers.add(fifoSendWorker);
				MessageUtil.pendingMessages.put(newNodInfo.getUuid(), new LinkedBlockingQueue<>());
				Thread senderThread = new Thread(fifoSendWorker);
				senderThread.start();

				String newMessageText = "";
				if (clientMessage.getMessageText().equals("")) {
					newMessageText = String.valueOf(AppConfig.myServentInfo.getListenerPort());
				} else {
					newMessageText = clientMessage.getMessageText() + "," + AppConfig.myServentInfo.getListenerPort();
				}
				JobCommandHandler.fractalIds.put(AppConfig.chordState.getNodeCount() - 1, "");
				List<String> currIps = new ArrayList<>(updateMessage.getIps());
				currIps.add(AppConfig.myServentInfo.getIpAddress());
				List<Integer> currIds = new ArrayList<>(updateMessage.getIds());
				currIds.add(AppConfig.myServentInfo.getUuid());
				Message nextUpdate = new UpdateMessage(clientMessage.getSenderPort(), AppConfig.chordState.getNextNodePort(),
						newMessageText, JobCommandHandler.fractalIds, AppConfig.activeJobs, updateMessage.getNewId(),
						currIps, currIds);
				nextUpdate.setSenderIp(clientMessage.getSenderIp());
				nextUpdate.setReceiverIp(AppConfig.chordState.getNextNodeIp());
				MessageUtil.sendMessage(nextUpdate);
			} else {
				String messageText = clientMessage.getMessageText();
				String[] ports = messageText.split(",");
				UpdateMessage updateMessage = (UpdateMessage) clientMessage;
				
				List<ServentInfo> allNodes = new ArrayList<>();
				int i = 0;
				for (String port : ports) {
					ServentInfo newServentInfo = new ServentInfo(updateMessage.getIps().get(i), Integer.parseInt(port));
					newServentInfo.setUuid(updateMessage.getIds().get(i++));
					allNodes.add(newServentInfo);

					FifoSendWorker fifoSendWorker = new FifoSendWorker(newServentInfo.getUuid());
					AppConfig.fifoSendWorkers.add(fifoSendWorker);
					AppConfig.chordState.getSuspiciousMap().put(newServentInfo.getUuid(), false);
					AppConfig.chordState.getLastHeardMap().put(newServentInfo.getUuid(), new Timestamp(System.currentTimeMillis()));
					MessageUtil.pendingMessages.put(newServentInfo.getUuid(), new LinkedBlockingQueue<>());
					Thread senderThread = new Thread(fifoSendWorker);
					senderThread.start();
				}

//				AppConfig.myServentInfo.setUuid(allNodes.size());
				AppConfig.chordState.setNodeCount(allNodes.size() + 1);
				AppConfig.chordState.getAllNodeInfoHelper().addAll(allNodes);
				AppConfig.chordState.getAllNodeInfoHelper().add(AppConfig.myServentInfo);
				AppConfig.chordState.updateLogLevel();
				AppConfig.chordState.addNodes(allNodes);
				UpdateMessage arrivedMessage = (UpdateMessage) clientMessage;
				JobCommandHandler.fractalIds = arrivedMessage.getFractalIds();
				AppConfig.activeJobs = arrivedMessage.getActiveJobs();
				if (AppConfig.activeJobs.size() > 0) {
					AppConfig.jobLatch = new CountDownLatch(AppConfig.chordState.getNodeCount());
					JobCommandHandler.restructureEntry();
					try {
						AppConfig.jobLatch.await();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}

				AppConfig.chordState.init2();

				ReleaseEntryMessage releaseEntryMessage = new ReleaseEntryMessage(AppConfig.myServentInfo.getListenerPort(),
						AppConfig.chordState.getNextNodePort());
				releaseEntryMessage.setSenderIp(AppConfig.myServentInfo.getIpAddress());
				releaseEntryMessage.setReceiverIp(AppConfig.chordState.getNextNodeIp());
				MessageUtil.sendMessage(releaseEntryMessage);
			}
		} else {
			AppConfig.timestampedErrorPrint("Update message handler got message that is not UPDATE");
		}
	}

}
