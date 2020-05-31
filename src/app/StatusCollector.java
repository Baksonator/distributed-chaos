package app;

import java.util.ArrayList;
import java.util.List;

public class StatusCollector implements Runnable {

    private int numberOfReponses;

    public StatusCollector(int numberOfReponses) {
        this.numberOfReponses = numberOfReponses;
    }

    @Override
    public void run() {
        List<StatusResult> allResults = new ArrayList<>();
        while (numberOfReponses > 0) {
            numberOfReponses--;
            try {
                allResults.add(AppConfig.statusResults.take());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        for (StatusResult statusResult : allResults) {
            if (!AppConfig.isSingleId) {
                AppConfig.timestampedStandardPrint("STATUS FOR JOB: " + statusResult.getJob().getName());
                AppConfig.timestampedStandardPrint("NUMBER OF NODES WORKING: " + statusResult.getIds().size());
                for (int i = 0; i < statusResult.getIds().size(); i++) {
                    AppConfig.timestampedStandardPrint("NUMBER OF POINTS FOR ID: " + statusResult.getIds().get(i) + " IS "
                            + statusResult.getResults().get(i));
                }
            } else {
                AppConfig.timestampedStandardPrint("NUMBER OF POINTS FOR ID: " + statusResult.getIds().get(0) + " IS "
                        + statusResult.getResults().get(0));
            }
        }
    }
}
