package servent.message;

import app.Point;

import java.util.List;

public class ResultCollectionMessage extends BasicMessage {

    private static final long serialVersionUID = -743610137602656631L;

    private final List<Point> results;

    public ResultCollectionMessage(int senderPort, int receiverPort, String messageText, List<Point> results) {
        super(MessageType.RESULT_COLLECTION, senderPort, receiverPort, messageText);

        this.results = results;
    }

    public List<Point> getResults() {
        return results;
    }
}
