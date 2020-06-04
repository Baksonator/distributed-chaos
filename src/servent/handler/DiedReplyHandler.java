package servent.handler;

import app.AppConfig;
import servent.message.DiedReplyMessage;
import servent.message.Message;
import servent.message.MessageType;

public class DiedReplyHandler implements MessageHandler {

    private final Message clientMessage;

    public DiedReplyHandler(Message clientMessage) {
        this.clientMessage = clientMessage;
    }

    @Override
    public void run() {
        if (clientMessage.getMessageType() == MessageType.DIED_REPLY) {
            DiedReplyMessage diedReplyMessage = (DiedReplyMessage) clientMessage;
            int alsoDied = Integer.parseInt(diedReplyMessage.getMessageText());
            if (alsoDied != -1) {
                AppConfig.alsoDied.set(alsoDied);
                AppConfig.diedLatch.countDown();
            }
            AppConfig.diedLatch.countDown();
        }
    }
}
