package servent.handler;

import java.util.concurrent.CountDownLatch;

import app.AppConfig;
import app.ServentInfo;
import mutex.LogicalTimestamp;
import servent.message.*;
import servent.message.util.MessageUtil;

public class NewNodeHandler implements MessageHandler {

	private Message clientMessage;
	
	public NewNodeHandler(Message clientMessage) {
		this.clientMessage = clientMessage;
	}
	
	@Override
	public void run() {
		if (clientMessage.getMessageType() == MessageType.NEW_NODE) {
			int newNodePort = clientMessage.getSenderPort();
			ServentInfo newNodeInfo = new ServentInfo("localhost", newNodePort);

			// TODO PAZI NA OVU PROMENU
			if (AppConfig.myServentInfo.getUuid() == AppConfig.chordState.getAllNodeInfoHelper().get(0).getUuid()) {
				// I am the first node in the ring
				AppConfig.chordState.setPredecessor(newNodeInfo);

//				synchronized (AppConfig.localLock) {
				try {
					AppConfig.localSemaphore.acquire();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				AppConfig.lamportClock.tick();
				LogicalTimestamp myRequestLogicalTimestamp = new LogicalTimestamp(AppConfig.lamportClock.getValue(),
						AppConfig.myServentInfo.getUuid());

				AppConfig.replyLatch = new CountDownLatch(AppConfig.chordState.getNodeCount() - 1);

				if (AppConfig.chordState.getNodeCount() > 1) {
					MutexRequestMessage mutexRequestMessage = new MutexRequestMessage(AppConfig.myServentInfo.getListenerPort(),
							AppConfig.chordState.getNextNodePort(), Integer.toString(AppConfig.myServentInfo.getUuid()),
							myRequestLogicalTimestamp);
					MessageUtil.sendMessage(mutexRequestMessage);
				}

				AppConfig.requestQueue.add(myRequestLogicalTimestamp);

				try {
					AppConfig.replyLatch.await();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				while (!AppConfig.requestQueue.peek().equals(myRequestLogicalTimestamp)) {
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}

				WelcomeMessage wm = new WelcomeMessage(AppConfig.myServentInfo.getListenerPort(), newNodePort, null,
						AppConfig.lamportClock.getValue(), AppConfig.chordState.getNodeCount());
				MessageUtil.sendMessage(wm);
//				}
			} else {
				ServentInfo nextNode = AppConfig.chordState.getNextNodeForFirst();
				NewNodeMessage nnm = new NewNodeMessage(newNodePort, nextNode.getListenerPort());
				MessageUtil.sendMessage(nnm);
			}
//		}
//		if (clientMessage.getMessageType() == MessageType.NEW_NODE) {
//			int newNodePort = clientMessage.getSenderPort();
//			ServentInfo newNodeInfo = new ServentInfo("localhost", newNodePort);
//
//			//check if the new node collides with another existing node.
//			if (AppConfig.chordState.isCollision(newNodeInfo.getChordId())) {
//				Message sry = new SorryMessage(AppConfig.myServentInfo.getListenerPort(), clientMessage.getSenderPort());
//				MessageUtil.sendMessage(sry);
//				return;
//			}
//
//			//check if he is my predecessor
//			boolean isMyPred = AppConfig.chordState.isKeyMine(newNodeInfo.getChordId());
//			if (isMyPred) { //if yes, prepare and send welcome message
//				ServentInfo hisPred = AppConfig.chordState.getPredecessor();
//				if (hisPred == null) {
//					hisPred = AppConfig.myServentInfo;
//				}
//
//				AppConfig.chordState.setPredecessor(newNodeInfo);
//
//				Map<Integer, Integer> myValues = AppConfig.chordState.getValueMap();
//				Map<Integer, Integer> hisValues = new HashMap<>();
//
//				int myId = AppConfig.myServentInfo.getChordId();
//				int hisPredId = hisPred.getChordId();
//				int newNodeId = newNodeInfo.getChordId();
//
//				for (Entry<Integer, Integer> valueEntry : myValues.entrySet()) {
//					if (hisPredId == myId) { //i am first and he is second
//						if (myId < newNodeId) {
//							if (valueEntry.getKey() <= newNodeId && valueEntry.getKey() > myId) {
//								hisValues.put(valueEntry.getKey(), valueEntry.getValue());
//							}
//						} else {
//							if (valueEntry.getKey() <= newNodeId || valueEntry.getKey() > myId) {
//								hisValues.put(valueEntry.getKey(), valueEntry.getValue());
//							}
//						}
//					}
//					if (hisPredId < myId) { //my old predecesor was before me
//						if (valueEntry.getKey() <= newNodeId) {
//							hisValues.put(valueEntry.getKey(), valueEntry.getValue());
//						}
//					} else { //my old predecesor was after me
//						if (hisPredId > newNodeId) { //new node overflow
//							if (valueEntry.getKey() <= newNodeId || valueEntry.getKey() > hisPredId) {
//								hisValues.put(valueEntry.getKey(), valueEntry.getValue());
//							}
//						} else { //no new node overflow
//							if (valueEntry.getKey() <= newNodeId && valueEntry.getKey() > hisPredId) {
//								hisValues.put(valueEntry.getKey(), valueEntry.getValue());
//							}
//						}
//
//					}
//
//				}
//				for (Integer key : hisValues.keySet()) { //remove his values from my map
//					myValues.remove(key);
//				}
//				AppConfig.chordState.setValueMap(myValues);
//
//				WelcomeMessage wm = new WelcomeMessage(AppConfig.myServentInfo.getListenerPort(), newNodePort, hisValues);
//				MessageUtil.sendMessage(wm);
//			} else { //if he is not my predecessor, let someone else take care of it
//				ServentInfo nextNode = AppConfig.chordState.getNextNodeForKey(newNodeInfo.getChordId());
//				NewNodeMessage nnm = new NewNodeMessage(newNodePort, nextNode.getListenerPort());
//				MessageUtil.sendMessage(nnm);
//			}
//
		} else {
			AppConfig.timestampedErrorPrint("NEW_NODE handler got something that is not new node message.");
		}

	}

}
