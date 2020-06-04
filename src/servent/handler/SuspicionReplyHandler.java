package servent.handler;

import app.AppConfig;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.SuspicionReplyMessage;

public class SuspicionReplyHandler implements MessageHandler {

    private final Message clientMessage;

    public SuspicionReplyHandler(Message clientMessage) {
        this.clientMessage = clientMessage;
    }

    @Override
    public void run() {
        if (clientMessage.getMessageType() == MessageType.SUSPICION_REPLY) {
//            int receiverId =
            SuspicionReplyMessage suspicionReplyMessage = (SuspicionReplyMessage) clientMessage;
            if (suspicionReplyMessage.isSuspicious()) {
                AppConfig.chordState.getReallySuspucious().put(suspicionReplyMessage.getNodeId(), true);
            }
        }
    }
}
