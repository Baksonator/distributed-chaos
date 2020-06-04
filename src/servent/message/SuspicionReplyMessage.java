package servent.message;

public class SuspicionReplyMessage extends BasicMessage {

    private static final long serialVersionUID = -4145869080556563551L;

    private final boolean isSuspicious;
    private final int nodeId;

    public SuspicionReplyMessage(int senderPort, int receiverPort, String messageText, boolean isSuspicious, int nodeId) {
        super(MessageType.SUSPICION_REPLY, senderPort, receiverPort, messageText);

        this.isSuspicious = isSuspicious;
        this.nodeId = nodeId;
    }

    public boolean isSuspicious() {
        return isSuspicious;
    }

    public int getNodeId() {
        return nodeId;
    }
}
