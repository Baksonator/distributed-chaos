package servent.message;

public class DiedMessage extends BasicMessage {

    private static final long serialVersionUID = 6726761745850466908L;

    public DiedMessage(int senderPort, int receiverPort, String messageText) {
        super(MessageType.DIED, senderPort, receiverPort, messageText);
    }
}
