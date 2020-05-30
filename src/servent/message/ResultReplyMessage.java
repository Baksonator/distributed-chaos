package servent.message;

import app.Job;
import app.Point;

import java.util.List;

public class ResultReplyMessage extends BasicMessage {

    private static final long serialVersionUID = 3077921894792407042L;

    private final List<Point> results;

    public ResultReplyMessage(int senderPort, int receiverPort, String messageText, List<Point> results) {
        super(MessageType.RESULT_REPLY, senderPort, receiverPort, messageText);

        this.results = results;
    }

    public List<Point> getResults() {
        return results;
    }

}
