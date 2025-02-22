package app;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import servent.message.AskGetMessage;
import servent.message.PutMessage;
import servent.message.UpdateMessage;
import servent.message.WelcomeMessage;
import servent.message.util.MessageUtil;

/**
 * This class implements all the logic required for Chord to function.
 * It has a static method <code>chordHash</code> which will calculate our chord ids.
 * It also has a static attribute <code>CHORD_SIZE</code> that tells us what the maximum
 * key is in our system.
 * 
 * Other public attributes and methods:
 * <ul>
 *   <li><code>chordLevel</code> - log_2(CHORD_SIZE) - size of <code>successorTable</code></li>
 *   <li><code>successorTable</code> - a map of shortcuts in the system.</li>
 *   <li><code>predecessorInfo</code> - who is our predecessor.</li>
 *   <li><code>valueMap</code> - DHT values stored on this node.</li>
 *   <li><code>init()</code> - should be invoked when we get the WELCOME message.</li>
 *   <li><code>isCollision(int chordId)</code> - checks if a servent with that Chord ID is already active.</li>
 *   <li><code>isKeyMine(int key)</code> - checks if we have a key locally.</li>
 *   <li><code>getNextNodeForKey(int key)</code> - if next node has this key, then return it, otherwise returns the nearest predecessor for this key from my successor table.</li>
 *   <li><code>addNodes(List<ServentInfo> nodes)</code> - updates the successor table.</li>
 *   <li><code>putValue(int key, int value)</code> - stores the value locally or sends it on further in the system.</li>
 *   <li><code>getValue(int key)</code> - gets the value locally, or sends a message to get it from somewhere else.</li>
 * </ul>
 * @author bmilojkovic
 *
 */
public class ChordState {

	public static int CHORD_SIZE;
	public static int chordHash(int value) {
		return 61 * value % CHORD_SIZE;
	}

	private int nodeCount;

	private int chordLevel; //log_2(CHORD_SIZE)

	private int logLevel;
	
	private ServentInfo[] successorTable;
	private List<ServentInfo> successorTableAlt;
	private ServentInfo predecessorInfo;
	
	//we DO NOT use this to send messages, but only to construct the successor table
	private List<ServentInfo> allNodeInfo;

	private List<ServentInfo> allNodeInfoHelper;

	private Map<Integer, Boolean> suspiciousMap;
	private Map<Integer, Timestamp> lastHeardMap;
	private Map<Integer, Boolean> reallySuspucious;
	
	private Map<Integer, Integer> valueMap;
	
	public ChordState() {
		this.chordLevel = 1;
		int tmp = CHORD_SIZE;
		while (tmp != 2) {
			if (tmp % 2 != 0) { //not a power of 2
				throw new NumberFormatException();
			}
			tmp /= 2;
			this.chordLevel++;
		}
		
		successorTable = new ServentInfo[chordLevel];
		for (int i = 0; i < chordLevel; i++) {
			successorTable[i] = null;
		}
		
		predecessorInfo = null;
		successorTableAlt = new ArrayList<>();
		valueMap = new ConcurrentHashMap<>();
		allNodeInfo = new CopyOnWriteArrayList<>();
		allNodeInfoHelper = new CopyOnWriteArrayList<>();
		suspiciousMap = new ConcurrentHashMap<>();
		lastHeardMap = new ConcurrentHashMap<>();
		reallySuspucious = new ConcurrentHashMap<>();
	}
	
	/**
	 * This should be called once after we get <code>WELCOME</code> message.
	 * It sets up our initial value map and our first successor so we can send <code>UPDATE</code>.
	 * It also lets bootstrap know that we did not collide.
	 */
	public void init(WelcomeMessage welcomeMsg) {
		//set a temporary pointer to next node, for sending of update message
//		successorTable[0] = new ServentInfo("localhost", welcomeMsg.getSenderPort());
		successorTableAlt.add(new ServentInfo(welcomeMsg.getSenderIp(), welcomeMsg.getSenderPort()));
		this.valueMap = welcomeMsg.getValues();
		
		//tell bootstrap this node is not a collider
//		try {
//			Socket bsSocket = new Socket("localhost", AppConfig.BOOTSTRAP_PORT);
//
//			PrintWriter bsWriter = new PrintWriter(bsSocket.getOutputStream());
//			bsWriter.write("New\n" + AppConfig.myServentInfo.getListenerPort() + "\n");
//
//			bsWriter.flush();
//			bsSocket.close();
//		} catch (UnknownHostException e) {
//			e.printStackTrace();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
	}

