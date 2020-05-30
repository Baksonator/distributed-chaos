package servent.handler;

import app.AppConfig;
import app.Job;
import app.JobCommandHandler;
import app.JobWorker;
import servent.message.JobMessage;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.util.MessageUtil;

import java.util.ArrayList;
import java.util.Map;

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

                String myFractalId = fractalIds.get(AppConfig.myServentInfo.getUuid());
                int firstZero = myFractalId.indexOf("0");
                String justId = myFractalId.substring(firstZero);

                if (justId.length() - level == 1) {
                    JobCommandHandler.fractalIds = fractalIds;
                    AppConfig.myServentInfo.setFractalId(myFractalId);
                    JobWorker worker = new JobWorker(job);
                    AppConfig.jobWorker = worker;
                    Thread t = new Thread(worker);
                    t.start();
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
                                Integer.toString(lastAssigned), jobs.get(i), fractalIds, level + 1);
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
                        jobMsg.getJob(), jobMsg.getFractalIds(), jobMsg.getLevel());
                MessageUtil.sendMessage(jobMessage);
            }
        }
    }
}
