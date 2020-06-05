package app;

import servent.message.SuspicionRequestMessage;
import servent.message.util.MessageUtil;

import java.sql.Timestamp;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

public class FailureDetector implements Runnable, Cancellable {

    private volatile boolean working = true;
    private long savedTime = -1;
    private boolean flag = false;

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
                        if (AppConfig.chordState.getNodeCount() == 3) {
                            JobCommandHandler.failure3_1(entry.getKey());
                        } else {
                            JobCommandHandler.failure(entry.getKey());
                        }
                    }
                }
            }

            if (AppConfig.chordState.getNodeCount() > 1) {
                if (AppConfig.chordState.getNodeCount() == 2) {

                    if (AppConfig.chordState.getSuspiciousMap() != null &&
                            AppConfig.chordState.getSuspiciousMap().containsKey(AppConfig.chordState.getSuccessorTableAlt().get(0).getUuid()) &&
                            AppConfig.chordState.getSuspiciousMap().get(AppConfig.chordState.getSuccessorTableAlt().get(0).getUuid()) != null) {

                        if (savedTime == -1) {
                            savedTime = System.currentTimeMillis();
                        }

                        if (AppConfig.chordState.getSuspiciousMap().containsKey(AppConfig.chordState.getSuccessorTableAlt().get(0).getUuid()) &&
                                AppConfig.chordState.getSuspiciousMap().get(AppConfig.chordState.getSuccessorTableAlt().get(0).getUuid())) {

                            if (!flag) {
                                flag = true;
                                savedTime = System.currentTimeMillis();
                            } else {

                                if (System.currentTimeMillis() - savedTime > AppConfig.HARD_FAILURE_TIME) {
                                    AppConfig.timestampedStandardPrint("Some nodes failed");
                                    JobCommandHandler.failure2_1();
                                    if (AppConfig.chordState.getSuccessorTableAlt().size() > 0) {
                                        AppConfig.chordState.getSuspiciousMap().put(AppConfig.chordState.getSuccessorTableAlt().get(0).getUuid(), false);
                                    }
                                    savedTime = System.currentTimeMillis();
                                }
                            }

                        } else {
                            AppConfig.chordState.getLastHeardMap().put(AppConfig.chordState.getSuccessorTableAlt().get(0).getUuid(), new Timestamp(System.currentTimeMillis()));
                            flag = false;
                        }

                    }

                } else if (AppConfig.chordState.getNodeCount() == 3) {

                    if (AppConfig.chordState.getSuspiciousMap() != null && AppConfig.chordState.getSuspiciousMap().containsKey(AppConfig.chordState.getPredecessor().getUuid()) &&
                            AppConfig.chordState.getSuspiciousMap().get(AppConfig.chordState.getPredecessor().getUuid()) != null &&
                            AppConfig.chordState.getSuspiciousMap().containsKey(AppConfig.chordState.getSuccessorTableAlt().get(0).getUuid()) &&
                            AppConfig.chordState.getSuspiciousMap().get(AppConfig.chordState.getSuccessorTableAlt().get(0).getUuid()) != null) {

                        if (savedTime == -1) {
                            savedTime = System.currentTimeMillis();
                        }

                        if (AppConfig.chordState.getSuspiciousMap().containsKey(AppConfig.chordState.getPredecessor().getUuid()) &&
                                AppConfig.chordState.getSuspiciousMap().containsKey(AppConfig.chordState.getSuccessorTableAlt().get(0).getUuid()) &&
                                AppConfig.chordState.getSuspiciousMap().get(AppConfig.chordState.getPredecessor().getUuid()) &&
                                AppConfig.chordState.getSuspiciousMap().get(AppConfig.chordState.getSuccessorTableAlt().get(0).getUuid())) {

                            if (!flag) {
                                flag = true;
                                savedTime = System.currentTimeMillis();
                            } else {

                                if (System.currentTimeMillis() - savedTime > AppConfig.HARD_FAILURE_TIME) {
                                    AppConfig.timestampedStandardPrint("Some nodes failed");
                                    JobCommandHandler.failure2_3();
                                    if (AppConfig.chordState.getSuccessorTableAlt().size() > 0) {
                                        AppConfig.chordState.getSuspiciousMap().put(AppConfig.chordState.getSuccessorTableAlt().get(0).getUuid(), false);
                                    }
                                    if (AppConfig.chordState.getPredecessor() != null) {
                                        AppConfig.chordState.getSuspiciousMap().put(AppConfig.chordState.getPredecessor().getUuid(), false);
                                    }
                                    savedTime = System.currentTimeMillis();
                                }
                            }

                        } else {
                            flag = false;
                            if (AppConfig.chordState.getSuspiciousMap().containsKey(AppConfig.chordState.getSuccessorTableAlt().get(0).getUuid()) &&
                                    AppConfig.chordState.getSuspiciousMap().get(AppConfig.chordState.getSuccessorTableAlt().get(0).getUuid())) {

                                SuspicionRequestMessage suspicionRequestMessage = new SuspicionRequestMessage(AppConfig.myServentInfo.getListenerPort(),
                                        AppConfig.chordState.getPredecessor().getListenerPort(), Integer.toString(AppConfig.chordState.getPredecessor().getUuid()),
                                        AppConfig.myServentInfo.getUuid(), AppConfig.chordState.getSuccessorTableAlt().get(0).getUuid());
                                suspicionRequestMessage.setSenderIp(AppConfig.myServentInfo.getIpAddress());
                                suspicionRequestMessage.setReceiverIp(AppConfig.chordState.getPredecessor().getIpAddress());
                                MessageUtil.sendMessage(suspicionRequestMessage);
                            }
                        }
                    }

                } else {
                    if (AppConfig.chordState.getSuspiciousMap() != null && AppConfig.chordState.getSuspiciousMap().containsKey(AppConfig.chordState.getPredecessor().getUuid()) &&
                            AppConfig.chordState.getSuspiciousMap().get(AppConfig.chordState.getPredecessor().getUuid()) != null &&
                            AppConfig.chordState.getSuspiciousMap().containsKey(AppConfig.chordState.getSuccessorTableAlt().get(0).getUuid()) &&
                            AppConfig.chordState.getSuspiciousMap().get(AppConfig.chordState.getSuccessorTableAlt().get(0).getUuid()) != null) {

                        if (AppConfig.chordState.getSuspiciousMap().containsKey(AppConfig.chordState.getPredecessor().getUuid()) &&
                                AppConfig.chordState.getSuspiciousMap().get(AppConfig.chordState.getPredecessor().getUuid()) &&
                                AppConfig.chordState.getSuspiciousMap().containsKey(AppConfig.chordState.getSuccessorTableAlt().get(0).getUuid()) &&
                                AppConfig.chordState.getSuspiciousMap().get(AppConfig.chordState.getSuccessorTableAlt().get(0).getUuid())) {

                            SuspicionRequestMessage suspicionRequestMessage = new SuspicionRequestMessage(AppConfig.myServentInfo.getListenerPort(),
                                    AppConfig.chordState.getSuccessorTableAlt().get(1).getListenerPort(),
                                    Integer.toString(AppConfig.chordState.getPredecessor().getUuid()),
                                    AppConfig.myServentInfo.getUuid(), AppConfig.chordState.getSuccessorTableAlt().get(0).getUuid());
                            suspicionRequestMessage.setSenderIp(AppConfig.myServentInfo.getIpAddress());
                            suspicionRequestMessage.setReceiverIp(AppConfig.chordState.getSuccessorTableAlt().get(1).getIpAddress());
                            MessageUtil.sendMessage(suspicionRequestMessage);

                        } else if (AppConfig.chordState.getSuspiciousMap().containsKey(AppConfig.chordState.getSuccessorTableAlt().get(0).getUuid()) &&
                                AppConfig.chordState.getSuspiciousMap().get(AppConfig.chordState.getSuccessorTableAlt().get(0).getUuid())) {

                            SuspicionRequestMessage suspicionRequestMessage = new SuspicionRequestMessage(AppConfig.myServentInfo.getListenerPort(),
                                    AppConfig.chordState.getPredecessor().getListenerPort(), Integer.toString(AppConfig.chordState.getPredecessor().getUuid()),
                                    AppConfig.myServentInfo.getUuid(), AppConfig.chordState.getSuccessorTableAlt().get(0).getUuid());
                            suspicionRequestMessage.setSenderIp(AppConfig.myServentInfo.getIpAddress());
                            suspicionRequestMessage.setReceiverIp(AppConfig.chordState.getPredecessor().getIpAddress());
                            MessageUtil.sendMessage(suspicionRequestMessage);

                        }
                    }
                }
            }

//            for (Map.Entry<Integer, Boolean> entry : AppConfig.chordState.getSuspiciousMap().entrySet()) {
//                if (entry.getValue()) {
////                    AppConfig.timestampedStandardPrint("Node with ID " + entry.getKey() + " is suspicious!");
//                }
//            }
        }
    }

    public void setSavedTime(long savedTime) {
        this.savedTime = savedTime;
    }

    public void setFlag(boolean flag) {
        this.flag = flag;
    }

    @Override
    public void stop() {
        this.working = false;
    }

}