	public void init2() {
		try {
			Socket bsSocket = new Socket(AppConfig.BOOTSTRAP_IP, AppConfig.BOOTSTRAP_PORT);

			PrintWriter bsWriter = new PrintWriter(bsSocket.getOutputStream());
			bsWriter.write("New\n" + AppConfig.myServentInfo.getIpAddress() + "," + AppConfig.myServentInfo.getListenerPort() + "\n");

			bsWriter.flush();
			bsSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public int getChordLevel() {
		return chordLevel;
	}
	
	public ServentInfo[] getSuccessorTable() {
		return successorTable;
	}
	
	public int getNextNodePort() {
		if (nodeCount == 1) {
			return AppConfig.myServentInfo.getListenerPort();
		}
		return successorTableAlt.get(0).getListenerPort();
//		return successorTable[0].getListenerPort();
	}

	public String getNextNodeIp() {
		if (nodeCount == 1) {
			return AppConfig.myServentInfo.getIpAddress();
		}
		return successorTableAlt.get(0).getIpAddress();
	}
	
	public ServentInfo getPredecessor() {
		return predecessorInfo;
	}
	
	public void setPredecessor(ServentInfo newNodeInfo) {
		this.predecessorInfo = newNodeInfo;
	}

	public Map<Integer, Integer> getValueMap() {
		return valueMap;
	}
	
	public void setValueMap(Map<Integer, Integer> valueMap) {
		this.valueMap = valueMap;
	}

	public int getNodeCount() {
		return nodeCount;
	}

	public void setNodeCount(int nodeCount) {
		this.nodeCount = nodeCount;
	}

	public List<ServentInfo> getSuccessorTableAlt() {
		return successorTableAlt;
	}

	public int getLogLevel() {
		return logLevel;
	}

	public List<ServentInfo> getAllNodeInfo() {
		return allNodeInfo;
	}

	public List<ServentInfo> getAllNodeInfoHelper() {
		return allNodeInfoHelper;
	}

	public Map<Integer, Boolean> getSuspiciousMap() {
		return suspiciousMap;
	}

	public Map<Integer, Timestamp> getLastHeardMap() {
		return lastHeardMap;
	}

	public Map<Integer, Boolean> getReallySuspucious() {
		return reallySuspucious;
	}

	public void updateLogLevel() {
		if (nodeCount == 1) {
			this.logLevel = 1;
			return;
		}
		this.logLevel = 1;
//		int tmp = this.nodeCount;
		int tmp = 1;
		while (tmp * 2 <= this.nodeCount) {
			tmp *= 2;
		}
//		while (tmp % 2 != 0) {
//			tmp--;
//		}
		while (tmp != 2) {
			if (tmp % 2 != 0) { //not a power of 2
				throw new NumberFormatException();
			}
			tmp /= 2;
			this.logLevel++;
		}
	}

	public void incrementNodeCount() {
		 this.nodeCount++;
	}

	public void decrementNodeCount() {
		this.nodeCount--;
	}

	public boolean isCollision(int chordId) {
		if (chordId == AppConfig.myServentInfo.getChordId()) {
			return true;
		}
		for (ServentInfo serventInfo : allNodeInfo) {
			if (serventInfo.getChordId() == chordId) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Returns true if we are the owner of the specified key.
	 */
	public boolean isKeyMine(int key) {
		if (key == AppConfig.myServentInfo.getUuid()) {
			return true;
		} else {
			return false;
		}

//		if (predecessorInfo == null) {
//			return true;
//		}
//
//		int predecessorUuid = predecessorInfo.getUuid();
//		int myChordUuid = AppConfig.myServentInfo.getUuid();
//
//		if (predecessorUuid < myChordUuid) { //no overflow
//			if (key <= myChordUuid && key > predecessorUuid) {
//				return true;
//			}
//		} else { //overflow
//			if (key <= myChordUuid || key > predecessorUuid) {
//				return true;
//			}
//		}
//
//		return false;
	}

	public ServentInfo getNextNodeForFirst() {
		for (int i = 1; i < successorTableAlt.size(); i++) {
			int successorId = successorTableAlt.get(i).getUuid();

			if (successorId > 0) {
				return successorTableAlt.get(i - 1);
			}
		}

		return successorTableAlt.get(successorTableAlt.size() - 1);
	}

	/**
	 * Main chord operation - find the nearest node to hop to to find a specific key.
	 * We have to take a value that is smaller than required to make sure we don't overshoot.
	 * We can only be certain we have found the required node when it is our first next node.
	 */
	public ServentInfo getNextNodeForKey(int key) {
		if (isKeyMine(key)) {
			return AppConfig.myServentInfo;
		}

//		int previousId = successorTable[0].getChordId();
		int previousId = successorTableAlt.get(0).getUuid();
		if (previousId == key) {
			return successorTableAlt.get(0);
		}
		for (int i = 1; i < successorTableAlt.size(); i++) {
			if (successorTableAlt.get(i) == null) {
				AppConfig.timestampedErrorPrint("Couldn't find successor for " + key);
				break;
			}
			
			int successorId = successorTableAlt.get(i).getUuid();

			if (successorId == key) {
				return successorTableAlt.get(i);
			}
			if (AppConfig.myServentInfo.getUuid() < key) {
				if (successorId >= key) {
					return successorTableAlt.get(i - 1);
				}
				if (key > previousId && successorId < previousId) { //overflow
					return successorTableAlt.get(i - 1);
				}
			} else {
				if (successorId < AppConfig.myServentInfo.getUuid() && successorId >= key) {
					return successorTableAlt.get(i - 1);
				}
				if (key > previousId && successorId < previousId) { //overflow
					return successorTableAlt.get(i - 1);
				}
			}
//			if (successorId >= key) {
//				if (AppConfig.myServentInfo.getUuid() > key) {
//
//				} else {
//					return successorTableAlt.get(i - 1);
//				}
//			}
//			if (key > previousId && successorId < previousId) { //overflow
//				return successorTableAlt.get(i - 1);
//			}
			previousId = successorId;
		}
		//if we have only one node in all slots in the table, we might get here
		//then we can return any item
		return successorTableAlt.get(successorTableAlt.size() - 1);
	}

	private void updateSuccessorTable() {
		//first node after me has to be successorTable[0]
		if (nodeCount == 1) {
			successorTableAlt.clear();
			return;
		}
		
		int currentNodeIndex = 0;
		ServentInfo currentNode = allNodeInfo.get(currentNodeIndex);
		successorTableAlt.clear();
		successorTableAlt.add(currentNode);
		successorTable[0] = currentNode;
		
		int currentIncrement = 2;
		
		ServentInfo previousNode = AppConfig.myServentInfo;

		for (int i = 1; i < logLevel; i++, currentIncrement *= 2) {
			successorTableAlt.add(allNodeInfo.get(currentIncrement - 1));
		}

//		//i is successorTable index
//		for(int i = 1; i < chordLevel; i++, currentIncrement *= 2) {
//			//we are looking for the node that has larger chordId than this
//			int currentValue = (AppConfig.myServentInfo.getChordId() + currentIncrement) % CHORD_SIZE;
//
//			int currentId = currentNode.getChordId();
//			int previousId = previousNode.getChordId();
//
//			//this loop needs to skip all nodes that have smaller chordId than currentValue
//			while (true) {
//				if (currentValue > currentId) {
//					//before skipping, check for overflow
//					if (currentId > previousId || currentValue < previousId) {
//						//try same value with the next node
//						previousId = currentId;
//						currentNodeIndex = (currentNodeIndex + 1) % allNodeInfo.size();
//						currentNode = allNodeInfo.get(currentNodeIndex);
//						currentId = currentNode.getChordId();
//					} else {
//						successorTable[i] = currentNode;
//						break;
//					}
//				} else { //node id is larger
//					ServentInfo nextNode = allNodeInfo.get((currentNodeIndex + 1) % allNodeInfo.size());
//					int nextNodeId = nextNode.getChordId();
//					//check for overflow
//					if (nextNodeId < currentId && currentValue <= nextNodeId) {
//						//try same value with the next node
//						previousId = currentId;
//						currentNodeIndex = (currentNodeIndex + 1) % allNodeInfo.size();
//						currentNode = allNodeInfo.get(currentNodeIndex);
//						currentId = currentNode.getChordId();
//					} else {
//						successorTable[i] = currentNode;
//						break;
//					}
//				}
//			}
//		}
		
	}

	/**
	 * This method constructs an ordered list of all nodes. They are ordered by chordId, starting from this node.
	 * Once the list is created, we invoke <code>updateSuccessorTable()</code> to do the rest of the work.
	 * 
	 */
	public void addNodes(List<ServentInfo> newNodes) {
		allNodeInfo.addAll(newNodes);
		
		allNodeInfo.sort(new Comparator<ServentInfo>() {
			
			@Override
			public int compare(ServentInfo o1, ServentInfo o2) {
				return o1.getUuid() - o2.getUuid();
			}
			
		});

//		allNodeInfoHelper.addAll(newNodes);
		
		List<ServentInfo> newList = new ArrayList<>();
		List<ServentInfo> newList2 = new ArrayList<>();
		
		int myId = AppConfig.myServentInfo.getUuid();
		for (ServentInfo serventInfo : allNodeInfo) {
			if (serventInfo.getUuid() < myId) {
				newList2.add(serventInfo);
			} else {
				newList.add(serventInfo);
			}
		}
		
		allNodeInfo.clear();
		allNodeInfo.addAll(newList);
		allNodeInfo.addAll(newList2);
		if (newList2.size() > 0) {
			predecessorInfo = newList2.get(newList2.size()-1);
		} else {
			predecessorInfo = newList.get(newList.size()-1);
		}
		
		updateSuccessorTable();
	}

	public void removeNode(ServentInfo node) {
		int nodeId = node.getUuid();
		allNodeInfo.remove(node);

		if (allNodeInfo.size() == 0) {
			updateSuccessorTable();
			return;
		}
//		AppConfig.timestampedStandardPrint(allNodeInfo.remove(node)+"");

//		allNodeInfoHelper.remove(node);

//		if (nodeId < AppConfig.myServentInfo.getUuid()) {
//			AppConfig.myServentInfo.setUuid(AppConfig.myServentInfo.getUuid() - 1);
//		}

		allNodeInfo.sort(new Comparator<ServentInfo>() {

			@Override
			public int compare(ServentInfo o1, ServentInfo o2) {
				return o1.getUuid() - o2.getUuid();
			}

		});

		List<ServentInfo> newList = new ArrayList<>();
		List<ServentInfo> newList2 = new ArrayList<>();

		int myId = AppConfig.myServentInfo.getUuid();
		for (ServentInfo serventInfo : allNodeInfo) {
//			if (serventInfo.getUuid() > nodeId) {
//				serventInfo.setUuid(serventInfo.getUuid() - 1);
//			}
			if (serventInfo.getUuid() < myId) {
				newList2.add(serventInfo);
			} else {
				newList.add(serventInfo);
			}
		}

		allNodeInfo.clear();
		allNodeInfo.addAll(newList);
		allNodeInfo.addAll(newList2);
		if (newList2.size() > 0) {
			predecessorInfo = newList2.get(newList2.size()-1);
		} else {
			predecessorInfo = newList.get(newList.size()-1);
		}

		updateSuccessorTable();
	}

	/**
	 * The Chord put operation. Stores locally if key is ours, otherwise sends it on.
	 */
	public void putValue(int key, int value) {
		if (isKeyMine(key)) {
			valueMap.put(key, value);
		} else {
			ServentInfo nextNode = getNextNodeForKey(key);
			PutMessage pm = new PutMessage(AppConfig.myServentInfo.getListenerPort(), nextNode.getListenerPort(), key, value);
			MessageUtil.sendMessage(pm);
		}
	}
	
	/**
	 * The chord get operation. Gets the value locally if key is ours, otherwise asks someone else to give us the value.
	 * @return <ul>
	 *			<li>The value, if we have it</li>
	 *			<li>-1 if we own the key, but there is nothing there</li>
	 *			<li>-2 if we asked someone else</li>
	 *		   </ul>
	 */
	public int getValue(int key) {
		if (isKeyMine(key)) {
			if (valueMap.containsKey(key)) {
				return valueMap.get(key);
			} else {
				return -1;
			}
		}
		
		ServentInfo nextNode = getNextNodeForKey(key);
		AskGetMessage agm = new AskGetMessage(AppConfig.myServentInfo.getListenerPort(), nextNode.getListenerPort(), String.valueOf(key));
		MessageUtil.sendMessage(agm);
		
		return -2;
	}

}
