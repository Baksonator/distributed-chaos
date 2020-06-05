package cli.command;

import app.AppConfig;
import app.Job;
import app.JobCommandHandler;
import mutex.LogicalTimestamp;
import servent.message.MutexRequestMessage;
import servent.message.ResultRequestMessage;
import servent.message.util.MessageUtil;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

public class ResultCommand implements CLICommand {

    @Override
    public String commandName() {
        return "result";
    }

    @Override
    public void execute(String args) {
        String[] splitArgs = args.split(" ");
        if (splitArgs.length == 1) {
            String jobName = splitArgs[0];
            int nameLen = jobName.length();

            Job dummy = new Job(jobName, 0, 0, 0, 0, new ArrayList<>());
            if (!AppConfig.activeJobs.contains(dummy)) {
                AppConfig.timestampedErrorPrint("Job does not exist!");
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

            int receiverId = -1;
            for (Map.Entry<Integer, String> entry : JobCommandHandler.fractalIds.entrySet()) {
                if (entry.getValue().equals("")) {
                    continue;
                }
                String realJobName = entry.getValue().substring(0, entry.getValue().indexOf("0"));
                if (realJobName.equals(jobName)) {
                    receiverId = entry.getKey();
                    break;
                }
            }
            int lastId = -1;
            for (Map.Entry<Integer, String> entry : JobCommandHandler.fractalIds.entrySet()) {
                if (entry.getValue().equals("")) {
                    continue;
                }
                String realJobName = entry.getValue().substring(0, entry.getValue().indexOf("0"));
                if (realJobName.equals(jobName)) {
                    lastId = entry.getKey();
                }
            }

            AppConfig.pendingResultJobName = jobName;
            ResultRequestMessage resultRequestMessage = new ResultRequestMessage(AppConfig.myServentInfo.getListenerPort(),
                    AppConfig.chordState.getNextNodeForKey(receiverId).getListenerPort(), receiverId + "," + lastId);
            resultRequestMessage.setSenderIp(AppConfig.myServentInfo.getIpAddress());
            resultRequestMessage.setReceiverIp(AppConfig.chordState.getNextNodeForKey(receiverId).getIpAddress());
            MessageUtil.sendMessage(resultRequestMessage);
        } else {
            String jobName = splitArgs[0];
            String fractalId = splitArgs[1];
            String fullFractalId = jobName + fractalId;

            Job dummy = new Job(jobName, 0, 0, 0, 0, new ArrayList<>());
            if (!AppConfig.activeJobs.contains(dummy)) {
                AppConfig.timestampedErrorPrint("Job does not exist!");
                return;
            }

            int receiverId = -1;
            for (Map.Entry<Integer, String> entry : JobCommandHandler.fractalIds.entrySet()) {
                if (entry.getValue().equals("")) {
                    continue;
                }
                if (entry.getValue().equals(fullFractalId)) {
                    receiverId = entry.getKey();
                    break;
                }
            }

            if (receiverId == -1) {
                AppConfig.timestampedErrorPrint("Fractal ID does not exist!");
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

            ResultRequestMessage resultRequestMessage = new ResultRequestMessage(AppConfig.myServentInfo.getListenerPort(),
                    AppConfig.chordState.getNextNodeForKey(receiverId).getListenerPort(), receiverId + "," + receiverId);
            resultRequestMessage.setSenderIp(AppConfig.myServentInfo.getIpAddress());
            resultRequestMessage.setReceiverIp(AppConfig.chordState.getNextNodeForKey(receiverId).getIpAddress());
            MessageUtil.sendMessage(resultRequestMessage);

            AppConfig.pendingResultJobName = jobName;
        }
    }
}
