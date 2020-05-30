package servent.message;

import app.Point;

import java.util.List;

public class JobMigrationMessage extends BasicMessage {

    private static final long serialVersionUID = -4348819449598839503L;

    private final List<Point> data;

    public JobMigrationMessage(int senderPort, int receiverPort, String messageText, List<Point> data) {
        super(MessageType.JOB_MIGRATION, senderPort, receiverPort, messageText);

        this.data = data;
    }

    public List<Point> getData() {
        return data;
    }
}
