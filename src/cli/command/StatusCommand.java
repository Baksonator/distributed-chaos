package cli.command;

import app.AppConfig;
import app.Job;
import app.JobCommandHandler;
import app.StatusCollector;
import servent.message.ResultRequestMessage;
import servent.message.StatusRequestMessage;
import servent.message.util.MessageUtil;

import java.util.Map;

public class StatusCommand implements CLICommand {

    @Override
    public String commandName() {
        return "status";
    }

    @Override
    public void execute(String args) {
        if (args == null) {
            AppConfig.isSingleId = false;
            Thread t = new Thread(new StatusCollector(AppConfig.activeJobs.size()));
            t.start();
            for (Job job : AppConfig.activeJobs) {
                String jobName = job.getName();
                int nameLen = jobName.length();

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

//                AppConfig.pendingResultJobName = jobName;
                StatusRequestMessage statusRequestMessage = new StatusRequestMessage(AppConfig.myServentInfo.getListenerPort(),
                        AppConfig.chordState.getNextNodeForKey(receiverId).getListenerPort(), receiverId + "," + lastId);
                MessageUtil.sendMessage(statusRequestMessage);
            }
        } else {
            String[] argsSplit = args.split(" ");
            if (argsSplit.length == 1) {
                String jobName = argsSplit[0];
                int nameLen = jobName.length();

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

                Thread t = new Thread(new StatusCollector(1));
                t.start();

                AppConfig.isSingleId = false;
                AppConfig.pendingResultJobName = jobName;
                StatusRequestMessage statusRequestMessage = new StatusRequestMessage(AppConfig.myServentInfo.getListenerPort(),
                        AppConfig.chordState.getNextNodeForKey(receiverId).getListenerPort(), receiverId + "," + lastId);
                MessageUtil.sendMessage(statusRequestMessage);
            } else {
                String jobName = argsSplit[0];
                String fractalId = argsSplit[1];
                String fullFractalId = jobName + fractalId;

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

                Thread t = new Thread(new StatusCollector(1));
                t.start();

                AppConfig.isSingleId = true;
                AppConfig.pendingResultJobName = jobName;
                StatusRequestMessage statusRequestMessage = new StatusRequestMessage(AppConfig.myServentInfo.getListenerPort(),
                        AppConfig.chordState.getNextNodeForKey(receiverId).getListenerPort(), receiverId + "," + receiverId);
                MessageUtil.sendMessage(statusRequestMessage);

            }
        }
    }
}
