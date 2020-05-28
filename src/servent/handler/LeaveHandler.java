package servent.handler;

import app.AppConfig;
import app.ServentInfo;
import servent.message.LeaveMessage;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.util.MessageUtil;

public class LeaveHandler implements MessageHandler {

    private Message clientMessage;

    public LeaveHandler(Message clientMessage) {
        this.clientMessage = clientMessage;
    }

    @Override
    public void run() {
        if (clientMessage.getMessageType() == MessageType.LEAVE) {
            int leaverId = Integer.parseInt(clientMessage.getMessageText());
            ServentInfo leaverInfo = new ServentInfo("localhost", clientMessage.getSenderPort());
            leaverInfo.setUuid(leaverId);
            AppConfig.chordState.removeNode(leaverInfo);

            if (AppConfig.myServentInfo.getUuid() != leaverId - 1) {
                LeaveMessage leaveMessage = new LeaveMessage(clientMessage.getSenderPort(),
                        AppConfig.chordState.getNextNodePort(), Integer.toString(leaverId));
                MessageUtil.sendMessage(leaveMessage);
            }
        }
    }

}
