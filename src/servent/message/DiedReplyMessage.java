package servent.message;

public class DiedReplyMessage extends BasicMessage {

    private static final long serialVersionUID = 8430784490022344639L;

    private final int senderId;

    public DiedReplyMessage(int senderPort, int receiverPort, String messageText, int senderId) {
        super(MessageType.DIED_REPLY, senderPort, receiverPort, messageText);

        this.senderId = senderId;
    }

    public int getSenderId() {
        return senderId;
    }
}
