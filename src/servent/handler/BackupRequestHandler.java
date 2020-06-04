package servent.handler;

import app.AppConfig;
import servent.message.BackupReplyMessage;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.util.MessageUtil;

public class BackupRequestHandler implements MessageHandler {

    private final Message clientMessage;

    public BackupRequestHandler(Message clientMessage) {
        this.clientMessage = clientMessage;
    }

    @Override
    public void run() {
        if (clientMessage.getMessageType() == MessageType.BACKUP_REQUEST) {
            int nodeId = Integer.parseInt(clientMessage.getMessageText());
            if (AppConfig.chordState.getPredecessor().getUuid() == nodeId) {
                BackupReplyMessage backupReplyMessage = new BackupReplyMessage(AppConfig.myServentInfo.getListenerPort(),
                        clientMessage.getSenderPort(), AppConfig.backupPredecessor, nodeId);
                MessageUtil.sendMessage(backupReplyMessage);
            } else {
                BackupReplyMessage backupReplyMessage = new BackupReplyMessage(AppConfig.myServentInfo.getListenerPort(),
                        clientMessage.getSenderPort(), AppConfig.backupSuccessor, nodeId);
                MessageUtil.sendMessage(backupReplyMessage);
            }
        }
    }
}
