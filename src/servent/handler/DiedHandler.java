package servent.handler;

import app.AppConfig;
import servent.message.DiedReplyMessage;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.util.MessageUtil;

public class DiedHandler implements MessageHandler {

    private final Message clientMessage;

    public DiedHandler(Message clientMessage) {
        this.clientMessage = clientMessage;
    }

    @Override
    public void run() {
        if (clientMessage.getMessageType() == MessageType.DIED) {
            // TODO Remove from successor table
            DiedReplyMessage diedReplyMessage = new DiedReplyMessage(AppConfig.myServentInfo.getListenerPort(),
                    clientMessage.getSenderPort(), Integer.toString(AppConfig.myDied.get()), AppConfig.myServentInfo.getUuid());
            MessageUtil.sendMessage(diedReplyMessage);
        }
    }
}
