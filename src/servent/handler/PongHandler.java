package servent.handler;

import app.AppConfig;
import servent.message.Message;
import servent.message.MessageType;

import java.sql.Timestamp;

public class PongHandler implements MessageHandler {

    private final Message clientMessage;

    public PongHandler(Message clientMessage) {
        this.clientMessage = clientMessage;
    }

    @Override
    public void run() {
        if (clientMessage.getMessageType() == MessageType.PONG) {
            int senderId = Integer.parseInt(clientMessage.getMessageText());
            AppConfig.chordState.getLastHeardMap().put(senderId, new Timestamp(System.currentTimeMillis()));
            AppConfig.chordState.getSuspiciousMap().put(senderId, false);
        }
    }
}
