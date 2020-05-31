package servent.handler;

import app.*;
import servent.message.JobMessage;
import servent.message.JobMigrationMessage;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.util.MessageUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class JobHandler implements MessageHandler {

    private final Message clientMessage;

    public JobHandler(Message clientMessage) {
        this.clientMessage = clientMessage;
    }

    @Override
    public void run() {
        if (clientMessage.getMessageType() == MessageType.JOB) {
            JobMessage jobMsg = (JobMessage) clientMessage;
            int receiver = Integer.parseInt(clientMessage.getMessageText());

            if (clientMessage.getMessageText().equals(Integer.toString(AppConfig.myServentInfo.getUuid()))) {
                Job job = jobMsg.getJob();
                Map<Integer, String> fractalIds = jobMsg.getFractalIds();
                int level = jobMsg.getLevel();

                if (job == null) {
                    if (jobMsg.getFractalIdMapping() != null) {
                        String myOldFractalId = AppConfig.myServentInfo.getFractalId();
                        for (Map.Entry<String, String> entry : jobMsg.getFractalIdMapping().entrySet()) {
                            if (entry.getKey().equals(myOldFractalId)) {
                                Integer key = getKeyByValue(fractalIds, entry.getValue());
                                JobMigrationMessage jobMigrationMessage = new JobMigrationMessage(
                                        AppConfig.myServentInfo.getListenerPort(),
                                        AppConfig.chordState.getNextNodeForKey(key).getListenerPort(),
                                        Integer.toString(key), AppConfig.jobWorker.getResults());
                                MessageUtil.sendMessage(jobMigrationMessage);
                            }
                        }

                        JobCommandHandler.fractalIds = fractalIds;
                        AppConfig.myServentInfo.setFractalId("");
                        AppConfig.activeJobs.add(jobMsg.getMainJob());
                    }
                    return;
                }

                String myFractalId = fractalIds.get(AppConfig.myServentInfo.getUuid());
                int firstZero = myFractalId.indexOf("0");
                String justId = myFractalId.substring(firstZero);

                if (justId.length() - level == 1) {
                    if (jobMsg.getFractalIdMapping() != null) {
                        String myOldFractalId = AppConfig.myServentInfo.getFractalId();
                        for (Map.Entry<String, String> entry : jobMsg.getFractalIdMapping().entrySet()) {
                            if (entry.getKey().equals(myOldFractalId)) {
                                Integer key = getKeyByValue(fractalIds, entry.getValue());
                                JobMigrationMessage jobMigrationMessage = new JobMigrationMessage(
                                        AppConfig.myServentInfo.getListenerPort(),
                                        AppConfig.chordState.getNextNodeForKey(key).getListenerPort(),
                                        Integer.toString(key), AppConfig.jobWorker.getResults());
                                MessageUtil.sendMessage(jobMigrationMessage);
                            }
                        }

                        int blockingCounter = 0;
                        for (Map.Entry<String, String> entry : jobMsg.getFractalIdMapping().entrySet()) {
                            if (entry.getValue().equals(myFractalId)) {
                                blockingCounter++;
                            }
                        }

                        List<Point> newData = new ArrayList<>();
                        while (blockingCounter > 0) {
                            try {
                                newData.addAll(AppConfig.incomingData.take());
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            blockingCounter--;
                        }

                        JobCommandHandler.fractalIds = fractalIds;
                        AppConfig.myServentInfo.setFractalId(myFractalId);
                        AppConfig.activeJobs.add(jobMsg.getMainJob());
                        JobWorker worker = new JobWorker(job, newData);
                        AppConfig.jobWorker = worker;
                        Thread t = new Thread(worker);
                        t.start();
                    } else {
                        JobCommandHandler.fractalIds = fractalIds;
                        AppConfig.myServentInfo.setFractalId(myFractalId);
                        AppConfig.activeJobs.add(jobMsg.getMainJob());
                        JobWorker worker = new JobWorker(job);
                        AppConfig.jobWorker = worker;
                        Thread t = new Thread(worker);
                        t.start();
                    }
                } else {
                    ArrayList<Job> jobs = JobCommandHandler.prepareJobs(job);

                    int nodeCount = 0;
                    for (Map.Entry<Integer, String> entry : fractalIds.entrySet()) {
                        int zero = entry.getValue().indexOf("0");
                        if (justId.substring(0, level + 1).equals(entry.getValue().substring(zero, zero + level + 1))) {
                            nodeCount++;
                        }
                    }
//                    fractalIds.forEach((integer, s) -> {
//                        if (justId.substring(0, level + 1).equals(s.substring(firstZero, level + 1))) {
//                            nodeCount++;
//                        }
//                    });
                    int dots = job.getN();

                    int needed = nodeCount;
                    int increment = dots - 1;
//                for (; needed + increment <= nodeCount.get(); needed += increment);

                    int baseFractalLevel = 1;
                    while (dots <= needed) {
                        baseFractalLevel++;
                        dots *= dots;
                    }

                    int overflowLevelNodes = (needed - ((int) Math.pow(job.getN(), baseFractalLevel - 1)));
                    overflowLevelNodes += (overflowLevelNodes / increment);

                    int lastAssigned = AppConfig.myServentInfo.getUuid();
                    for (int i = 0; i < job.getN(); i++) {
                        AppConfig.timestampedStandardPrint("Next node for key:" + lastAssigned + " is " + AppConfig.chordState.getNextNodeForKey(lastAssigned).getUuid());
                        JobMessage jobMessage = new JobMessage(AppConfig.myServentInfo.getListenerPort(),
                                AppConfig.chordState.getNextNodeForKey(lastAssigned).getListenerPort(),
                                Integer.toString(lastAssigned), jobs.get(i), fractalIds, level + 1, jobMsg.getMainJob(), jobMsg.getFractalIdMapping());
                        MessageUtil.sendMessage(jobMessage);

                        if (overflowLevelNodes > 0) {
                            lastAssigned += (int) Math.pow(job.getN(), baseFractalLevel - 1);
                            overflowLevelNodes -= job.getN();
                        } else {
                            lastAssigned += (int) Math.pow(job.getN(), baseFractalLevel - 2);
                        }
                    }
                }
            } else {
                AppConfig.timestampedStandardPrint("Next node for key:" + receiver + " is " + AppConfig.chordState.getNextNodeForKey(receiver).getUuid());
                JobMessage jobMessage = new JobMessage(AppConfig.myServentInfo.getListenerPort(),
                        AppConfig.chordState.getNextNodeForKey(receiver).getListenerPort(), clientMessage.getMessageText(),
                        jobMsg.getJob(), jobMsg.getFractalIds(), jobMsg.getLevel(), jobMsg.getMainJob(), jobMsg.getFractalIdMapping());
                MessageUtil.sendMessage(jobMessage);
            }
        }
    }

    private  <T, E> T getKeyByValue(Map<T, E> map, E value) {
        for (Map.Entry<T, E> entry : map.entrySet()) {
            if (Objects.equals(value, entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }
}
