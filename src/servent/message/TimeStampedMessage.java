package servent.message;

import mutex.LogicalTimestamp;

public class TimeStampedMessage extends BasicMessage {

    private static final long serialVersionUID = -451328324888007991L;

    private final LogicalTimestamp logicalTimestamp;

    public TimeStampedMessage(MessageType type, int senderPort, int receiverPort, LogicalTimestamp logicalTimestamp) {
        super(type, senderPort, receiverPort);

        setFifo(true);

        this.logicalTimestamp = logicalTimestamp;
    }

    public TimeStampedMessage(MessageType type, int senderPort, int receiverPort, String messageText,
                              LogicalTimestamp logicalTimestamp) {
        super(type, senderPort, receiverPort, messageText);

        setFifo(true);

        this.logicalTimestamp = logicalTimestamp;
    }
}
