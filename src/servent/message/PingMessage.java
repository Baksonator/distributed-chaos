package servent.message;

public class PingMessage extends BasicMessage {

    private static final long serialVersionUID = 5048882729501313905L;

    public PingMessage(int senderPort, int receiverPort) {
        super(MessageType.PING, senderPort, receiverPort);
    }
}
