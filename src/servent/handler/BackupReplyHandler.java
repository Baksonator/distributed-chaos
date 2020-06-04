package servent.handler;

import app.AppConfig;
import servent.message.BackupReplyMessage;
import servent.message.Message;
import servent.message.MessageType;

public class BackupReplyHandler implements MessageHandler {

    private final Message clientMessage;

    public BackupReplyHandler(Message clientMessage) {
        this.clientMessage = clientMessage;
    }

    @Override
    public void run() {
        if (clientMessage.getMessageType() == MessageType.BACKUP_REPLY) {
            BackupReplyMessage backupReplyMessage = (BackupReplyMessage) clientMessage;
            try {
                AppConfig.backupsReceived.put(backupReplyMessage.getBackup());
                AppConfig.backupsReceivedIds.put(backupReplyMessage.getNodeId());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
