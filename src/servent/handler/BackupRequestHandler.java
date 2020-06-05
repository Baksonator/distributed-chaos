package servent.handler;

import app.AppConfig;
import servent.message.BackupReplyMessage;
import servent.message.BackupRequestMessage;
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
            BackupRequestMessage backupRequestMessage = (BackupRequestMessage) clientMessage;
            int nodeId = Integer.parseInt(clientMessage.getMessageText());
            if (backupRequestMessage.isPred()) {
                BackupReplyMessage backupReplyMessage = new BackupReplyMessage(AppConfig.myServentInfo.getListenerPort(),
                        clientMessage.getSenderPort(), AppConfig.backupPredecessor, nodeId);
                backupReplyMessage.setSenderIp(AppConfig.myServentInfo.getIpAddress());
                backupReplyMessage.setReceiverIp(clientMessage.getSenderIp());
                MessageUtil.sendMessage(backupReplyMessage);
            } else {
                BackupReplyMessage backupReplyMessage = new BackupReplyMessage(AppConfig.myServentInfo.getListenerPort(),
                        clientMessage.getSenderPort(), AppConfig.backupSuccessor, nodeId);
                backupReplyMessage.setSenderIp(AppConfig.myServentInfo.getIpAddress());
                backupReplyMessage.setReceiverIp(clientMessage.getSenderIp());
                MessageUtil.sendMessage(backupReplyMessage);
            }
//            if (AppConfig.chordState.getPredecessor().getUuid() == nodeId) {
//                BackupReplyMessage backupReplyMessage = new BackupReplyMessage(AppConfig.myServentInfo.getListenerPort(),
//                        clientMessage.getSenderPort(), AppConfig.backupPredecessor, nodeId);
//                AppConfig.timestampedStandardPrint("PRED");
//                MessageUtil.sendMessage(backupReplyMessage);
//            } else {
//                AppConfig.timestampedStandardPrint("SUCC");
//                BackupReplyMessage backupReplyMessage = new BackupReplyMessage(AppConfig.myServentInfo.getListenerPort(),
//                        clientMessage.getSenderPort(), AppConfig.backupSuccessor, nodeId);
//                MessageUtil.sendMessage(backupReplyMessage);
//            }
        }
    }
}
