package servent.message;

public class LeaveMessage extends BasicMessage {

    private static final long serialVersionUID = 7646462840385834237L;

    private final boolean first;

    public LeaveMessage(int senderPort, int receiverPort, String messageText, boolean first) {
        super(MessageType.LEAVE, senderPort, receiverPort, messageText);

        this.first = first;
    }

    public boolean isFirst() {
        return first;
    }
}
