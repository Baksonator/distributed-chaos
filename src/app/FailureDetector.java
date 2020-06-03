package app;

import java.sql.Timestamp;
import java.util.Map;

public class FailureDetector implements Runnable, Cancellable {

    private volatile boolean working = true;

    @Override
    public void run() {
        while (working) {
            synchronized (AppConfig.pauseLock) {
                if (AppConfig.paused.get()) {
                    try {
                        AppConfig.pauseLock.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        return;
                    }

                    if (!working) {
                        break;
                    }
                }
            }

            try {
                Thread.sleep(AppConfig.SOFT_FAILURE_TIME * 2);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if (!working) {
                break;
            }

            long currTime = System.currentTimeMillis();

            for (Map.Entry<Integer, Timestamp> entry : AppConfig.chordState.getLastHeardMap().entrySet()) {
                if (currTime - entry.getValue().getTime() > AppConfig.SOFT_FAILURE_TIME) {
                    AppConfig.chordState.getSuspiciousMap().put(entry.getKey(), true);
                }
            }

            for (Map.Entry<Integer, Boolean> entry : AppConfig.chordState.getSuspiciousMap().entrySet()) {
                if (entry.getValue()) {
                    AppConfig.timestampedStandardPrint("Node with ID " + entry.getKey() + " is suspicious!");
                }
            }
        }
    }

    @Override
    public void stop() {
        this.working = false;
    }

}
