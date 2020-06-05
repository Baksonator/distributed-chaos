package app;

import servent.message.BackupMessage;
import servent.message.util.MessageUtil;

public class BackupWorker implements Runnable, Cancellable {

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
                Thread.sleep(60000);
            } catch (InterruptedException e) {
//                e.printStackTrace();
            }

            if (!working) {
                break;
            }

            if (AppConfig.jobWorker != null && AppConfig.jobWorker.isWorking()) {
                if (AppConfig.chordState.getNodeCount() > 1) {
                    if (AppConfig.chordState.getNodeCount() == 2) {
                        BackupMessage backupMessage = new BackupMessage(AppConfig.myServentInfo.getListenerPort(),
                                AppConfig.chordState.getNextNodePort(), AppConfig.jobWorker.getResults(),
                                AppConfig.myServentInfo.getUuid());
                        backupMessage.setSenderIp(AppConfig.myServentInfo.getIpAddress());
                        backupMessage.setReceiverIp(AppConfig.chordState.getNextNodeIp());
                        MessageUtil.sendMessage(backupMessage);
                    } else {
                        BackupMessage backupMessage = new BackupMessage(AppConfig.myServentInfo.getListenerPort(),
                                AppConfig.chordState.getNextNodePort(), AppConfig.jobWorker.getResults(),
                                AppConfig.myServentInfo.getUuid());
                        backupMessage.setSenderIp(AppConfig.myServentInfo.getIpAddress());
                        backupMessage.setReceiverIp(AppConfig.chordState.getNextNodeIp());
                        MessageUtil.sendMessage(backupMessage);

                        BackupMessage backupMessage1 = new BackupMessage(AppConfig.myServentInfo.getListenerPort(),
                                AppConfig.chordState.getPredecessor().getListenerPort(), AppConfig.jobWorker.getResults(),
                                AppConfig.myServentInfo.getUuid());
                        backupMessage1.setSenderIp(AppConfig.myServentInfo.getIpAddress());
                        backupMessage1.setReceiverIp(AppConfig.chordState.getPredecessor().getIpAddress());
                        MessageUtil.sendMessage(backupMessage1);
                    }
                }
            }
        }
    }

    @Override
    public void stop() {
        this.working = false;
    }

}
