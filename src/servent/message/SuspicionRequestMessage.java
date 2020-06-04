package servent.message;

public class SuspicionRequestMessage extends BasicMessage {

    private static final long serialVersionUID = 2097429033632155523L;

    private final int senderId;
    private final int inquiryNodeId;

    public SuspicionRequestMessage(int senderPort, int receiverPort, String messageText, int senderId, int inquiryNodeId) {
        super(MessageType.SUSPICION_REQUEST, senderPort, receiverPort, messageText);

        this.senderId = senderId;
        this.inquiryNodeId = inquiryNodeId;
    }

    public int getSenderId() {
        return senderId;
    }

    public int getInquiryNodeId() {
        return inquiryNodeId;
    }
}
