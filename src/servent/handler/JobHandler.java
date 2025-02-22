package servent.handler;

import app.*;
import servent.message.*;
import servent.message.util.MessageUtil;

import java.util.*;

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
                    if (AppConfig.jobWorker != null) {
                        AppConfig.jobWorker.stop();
                    }

                    if (jobMsg.getFractalIdMapping() != null) {
                        String myOldFractalId = AppConfig.myServentInfo.getFractalId();
                        for (Map.Entry<String, String> entry : jobMsg.getFractalIdMapping().entrySet()) {
                            if (entry.getKey().equals(myOldFractalId)) {
                                Integer key = Utils.getKeyByValue(fractalIds, entry.getValue());
                                JobMigrationMessage jobMigrationMessage = new JobMigrationMessage(
                                        AppConfig.myServentInfo.getListenerPort(),
                                        AppConfig.chordState.getNextNodeForKey(key).getListenerPort(),
                                        Integer.toString(key), AppConfig.jobWorker.getResults());
                                jobMigrationMessage.setSenderIp(AppConfig.myServentInfo.getIpAddress());
                                jobMigrationMessage.setReceiverIp(AppConfig.chordState.getNextNodeForKey(key).getIpAddress());
                                MessageUtil.sendMessage(jobMigrationMessage);
                            }
                        }
                    }
                    JobCommandHandler.fractalIds = fractalIds;
                    AppConfig.myServentInfo.setFractalId("");
                    if (jobMsg.getMainJob() != null) {
                        AppConfig.activeJobs.add(jobMsg.getMainJob());
                        AppConfig.myMainJob = jobMsg.getMainJob();
                    }
                    JobMessageResponse jobMessageResponse = new JobMessageResponse(AppConfig.myServentInfo.getListenerPort(),
                            AppConfig.chordState.getNextNodeForKey(jobMsg.getSenderId()).getListenerPort(),
                            Integer.toString(jobMsg.getSenderId()));
                    jobMessageResponse.setSenderIp(AppConfig.myServentInfo.getIpAddress());
                    jobMessageResponse.setReceiverIp(AppConfig.chordState.getNextNodeForKey(jobMsg.getSenderId()).getIpAddress());
                    MessageUtil.sendMessage(jobMessageResponse);
                    return;
                }

                String myFractalId = fractalIds.get(AppConfig.myServentInfo.getUuid());
                int firstZero = myFractalId.indexOf("0");
                String justId = myFractalId.substring(firstZero);

                if (justId.length() - level == 1) {
                    if (jobMsg.getFractalIdMapping() != null) {
                        if (AppConfig.jobWorker != null) {
                            AppConfig.jobWorker.stop();
                        }

                        String myOldFractalId = AppConfig.myServentInfo.getFractalId();
//                        AppConfig.timestampedStandardPrint(jobMsg.getFractalIdMapping().toString());
//                        AppConfig.timestampedStandardPrint(sortByValue(jobMsg.getFractalIdMapping()).toString());
                        for (Map.Entry<String, String> entry : jobMsg.getFractalIdMapping().entrySet()) {
                            if (entry.getKey().equals(myOldFractalId)) {
                                Integer key = Utils.getKeyByValue(fractalIds, entry.getValue());
                                JobMigrationMessage jobMigrationMessage = new JobMigrationMessage(
                                        AppConfig.myServentInfo.getListenerPort(),
                                        AppConfig.chordState.getNextNodeForKey(key).getListenerPort(),
                                        Integer.toString(key), AppConfig.jobWorker.getResults());
                                jobMigrationMessage.setSenderIp(AppConfig.myServentInfo.getIpAddress());
                                jobMigrationMessage.setReceiverIp(AppConfig.chordState.getNextNodeForKey(key).getIpAddress());
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
                        if (jobMsg.getMainJob() != null) {
                            AppConfig.activeJobs.add(jobMsg.getMainJob());
                            AppConfig.myMainJob = jobMsg.getMainJob();
                        }
                        JobWorker worker = new JobWorker(job, newData);
                        AppConfig.jobWorker = worker;
                        Thread t = new Thread(worker);
                        t.start();
                        JobMessageResponse jobMessageResponse = new JobMessageResponse(AppConfig.myServentInfo.getListenerPort(),
                                AppConfig.chordState.getNextNodeForKey(jobMsg.getSenderId()).getListenerPort(),
                                Integer.toString(jobMsg.getSenderId()));
                        jobMessageResponse.setSenderIp(AppConfig.myServentInfo.getIpAddress());
                        jobMessageResponse.setReceiverIp(AppConfig.chordState.getNextNodeForKey(jobMsg.getSenderId()).getIpAddress());
                        MessageUtil.sendMessage(jobMessageResponse);
                    } else {
                        JobCommandHandler.fractalIds = fractalIds;
                        AppConfig.myServentInfo.setFractalId(myFractalId);
                        if (jobMsg.getMainJob() != null) {
                            AppConfig.activeJobs.add(jobMsg.getMainJob());
                            AppConfig.myMainJob = jobMsg.getMainJob();
                        }
                        JobWorker worker = new JobWorker(job);
                        AppConfig.jobWorker = worker;
                        Thread t = new Thread(worker);
                        t.start();
                        JobMessageResponse jobMessageResponse = new JobMessageResponse(AppConfig.myServentInfo.getListenerPort(),
                                AppConfig.chordState.getNextNodeForKey(jobMsg.getSenderId()).getListenerPort(),
                                Integer.toString(jobMsg.getSenderId()));
                        jobMessageResponse.setSenderIp(AppConfig.myServentInfo.getIpAddress());
                        jobMessageResponse.setReceiverIp(AppConfig.chordState.getNextNodeForKey(jobMsg.getSenderId()).getIpAddress());
                        MessageUtil.sendMessage(jobMessageResponse);
                    }
                } else {
                    ArrayList<Job> jobs = JobCommandHandler.prepareJobs(job);

                    int nodeCount = 0;
                    for (Map.Entry<Integer, String> entry : fractalIds.entrySet()) {
                        if (entry.getValue().equals("")) {
                            continue;
                        }
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
                    int k = 0;
                    for (ServentInfo serventInfo : AppConfig.chordState.getAllNodeInfoHelper()) {
                        if (serventInfo.getUuid() == AppConfig.myServentInfo.getUuid()) {
                            lastAssigned = k;
                            break;
                        }
                        k++;
                    }
                    for (int i = 0; i < job.getN(); i++) {
                        int receiverId = AppConfig.chordState.getAllNodeInfoHelper().get(lastAssigned).getUuid();
                        AppConfig.timestampedStandardPrint("Next node for key:" + receiverId + " is " + AppConfig.chordState.getNextNodeForKey(receiverId).getUuid());
                        JobMessage jobMessage = new JobMessage(AppConfig.myServentInfo.getListenerPort(),
                                AppConfig.chordState.getNextNodeForKey(receiverId).getListenerPort(),
                                Integer.toString(receiverId), jobs.get(i), fractalIds, level + 1, jobMsg.getMainJob(),
                                jobMsg.getFractalIdMapping(), jobMsg.getSenderId());
                        jobMessage.setSenderIp(AppConfig.myServentInfo.getIpAddress());
                        jobMessage.setReceiverIp(AppConfig.chordState.getNextNodeForKey(receiverId).getIpAddress());
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
                        jobMsg.getJob(), jobMsg.getFractalIds(), jobMsg.getLevel(), jobMsg.getMainJob(),
                        jobMsg.getFractalIdMapping(), jobMsg.getSenderId());
                jobMessage.setSenderIp(AppConfig.myServentInfo.getIpAddress());
                jobMessage.setReceiverIp(AppConfig.chordState.getNextNodeForKey(receiver).getIpAddress());
                MessageUtil.sendMessage(jobMessage);
            }
        }
    }
}
