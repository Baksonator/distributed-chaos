package servent.message;

public class JobMessageResponse extends BasicMessage {

    private static final long serialVersionUID = 2137878679404891076L;

    public JobMessageResponse(int senderPort, int receiverPort, String messageText) {
        super(MessageType.JOB_MESSAGE_RESPONSE, senderPort, receiverPort, messageText);
    }
}
