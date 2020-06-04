package servent.handler;

import app.AppConfig;
import servent.message.BackupMessage;
import servent.message.Message;
import servent.message.MessageType;

import java.util.concurrent.CopyOnWriteArrayList;

public class BackupHandler implements MessageHandler {

    private final Message clientMessage;

    public BackupHandler(Message clientMessage) {
        this.clientMessage = clientMessage;
    }

    @Override
    public void run() {
        if (clientMessage.getMessageType() == MessageType.BACKUP) {
            BackupMessage backupMessage = (BackupMessage) clientMessage;
            if (AppConfig.chordState.getPredecessor().getUuid() == backupMessage.getNodeId()) {
                AppConfig.backupPredecessor = new CopyOnWriteArrayList<>(backupMessage.getBackUp());
            } else {
                AppConfig.backupSuccessor = new CopyOnWriteArrayList<>(backupMessage.getBackUp());
            }
        }
    }
}
