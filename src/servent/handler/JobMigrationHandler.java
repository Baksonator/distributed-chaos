package servent.handler;

import app.AppConfig;
import servent.message.JobMigrationMessage;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.util.MessageUtil;

public class JobMigrationHandler implements MessageHandler {

    private final Message clientMessage;

    public JobMigrationHandler(Message clientMessage) {
        this.clientMessage = clientMessage;
    }

    @Override
    public void run() {
        if (clientMessage.getMessageType() == MessageType.JOB_MIGRATION) {
            int receiverId = Integer.parseInt(clientMessage.getMessageText());
            JobMigrationMessage jobMigrationMessage = (JobMigrationMessage) clientMessage;
            if (receiverId == AppConfig.myServentInfo.getUuid()) {

                try {
                    AppConfig.incomingData.put(jobMigrationMessage.getData());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            } else {
                JobMigrationMessage jobMigrationMessageNew = new JobMigrationMessage(AppConfig.myServentInfo.getListenerPort(),
                        AppConfig.chordState.getNextNodeForKey(receiverId).getListenerPort(), Integer.toString(receiverId),
                        jobMigrationMessage.getData());
                jobMigrationMessageNew.setSenderIp(AppConfig.myServentInfo.getIpAddress());
                jobMigrationMessageNew.setReceiverIp(AppConfig.chordState.getNextNodeForKey(receiverId).getIpAddress());
                MessageUtil.sendMessage(jobMigrationMessageNew);
            }
        }
    }
}
