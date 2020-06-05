package cli.command;

import app.AppConfig;
import app.Job;
import app.JobCommandHandler;
import mutex.LogicalTimestamp;
import servent.message.MutexReleaseMessage;
import servent.message.MutexRequestMessage;
import servent.message.util.MessageUtil;

import java.util.concurrent.CountDownLatch;

public class StopJobCommand implements CLICommand {

    @Override
    public String commandName() {
        return "stop";
    }

    @Override
    public void execute(String args) {
        if (args == null) {
            AppConfig.timestampedErrorPrint("No job specified!");
            return;
        }

        Job job = AppConfig.jobs.stream().filter(job1 -> job1.getName().equals(args)).findFirst().get();
        if (!AppConfig.activeJobs.contains(job)) {
            AppConfig.timestampedErrorPrint("This job does not exist!");
            return;
        }

        try {
            AppConfig.localSemaphore.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        AppConfig.lamportClock.tick();
        LogicalTimestamp myRequestLogicalTimestamp = new LogicalTimestamp(AppConfig.lamportClock.getValue(),
                AppConfig.myServentInfo.getUuid());

        AppConfig.replyLatch = new CountDownLatch(AppConfig.chordState.getNodeCount() - 1);

        if (AppConfig.chordState.getNodeCount() > 1) {
            MutexRequestMessage mutexRequestMessage = new MutexRequestMessage(AppConfig.myServentInfo.getListenerPort(),
                    AppConfig.chordState.getNextNodePort(), Integer.toString(AppConfig.myServentInfo.getUuid()),
                    myRequestLogicalTimestamp);
            mutexRequestMessage.setSenderIp(AppConfig.myServentInfo.getIpAddress());
            mutexRequestMessage.setReceiverIp(AppConfig.chordState.getNextNodeIp());
            MessageUtil.sendMessage(mutexRequestMessage);
        }

        AppConfig.requestQueue.add(myRequestLogicalTimestamp);

        try {
            AppConfig.replyLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        while (!AppConfig.requestQueue.peek().equals(myRequestLogicalTimestamp)) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        AppConfig.isDesignated = false;

        AppConfig.jobLatch = new CountDownLatch(AppConfig.chordState.getNodeCount());
        JobCommandHandler.stop(job);

        try {
            AppConfig.jobLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        AppConfig.lamportClock.tick();
        AppConfig.requestQueue.poll();
        MutexReleaseMessage mutexReleaseMessage = new MutexReleaseMessage(AppConfig.myServentInfo.getListenerPort(),
                AppConfig.chordState.getNextNodePort(), Integer.toString(AppConfig.myServentInfo.getUuid()),
                new LogicalTimestamp(AppConfig.lamportClock.getValue(), AppConfig.myServentInfo.getUuid()), false);
        mutexReleaseMessage.setSenderIp(AppConfig.myServentInfo.getIpAddress());
        mutexReleaseMessage.setReceiverIp(AppConfig.chordState.getNextNodeIp());
        MessageUtil.sendMessage(mutexReleaseMessage);

        AppConfig.localSemaphore.release();
    }
}