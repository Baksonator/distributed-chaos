package servent.message;

import app.Job;

import java.util.Map;

public class JobMessage extends BasicMessage {

    private static final long serialVersionUID = -9129826617288151825L;

    private final Job job;
    private final Map<Integer, String> fractalIds;
    private final Integer level;
    private final Job mainJob;
    private final Map<String, String> fractalIdMapping;
    private final int senderId;

    public JobMessage(int senderPort, int receiverPort, String messageText, Job job, Map<Integer, String> fractalIds,
                      Integer level, Job mainJob, Map<String, String> fractalIdMapping, int senderId) {
        super(MessageType.JOB, senderPort, receiverPort, messageText);

        this.job = job;
        this.fractalIds = fractalIds;
        this.level = level;
        this.mainJob = mainJob;
        this.fractalIdMapping = fractalIdMapping;
        this.senderId = senderId;
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

    public Job getMainJob() {
        return mainJob;
    }

    public Map<String, String> getFractalIdMapping() {
        return fractalIdMapping;
    }

    public int getSenderId() {
        return senderId;
    }
}
