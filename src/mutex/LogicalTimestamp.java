package mutex;

import java.io.Serializable;

public class LogicalTimestamp implements Serializable {

    private static final long serialVersionUID = -7891416409229937206L;

    private final int clock;
    private final int uuid;

    public LogicalTimestamp(int clock, int uuid) {
        this.clock = clock;
        this.uuid = uuid;
    }

    public int getClock() {
        return clock;
    }

    public int getUuid() {
        return uuid;
    }
}
