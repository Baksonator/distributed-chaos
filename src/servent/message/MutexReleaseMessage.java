package servent.message;

import mutex.LogicalTimestamp;

public class MutexReleaseMessage extends TimeStampedMessage {

    private static final long serialVersionUID = 8247023419256999834L;

    public MutexReleaseMessage(int senderPort, int receiverPort, LogicalTimestamp logicalTimestamp) {
        super(MessageType.MUTEX_RELEASE, senderPort, receiverPort, logicalTimestamp);
    }
}
