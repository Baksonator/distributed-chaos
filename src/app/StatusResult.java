package app;

import java.util.List;

public class StatusResult {

    private final List<Integer> results;
    private final List<String> ids;
    private final Job job;

    public StatusResult(List<Integer> results, List<String> ids, Job job) {
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
