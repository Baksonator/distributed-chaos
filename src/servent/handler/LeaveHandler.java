package servent.handler;

import app.AppConfig;
import app.JobCommandHandler;
import app.ServentInfo;
import cli.CLIParser;
import mutex.LogicalTimestamp;
import servent.SimpleServentListener;
import servent.message.LeaveMessage;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.MutexReleaseMessage;
import servent.message.util.FifoSendWorker;
import servent.message.util.MessageUtil;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class LeaveHandler implements MessageHandler {

    private Message clientMessage;
    private CLIParser cliParser;
    private SimpleServentListener simpleServentListener;

    public LeaveHandler(Message clientMessage, CLIParser cliParser, SimpleServentListener simpleServentListener) {
        this.clientMessage = clientMessage;
        this.cliParser = cliParser;
        this.simpleServentListener = simpleServentListener;
    }

    @Override
    public void run() {
        if (clientMessage.getMessageType() == MessageType.LEAVE) {
            int leaverId = Integer.parseInt(clientMessage.getMessageText());
            if (leaverId != AppConfig.myServentInfo.getUuid()) {
                LeaveMessage leaveMessage = (LeaveMessage) clientMessage;
                if (leaveMessage.isFirst()) {
                    AppConfig.jobLatch = new CountDownLatch(AppConfig.chordState.getNodeCount() - 1);
                    AppConfig.isDesignated = true;
                }

                AppConfig.failureDetector.setFlag(false);
                AppConfig.failureDetector.setSavedTime(-1);

                int oldPort = AppConfig.chordState.getNextNodePort();
                String oldIp = AppConfig.chordState.getNextNodeIp();

                ServentInfo leaverInfo = new ServentInfo(clientMessage.getSenderIp(), clientMessage.getSenderPort());
                leaverInfo.setUuid(leaverId);

                AppConfig.chordState.decrementNodeCount();
                AppConfig.chordState.updateLogLevel();

                AppConfig.chordState.removeNode(leaverInfo);
                AppConfig.chordState.getAllNodeInfoHelper().remove(leaverInfo);
                AppConfig.chordState.getSuspiciousMap().remove(leaverId);
                AppConfig.chordState.getLastHeardMap().remove(leaverId);
                AppConfig.timestampedStandardPrint(AppConfig.chordState.getAllNodeInfoHelper().toString());

                for (FifoSendWorker sendWorker : AppConfig.fifoSendWorkers) {
                    if (sendWorker.getNeighbor() == leaverId) {
                        sendWorker.stop();
                        AppConfig.fifoSendWorkers.remove(sendWorker);
                        break;
                    }
                }

                LeaveMessage newLeaveMessage = new LeaveMessage(AppConfig.myServentInfo.getListenerPort(),
                        oldPort, Integer.toString(leaverId), false);
                newLeaveMessage.setSenderIp(AppConfig.myServentInfo.getIpAddress());
                newLeaveMessage.setReceiverIp(oldIp);
                MessageUtil.sendMessage(newLeaveMessage);
            } else {
                AppConfig.chordState.decrementNodeCount();
                AppConfig.chordState.getAllNodeInfoHelper().remove(AppConfig.myServentInfo);

                if (AppConfig.activeJobs.size() > 0) {
//                    AppConfig.jobLatch = new CountDownLatch(AppConfig.chordState.getNodeCount());
                    JobCommandHandler.restructureDeparture();
//                    try {
//                        AppConfig.jobLatch.await();
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
                } else {
                    AppConfig.lamportClock.tick();
                    AppConfig.requestQueue.poll();
                    MutexReleaseMessage mutexReleaseMessage = new MutexReleaseMessage(AppConfig.myServentInfo.getListenerPort(),
                            AppConfig.chordState.getNextNodePort(), Integer.toString(AppConfig.chordState.getPredecessor().getUuid()),
                            new LogicalTimestamp(AppConfig.lamportClock.getValue(), AppConfig.myServentInfo.getUuid()), true);
                    mutexReleaseMessage.setSenderIp(AppConfig.myServentInfo.getIpAddress());
                    mutexReleaseMessage.setReceiverIp(AppConfig.chordState.getNextNodeIp());
                    MessageUtil.sendMessage(mutexReleaseMessage);
                }

//                AppConfig.lamportClock.tick();
//                AppConfig.requestQueue.poll();
//                MutexReleaseMessage mutexReleaseMessage = new MutexReleaseMessage(AppConfig.myServentInfo.getListenerPort(),
//                        AppConfig.chordState.getNextNodePort(), Integer.toString(AppConfig.myServentInfo.getUuid()),
//                        new LogicalTimestamp(AppConfig.lamportClock.getValue(), AppConfig.chordState.getPredecessor().getUuid()));
//                MessageUtil.sendMessage(mutexReleaseMessage);

                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                AppConfig.timestampedStandardPrint("Stopping...");
                for (FifoSendWorker senderWorker : AppConfig.fifoSendWorkers) {
                    senderWorker.stop();
                }
                AppConfig.paused.set(false);
                synchronized (AppConfig.pauseLock) {
                    AppConfig.pauseLock.notifyAll();
                }
                simpleServentListener.stop();
                AppConfig.fifoListener.stop();
                AppConfig.backupWorker.stop();
                AppConfig.pinger.stop();
                AppConfig.failureDetector.stop();
            }

        }
    }

}
