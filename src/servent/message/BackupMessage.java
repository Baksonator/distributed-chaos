package servent.message;

import app.Point;

import java.util.List;

public class BackupMessage extends BasicMessage {

    private static final long serialVersionUID = -7588727584855549523L;

    private final List<Point> backUp;
    private final int nodeId;

    public BackupMessage(int senderPort, int receiverPort, List<Point> backUp, int nodeId) {
        super(MessageType.BACKUP, senderPort, receiverPort);

        this.backUp = backUp;
        this.nodeId = nodeId;
    }

    public List<Point> getBackUp() {
        return backUp;
    }

    public int getNodeId() {
        return nodeId;
    }
}
