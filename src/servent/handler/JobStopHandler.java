package servent.handler;

import app.*;
import servent.message.*;
import servent.message.util.MessageUtil;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class JobStopHandler implements MessageHandler {

    private final Message clientMessage;

    public JobStopHandler(Message clientMessage) {
        this.clientMessage = clientMessage;
    }

    @Override
    public void run() {
        if (clientMessage.getMessageType() == MessageType.JOB_STOP) {
            JobStopMessage jobStopMsg = (JobStopMessage) clientMessage;
            int receiver = Integer.parseInt(clientMessage.getMessageText());

            if (clientMessage.getMessageText().equals(Integer.toString(AppConfig.myServentInfo.getUuid()))) {
                Job job = jobStopMsg.getJob();
                Map<Integer, String> fractalIds = jobStopMsg.getFractalIds();
                int level = jobStopMsg.getLevel();

                if (job == null) {
                    if (AppConfig.jobWorker != null) {
                        AppConfig.jobWorker.stop();
                    }

                    if (jobStopMsg.getFractalIdMapping() != null) {
                        String myOldFractalId = AppConfig.myServentInfo.getFractalId();

                        int numberToSendTo = 0;
                        for (Map.Entry<String, String> entry : jobStopMsg.getFractalIdMapping().entrySet()) {
                            if (entry.getValue().equals(myOldFractalId)) {
                                numberToSendTo++;
                            }
                        }

                        if (numberToSendTo == 1) {
                            for (Map.Entry<String, String> entry : jobStopMsg.getFractalIdMapping().entrySet()) {
                                if (entry.getValue().equals(myOldFractalId)) {
                                    Integer key = Utils.getKeyByValue(fractalIds, entry.getKey());
                                    JobMigrationMessage jobMigrationMessage = new JobMigrationMessage(
                                            AppConfig.myServentInfo.getListenerPort(),
                                            AppConfig.chordState.getNextNodeForKey(key).getListenerPort(),
                                            Integer.toString(key), AppConfig.jobWorker.getResults());
                                    MessageUtil.sendMessage(jobMigrationMessage);
                                }
                            }
                        } else if (numberToSendTo > 1) {
                            int copyOfNumberToSendTo = numberToSendTo;
                            ArrayList<Job> jobsToSend;
                            int logLevel = 0;
                            while (numberToSendTo != 1) {
                                numberToSendTo /= AppConfig.jobWorker.getJob().getN();
                                logLevel++;
                            }
                            jobsToSend = JobCommandHandler.prepareJobs(AppConfig.jobWorker.getJob());
                            while (logLevel > 1) {
                                ArrayList<Job> tempJobs = new ArrayList<>(jobsToSend);
                                jobsToSend.clear();
                                for (Job job1 : tempJobs) {
                                    jobsToSend.addAll(JobCommandHandler.prepareJobs(job1));
                                }
                                logLevel--;
                            }
                            int k = 0;
                            double p;
                            if (jobStopMsg.getMainJob() != null) {
                                p = jobStopMsg.getMainJob().getP();
                            } else {
                                p = AppConfig.jobWorker.getJob().getP();
                            }
                            if (p <= 0.5) {
                                TreeMap<String, String> sortedFractalIds = new TreeMap<>(jobStopMsg.getFractalIdMapping());
                                for (Map.Entry<String, String> entry : sortedFractalIds.entrySet()) {
                                    if (entry.getValue().equals(myOldFractalId)) {
                                        Integer key = Utils.getKeyByValue(fractalIds, entry.getKey());
                                        JobMigrationMessage jobMigrationMessage = new JobMigrationMessage(
                                                AppConfig.myServentInfo.getListenerPort(),
                                                AppConfig.chordState.getNextNodeForKey(key).getListenerPort(),
                                                Integer.toString(key), Utils.inPolygon(jobsToSend.get(k++), AppConfig.jobWorker.getResults()));
                                        MessageUtil.sendMessage(jobMigrationMessage);
                                    }
                                }
                            } else {
                                ArrayList<List<Point>> toSend = new ArrayList<>();
                                List<Point> toSendList = List.copyOf(AppConfig.jobWorker.getResults());
                                int chunkSize = toSendList.size() / copyOfNumberToSendTo;
                                for (int i = 0; i < copyOfNumberToSendTo - 1; i++) {
                                    toSend.add(toSendList.subList(i * chunkSize, (i + 1) * chunkSize));
                                }
                                toSend.add(toSendList.subList((copyOfNumberToSendTo - 1) * chunkSize, toSendList.size()));

                                int l = 0;
                                TreeMap<String, String> sortedFractalIds = new TreeMap<>(jobStopMsg.getFractalIdMapping());
                                for (Map.Entry<String, String> entry : sortedFractalIds.entrySet()) {
                                    if (entry.getValue().equals(myOldFractalId)) {
                                        List<Point> sending = new CopyOnWriteArrayList<>(toSend.get(l++));
                                        Integer key = Utils.getKeyByValue(fractalIds, entry.getKey());
                                        JobMigrationMessage jobMigrationMessage = new JobMigrationMessage(
                                                AppConfig.myServentInfo.getListenerPort(),
                                                AppConfig.chordState.getNextNodeForKey(key).getListenerPort(),
                                                Integer.toString(key), sending);
                                        MessageUtil.sendMessage(jobMigrationMessage);
                                    }
                                }
                            }
                        }

                    }
                    JobCommandHandler.fractalIds = fractalIds;
                    AppConfig.myServentInfo.setFractalId("");
                    if (jobStopMsg.getMainJob() != null) {
                        AppConfig.activeJobs.remove(jobStopMsg.getMainJob());
                        AppConfig.myMainJob = jobStopMsg.getMainJob();
                    }
                    JobMessageResponse jobMessageResponse = new JobMessageResponse(AppConfig.myServentInfo.getListenerPort(),
                            AppConfig.chordState.getNextNodeForKey(jobStopMsg.getSenderId()).getListenerPort(),
                            Integer.toString(jobStopMsg.getSenderId()));
                    MessageUtil.sendMessage(jobMessageResponse);
                    return;
                }

                String myFractalId = fractalIds.get(AppConfig.myServentInfo.getUuid());
                int firstZero = myFractalId.indexOf("0");
                String justId = myFractalId.substring(firstZero);

                if (justId.length() - level == 1) {
                    if (jobStopMsg.getFractalIdMapping() != null) {
                        if (AppConfig.jobWorker != null) {
                            AppConfig.jobWorker.stop();
                        }

                        String myOldFractalId = AppConfig.myServentInfo.getFractalId();

                        int numberToSendTo = 0;
                        for (Map.Entry<String, String> entry : jobStopMsg.getFractalIdMapping().entrySet()) {
                            if (entry.getValue().equals(myOldFractalId)) {
                                numberToSendTo++;
                            }
                        }

                        if (numberToSendTo == 1) {
                            for (Map.Entry<String, String> entry : jobStopMsg.getFractalIdMapping().entrySet()) {
                                if (entry.getValue().equals(myOldFractalId)) {
                                    Integer key = Utils.getKeyByValue(fractalIds, entry.getKey());
                                    JobMigrationMessage jobMigrationMessage = new JobMigrationMessage(
                                            AppConfig.myServentInfo.getListenerPort(),
                                            AppConfig.chordState.getNextNodeForKey(key).getListenerPort(),
                                            Integer.toString(key), AppConfig.jobWorker.getResults());
                                    MessageUtil.sendMessage(jobMigrationMessage);
                                }
                            }
                        } else if (numberToSendTo > 1) {
                            int copyOfNumberToSendTo = numberToSendTo;
                            ArrayList<Job> jobsToSend;
                            int logLevel = 0;
                            while (numberToSendTo != 1) {
                                numberToSendTo /= AppConfig.jobWorker.getJob().getN();
                                logLevel++;
                            }
                            jobsToSend = JobCommandHandler.prepareJobs(AppConfig.jobWorker.getJob());
                            while (logLevel > 1) {
                                ArrayList<Job> tempJobs = new ArrayList<>(jobsToSend);
                                jobsToSend.clear();
                                for (Job job1 : tempJobs) {
                                    jobsToSend.addAll(JobCommandHandler.prepareJobs(job1));
                                }
                                logLevel--;
                            }

                            int k = 0;
                            if (job.getP() <= 0.5) {
                                TreeMap<String, String> sortedFractalIds = new TreeMap<>(jobStopMsg.getFractalIdMapping());
                                for (Map.Entry<String, String> entry : sortedFractalIds.entrySet()) {
                                    if (entry.getValue().equals(myOldFractalId)) {
                                        Integer key = Utils.getKeyByValue(fractalIds, entry.getKey());
                                        JobMigrationMessage jobMigrationMessage = new JobMigrationMessage(
                                                AppConfig.myServentInfo.getListenerPort(),
                                                AppConfig.chordState.getNextNodeForKey(key).getListenerPort(),
                                                Integer.toString(key), Utils.inPolygon(jobsToSend.get(k++), AppConfig.jobWorker.getResults()));
                                        MessageUtil.sendMessage(jobMigrationMessage);
                                    }
                                }
                            } else {
                                ArrayList<List<Point>> toSend = new ArrayList<>();
                                int chunkSize = AppConfig.jobWorker.getResults().size() / copyOfNumberToSendTo;
                                for (int i = 0; i < copyOfNumberToSendTo - 1; i++) {
                                    toSend.add(AppConfig.jobWorker.getResults().subList(i * chunkSize, (i + 1) * chunkSize));
                                }
                                toSend.add(AppConfig.jobWorker.getResults().subList((copyOfNumberToSendTo - 1) * chunkSize, AppConfig.jobWorker.getResults().size()));

                                int l = 0;
                                TreeMap<String, String> sortedFractalIds = new TreeMap<>(jobStopMsg.getFractalIdMapping());
                                for (Map.Entry<String, String> entry : sortedFractalIds.entrySet()) {
                                    if (entry.getValue().equals(myOldFractalId)) {
                                        List<Point> sending = new CopyOnWriteArrayList<>(toSend.get(l++));
                                        Integer key = Utils.getKeyByValue(fractalIds, entry.getKey());
                                        JobMigrationMessage jobMigrationMessage = new JobMigrationMessage(
                                                AppConfig.myServentInfo.getListenerPort(),
                                                AppConfig.chordState.getNextNodeForKey(key).getListenerPort(),
                                                Integer.toString(key), sending);
                                        MessageUtil.sendMessage(jobMigrationMessage);
                                    }
                                }
                            }
                        }

                        int blockingCounter = 0;
                        for (Map.Entry<String, String> entry : jobStopMsg.getFractalIdMapping().entrySet()) {
                            if (entry.getKey().equals(myFractalId)) {
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
                        if (jobStopMsg.getMainJob() != null) {
                            AppConfig.activeJobs.remove(jobStopMsg.getMainJob());
                            AppConfig.myMainJob = jobStopMsg.getMainJob();
                        }
                        JobWorker worker = new JobWorker(job, newData);
                        AppConfig.jobWorker = worker;
                        Thread t = new Thread(worker);
                        t.start();
                        JobMessageResponse jobMessageResponse = new JobMessageResponse(AppConfig.myServentInfo.getListenerPort(),
                                AppConfig.chordState.getNextNodeForKey(jobStopMsg.getSenderId()).getListenerPort(),
                                Integer.toString(jobStopMsg.getSenderId()));
                        MessageUtil.sendMessage(jobMessageResponse);
                    } else {
                        JobCommandHandler.fractalIds = fractalIds;
                        AppConfig.myServentInfo.setFractalId(myFractalId);
                        if (jobStopMsg.getMainJob() != null) {
                            AppConfig.activeJobs.remove(jobStopMsg.getMainJob());
                            AppConfig.myMainJob = jobStopMsg.getMainJob();
                        }
                        JobWorker worker = new JobWorker(job);
                        AppConfig.jobWorker = worker;
                        Thread t = new Thread(worker);
                        t.start();
                        JobMessageResponse jobMessageResponse = new JobMessageResponse(AppConfig.myServentInfo.getListenerPort(),
                                AppConfig.chordState.getNextNodeForKey(jobStopMsg.getSenderId()).getListenerPort(),
                                Integer.toString(jobStopMsg.getSenderId()));
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
                        JobStopMessage jobMessage = new JobStopMessage(AppConfig.myServentInfo.getListenerPort(),
                                AppConfig.chordState.getNextNodeForKey(receiverId).getListenerPort(),
                                Integer.toString(receiverId), jobs.get(i), fractalIds, level + 1, jobStopMsg.getMainJob(),
                                jobStopMsg.getFractalIdMapping(), jobStopMsg.getSenderId());
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
                JobStopMessage jobMessage = new JobStopMessage(AppConfig.myServentInfo.getListenerPort(),
                        AppConfig.chordState.getNextNodeForKey(receiver).getListenerPort(), clientMessage.getMessageText(),
                        jobStopMsg.getJob(), jobStopMsg.getFractalIds(), jobStopMsg.getLevel(), jobStopMsg.getMainJob(),
                        jobStopMsg.getFractalIdMapping(), jobStopMsg.getSenderId());
                MessageUtil.sendMessage(jobMessage);
            }
        }
    }

}
