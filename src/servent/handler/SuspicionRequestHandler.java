package servent.handler;

import app.AppConfig;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.SuspicionReplyMessage;
import servent.message.SuspicionRequestMessage;
import servent.message.util.MessageUtil;

public class SuspicionRequestHandler implements MessageHandler {

    private final Message clientMessage;

    public SuspicionRequestHandler(Message clientMessage) {
        this.clientMessage = clientMessage;
    }

    @Override
    public void run() {
        if (clientMessage.getMessageType() == MessageType.SUSPICION_REQUEST) {
            SuspicionRequestMessage suspicionRequestMessage = (SuspicionRequestMessage) clientMessage;
            SuspicionReplyMessage suspicionReplyMessage = new SuspicionReplyMessage(AppConfig.myServentInfo.getListenerPort(),
                    clientMessage.getSenderPort(),
                    Integer.toString(suspicionRequestMessage.getSenderId()),
                    AppConfig.chordState.getSuspiciousMap().get(suspicionRequestMessage.getInquiryNodeId()),
                    suspicionRequestMessage.getInquiryNodeId());
            suspicionReplyMessage.setSenderIp(AppConfig.myServentInfo.getIpAddress());
            suspicionReplyMessage.setReceiverIp(clientMessage.getSenderIp());
            MessageUtil.sendMessage(suspicionReplyMessage);
        }
    }
}
