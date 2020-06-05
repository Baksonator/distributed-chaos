package servent.message;

import app.Job;
import app.Point;

import java.util.List;

public class ResultReplyMessage extends BasicMessage {

    private static final long serialVersionUID = 3077921894792407042L;

    private final List<Point> results;
    private final Job job;
    private final boolean flag;
    private final String fractalId;

    public ResultReplyMessage(int senderPort, int receiverPort, String messageText, List<Point> results, Job job,
                              boolean flag, String fractalId) {
        super(MessageType.RESULT_REPLY, senderPort, receiverPort, messageText);

        this.results = results;
        this.job = job;
        this.flag = flag;
        this.fractalId = fractalId;
    }

    public List<Point> getResults() {
        return results;
    }

    public Job getJob() {
        return job;
    }

    public boolean isFlag() {
        return flag;
    }

    public String getFractalId() {
        return fractalId;
    }
}
