package servent.message;

public class StatusRequestMessage extends BasicMessage {

    private static final long serialVersionUID = 7294601860150774364L;

    public StatusRequestMessage(int senderPort, int receiverPort, String messageText) {
        super(MessageType.STATUS_REQUEST, senderPort, receiverPort, messageText);
    }
}
