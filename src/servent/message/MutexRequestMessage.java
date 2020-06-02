package servent.message;

import mutex.LogicalTimestamp;

public class MutexRequestMessage extends TimeStampedMessage {

    private static final long serialVersionUID = 7613582313930024843L;

    public MutexRequestMessage(int senderPort, int receiverPort, String messageText, LogicalTimestamp logicalTimestamp) {
        super(MessageType.MUTEX_REQUEST, senderPort, receiverPort, messageText, logicalTimestamp);
    }
}
