package servent.message;

public class LeaveMessage extends BasicMessage {

    public LeaveMessage(int senderPort, int receiverPort, String messageText) {
        super(MessageType.LEAVE, senderPort, receiverPort, messageText);
    }

}
