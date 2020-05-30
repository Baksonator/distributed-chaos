package servent.message;

public class ResultRequestMessage extends BasicMessage {

    private static final long serialVersionUID = 555642990686182462L;

    public ResultRequestMessage(int senderPort, int receiverPort, String messageText) {
        super(MessageType.RESULT_REQUEST, senderPort, receiverPort, messageText);
    }
}
