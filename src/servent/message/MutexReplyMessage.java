package servent.message;

import mutex.LogicalTimestamp;

public class MutexReplyMessage extends TimeStampedMessage {

    private static final long serialVersionUID = -7834537782983873171L;

    public MutexReplyMessage(int senderPort, int receiverPort, String messageText, LogicalTimestamp logicalTimestamp) {
        super(MessageType.MUTEX_REPLY, senderPort, receiverPort, messageText, logicalTimestamp);
    }

}
