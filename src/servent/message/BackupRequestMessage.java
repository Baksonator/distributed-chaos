package servent.message;

public class BackupRequestMessage extends BasicMessage {

    private static final long serialVersionUID = -554525467974026264L;

    private final boolean pred;

    public BackupRequestMessage(int senderPort, int receiverPort, String messageText, boolean pred) {
        super(MessageType.BACKUP_REQUEST, senderPort, receiverPort, messageText);

        this.pred = pred;
    }

    public boolean isPred() {
        return pred;
    }
}
