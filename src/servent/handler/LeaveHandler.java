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
                int oldPort = AppConfig.chordState.getNextNodePort();

                ServentInfo leaverInfo = new ServentInfo("localhost", clientMessage.getSenderPort());
                leaverInfo.setUuid(leaverId);

                AppConfig.chordState.decrementNodeCount();
                AppConfig.chordState.updateLogLevel();

                AppConfig.chordState.removeNode(leaverInfo);
                AppConfig.chordState.getAllNodeInfoHelper().remove(leaverInfo);

                for (FifoSendWorker sendWorker : AppConfig.fifoSendWorkers) {
                    if (sendWorker.getNeighbor() == leaverId) {
                        sendWorker.stop();
                        AppConfig.fifoSendWorkers.remove(sendWorker);
                        break;
                    }
                }

                LeaveMessage leaveMessage = new LeaveMessage(AppConfig.myServentInfo.getListenerPort(),
                        oldPort, Integer.toString(leaverId));
                MessageUtil.sendMessage(leaveMessage);
            } else {
                AppConfig.chordState.decrementNodeCount();
                AppConfig.chordState.getAllNodeInfoHelper().remove(AppConfig.myServentInfo);

                if (AppConfig.activeJobs.size() > 0) {
                    AppConfig.jobLatch = new CountDownLatch(AppConfig.chordState.getNodeCount());
                    JobCommandHandler.restructureDeparture();
                    try {
                        AppConfig.jobLatch.await();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                AppConfig.lamportClock.tick();
                AppConfig.requestQueue.poll();
                MutexReleaseMessage mutexReleaseMessage = new MutexReleaseMessage(AppConfig.myServentInfo.getListenerPort(),
                        AppConfig.chordState.getNextNodePort(), Integer.toString(AppConfig.myServentInfo.getUuid()),
                        new LogicalTimestamp(AppConfig.lamportClock.getValue(), AppConfig.chordState.getPredecessor().getUuid()));
                MessageUtil.sendMessage(mutexReleaseMessage);

                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                AppConfig.timestampedStandardPrint("Stopping...");
                for (FifoSendWorker senderWorker : AppConfig.fifoSendWorkers) {
                    senderWorker.stop();
                }
                simpleServentListener.stop();
                AppConfig.fifoListener.stop();
            }

        }
    }

}
