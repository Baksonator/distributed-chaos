package servent.message;

import java.util.List;

public class StatusCollectionMessage extends BasicMessage {

    private static final long serialVersionUID = 5193198401482845768L;

    private final List<Integer> results;
    private final List<String> ids;

    public StatusCollectionMessage(int senderPort, int receiverPort, String messageText, List<Integer> results,
                                   List<String> ids) {
        super(MessageType.STATUS_COLLECTION, senderPort, receiverPort, messageText);

        this.results = results;
        this.ids = ids;
    }

    public List<Integer> getResults() {
        return results;
    }

    public List<String> getIds() {
        return ids;
    }
}
