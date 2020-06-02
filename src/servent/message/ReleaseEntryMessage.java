package servent.message;

public class ReleaseEntryMessage extends BasicMessage {

    private static final long serialVersionUID = 4737848539739138038L;

    public ReleaseEntryMessage(int senderPort, int receiverPort) {
        super(MessageType.RELEASE_ENTRY, senderPort, receiverPort);
    }
}
