package servent.handler;

import app.AppConfig;
import app.JobCommandHandler;
import app.ServentInfo;
import servent.message.*;
import servent.message.util.MessageUtil;

public class ResultRequestHandler implements MessageHandler {

    private final Message clientMessage;

    public ResultRequestHandler(Message clientMessage) {
        this.clientMessage = clientMessage;
    }

    @Override
    public void run() {
        if (clientMessage.getMessageType() == MessageType.RESULT_REQUEST) {
            int receiverId = Integer.parseInt(clientMessage.getMessageText().split(",")[0]);
            int lastId = Integer.parseInt(clientMessage.getMessageText().split(",")[1]);
            if (receiverId == AppConfig.myServentInfo.getUuid()) {
                if (lastId != AppConfig.myServentInfo.getUuid()) {
                    ResultCollectionMessage resultCollectionMessage = new ResultCollectionMessage(clientMessage.getSenderPort(),
                            AppConfig.chordState.getNextNodePort(), Integer.toString(lastId), AppConfig.jobWorker.getResults());
                    MessageUtil.sendMessage(resultCollectionMessage);
                } else {
                    int requestorId = -1;
                    for (ServentInfo nodeInfo : AppConfig.chordState.getAllNodeInfo()) {
                        // TODO Add IP Address as well
                        if (nodeInfo.getListenerPort() == clientMessage.getSenderPort()) {
                            requestorId = nodeInfo.getUuid();
                        }
                    }
                    if (AppConfig.myServentInfo.getListenerPort() == clientMessage.getSenderPort()) {
                        requestorId = AppConfig.myServentInfo.getUuid();
                    }
                    ResultReplyMessage resultReplyMessage = new ResultReplyMessage(AppConfig.myServentInfo.getListenerPort(),
                            AppConfig.chordState.getNextNodeForKey(requestorId).getListenerPort(), Integer.toString(requestorId),
                            AppConfig.jobWorker.getResults(), AppConfig.myMainJob);
                    MessageUtil.sendMessage(resultReplyMessage);
                }
            } else {
                ResultRequestMessage resultRequestMessage = new ResultRequestMessage(clientMessage.getSenderPort(),
                        AppConfig.chordState.getNextNodeForKey(receiverId).getListenerPort(), receiverId + "," + lastId);
                MessageUtil.sendMessage(resultRequestMessage);
            }
        }
    }
}
