package mutex;

import java.io.Serializable;
import java.util.Objects;

public class LogicalTimestamp implements Serializable, Comparable<LogicalTimestamp> {

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

    @Override
    public int compareTo(LogicalTimestamp o) {
        if (clock == o.clock) {
            return Integer.compare(uuid, o.uuid);
        } else {
            return Integer.compare(clock, o.clock);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LogicalTimestamp that = (LogicalTimestamp) o;
        return clock == that.clock &&
                uuid == that.uuid;
    }

    @Override
    public int hashCode() {
        return Objects.hash(clock, uuid);
    }
}
