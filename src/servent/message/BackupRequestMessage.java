package servent.message;

public class BackupRequestMessage extends BasicMessage {

    private static final long serialVersionUID = -554525467974026264L;

    public BackupRequestMessage(int senderPort, int receiverPort, String messageText) {
        super(MessageType.BACKUP_REQUEST, senderPort, receiverPort, messageText);
    }
}
