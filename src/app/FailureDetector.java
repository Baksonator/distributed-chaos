package app;

import servent.message.SuspicionRequestMessage;
import servent.message.util.MessageUtil;

import java.sql.Timestamp;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

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

            AppConfig.myDied.set(-1);

            long currTime = System.currentTimeMillis();

            for (Map.Entry<Integer, Timestamp> entry : AppConfig.chordState.getLastHeardMap().entrySet()) {
                if (currTime - entry.getValue().getTime() > AppConfig.SOFT_FAILURE_TIME) {
                    AppConfig.chordState.getSuspiciousMap().put(entry.getKey(), true);
                }
                if (currTime - entry.getValue().getTime() > AppConfig.HARD_FAILURE_TIME) {
                    if (AppConfig.chordState.getReallySuspucious().containsKey(entry.getKey())) {
                        AppConfig.timestampedStandardPrint("NODE " + entry.getKey() + " DIEDED");
                        AppConfig.myDied.set(entry.getKey());
                        AppConfig.diedLatch = new CountDownLatch(AppConfig.chordState.getNodeCount() - 2);
                        JobCommandHandler.failure(entry.getKey());
                    }
                }
            }

            if (AppConfig.chordState.getNodeCount() > 1) {
                if (AppConfig.chordState.getNodeCount() == 2) {

                } else if (AppConfig.chordState.getNodeCount() == 3) {

                } else {
                    if (AppConfig.chordState.getSuspiciousMap() != null && AppConfig.chordState.getSuspiciousMap().get(AppConfig.chordState.getPredecessor().getUuid()) != null &&
                        AppConfig.chordState.getSuspiciousMap().get(AppConfig.chordState.getSuccessorTableAlt().get(0).getUuid()) != null) {

                        if (AppConfig.chordState.getSuspiciousMap().get(AppConfig.chordState.getPredecessor().getUuid()) &&
                                AppConfig.chordState.getSuspiciousMap().get(AppConfig.chordState.getSuccessorTableAlt().get(0).getUuid())) {

                            SuspicionRequestMessage suspicionRequestMessage = new SuspicionRequestMessage(AppConfig.myServentInfo.getListenerPort(),
                                    AppConfig.chordState.getSuccessorTableAlt().get(1).getListenerPort(),
                                    Integer.toString(AppConfig.chordState.getPredecessor().getUuid()),
                                    AppConfig.myServentInfo.getUuid(), AppConfig.chordState.getSuccessorTableAlt().get(0).getUuid());
                            MessageUtil.sendMessage(suspicionRequestMessage);

                        } else if (AppConfig.chordState.getSuspiciousMap().get(AppConfig.chordState.getSuccessorTableAlt().get(0).getUuid())) {

                            SuspicionRequestMessage suspicionRequestMessage = new SuspicionRequestMessage(AppConfig.myServentInfo.getListenerPort(),
                                    AppConfig.chordState.getPredecessor().getListenerPort(), Integer.toString(AppConfig.chordState.getPredecessor().getUuid()),
                                    AppConfig.myServentInfo.getUuid(), AppConfig.chordState.getSuccessorTableAlt().get(0).getUuid());
                            MessageUtil.sendMessage(suspicionRequestMessage);

                        }
                    }
                }
            }

            for (Map.Entry<Integer, Boolean> entry : AppConfig.chordState.getSuspiciousMap().entrySet()) {
                if (entry.getValue()) {
//                    AppConfig.timestampedStandardPrint("Node with ID " + entry.getKey() + " is suspicious!");
                }
            }
        }
    }

    @Override
    public void stop() {
        this.working = false;
    }

}
