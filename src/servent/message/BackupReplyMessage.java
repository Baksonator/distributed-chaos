package servent.message;

import app.Point;

import java.util.List;

public class BackupReplyMessage extends BasicMessage {

    private static final long serialVersionUID = 283979902122840377L;

    private final List<Point> backup;
    private final int nodeId;

    public BackupReplyMessage(int senderPort, int receiverPort, List<Point> backup, int nodeId) {
        super(MessageType.BACKUP_REPLY, senderPort, receiverPort);

        this.backup = backup;
        this.nodeId = nodeId;
    }

    public List<Point> getBackup() {
        return backup;
    }

    public int getNodeId() {
        return nodeId;
    }
}
