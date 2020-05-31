package cli.command;

import app.AppConfig;
import app.JobCommandHandler;
import servent.message.ResultRequestMessage;
import servent.message.util.MessageUtil;

import java.util.Map;

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

            int receiverId = -1;
            for (Map.Entry<Integer, String> entry : JobCommandHandler.fractalIds.entrySet()) {
                if (entry.getValue().equals("")) {
                    continue;
                }
                if (entry.getValue().substring(0, nameLen).equals(jobName)) {
                    receiverId = entry.getKey();
                    break;
                }
            }
            int lastId = -1;
            for (Map.Entry<Integer, String> entry : JobCommandHandler.fractalIds.entrySet()) {
                if (entry.getValue().equals("")) {
                    continue;
                }
                if (entry.getValue().substring(0, nameLen).equals(jobName)) {
                    lastId = entry.getKey();
                }
            }

            AppConfig.pendingResultJobName = jobName;
            ResultRequestMessage resultRequestMessage = new ResultRequestMessage(AppConfig.myServentInfo.getListenerPort(),
                    AppConfig.chordState.getNextNodeForKey(receiverId).getListenerPort(), receiverId + "," + lastId);
            MessageUtil.sendMessage(resultRequestMessage);
        } else {
            // TODO Dodaj i za fraktalniID
        }
    }
}
