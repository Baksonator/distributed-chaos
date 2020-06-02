package servent.message;

import java.util.Map;

public class WelcomeMessage extends BasicMessage {

	private static final long serialVersionUID = -8981406250652693908L;

	private final Map<Integer, Integer> values;
	private final int logicalTime;
	private final int totalNodes;
	
	public WelcomeMessage(int senderPort, int receiverPort, Map<Integer, Integer> values, int logicalTime, int totalNodes) {
		super(MessageType.WELCOME, senderPort, receiverPort);
		
		this.values = values;
		this.logicalTime = logicalTime;
		this.totalNodes = totalNodes;
	}
	
	public Map<Integer, Integer> getValues() {
		return values;
	}

	public int getLogicalTime() {
		return logicalTime;
	}

	public int getTotalNodes() {
		return totalNodes;
	}
}
