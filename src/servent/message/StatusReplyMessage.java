package servent.message;

import app.Job;

import java.util.List;

public class StatusReplyMessage extends BasicMessage {

    private static final long serialVersionUID = -3751384039370867665L;

    private final List<Integer> results;
    private final List<String> ids;
    private final Job job;

    public StatusReplyMessage(int senderPort, int receiverPort, String messageText, List<Integer> results, List<String> ids,
                              Job job) {
        super(MessageType.STATUS_REPLY, senderPort, receiverPort, messageText);

        this.results = results;
        this.ids = ids;
        this.job = job;
    }

    public List<Integer> getResults() {
        return results;
    }

    public List<String> getIds() {
        return ids;
    }

    public Job getJob() {
        return job;
    }
}
