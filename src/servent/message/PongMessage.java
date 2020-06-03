package servent.message;

public class PongMessage extends BasicMessage {

    private static final long serialVersionUID = 6526158468526582334L;

    public PongMessage(int senderPort, int receiverPort, String messageText) {
        super(MessageType.PONG, senderPort, receiverPort, messageText);
    }
}
