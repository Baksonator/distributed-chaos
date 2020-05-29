package servent.message;

import app.Job;

import java.util.Map;

public class JobMessage extends BasicMessage {

    private static final long serialVersionUID = -9129826617288151825L;

    private final Job job;
    private final Map<Integer, String> fractalIds;
    private final Integer level;

    public JobMessage(int senderPort, int receiverPort, String messageText, Job job, Map<Integer, String> fractalIds, Integer level) {
        super(MessageType.JOB, senderPort, receiverPort, messageText);

        this.job = job;
        this.fractalIds = fractalIds;
        this.level = level;
    }

    public Job getJob() {
        return job;
    }

    public Map<Integer, String> getFractalIds() {
        return fractalIds;
    }

    public Integer getLevel() {
        return level;
    }
}
