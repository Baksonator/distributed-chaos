package servent.message;

import mutex.LogicalTimestamp;

public class MutexReleaseMessage extends TimeStampedMessage {

    private static final long serialVersionUID = 8247023419256999834L;

    private final boolean flag;

    public MutexReleaseMessage(int senderPort, int receiverPort, String messageText, LogicalTimestamp logicalTimestamp,
                               boolean flag) {
        super(MessageType.MUTEX_RELEASE, senderPort, receiverPort, messageText, logicalTimestamp);

        this.flag = flag;
    }

    public boolean isFlag() {
        return flag;
    }
}
