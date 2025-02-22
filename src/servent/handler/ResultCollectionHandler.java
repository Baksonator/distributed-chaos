package servent.handler;

import app.AppConfig;
import app.JobCommandHandler;
import app.ServentInfo;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.ResultCollectionMessage;
import servent.message.ResultReplyMessage;
import servent.message.util.MessageUtil;

import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ResultCollectionHandler implements MessageHandler {

    private final Message clientMessage;

    public ResultCollectionHandler(Message clientMessage) {
        this.clientMessage = clientMessage;
    }

    @Override
    public void run() {
        if (clientMessage.getMessageType() == MessageType.RESULT_COLLECTION) {
            int lastId = Integer.parseInt(clientMessage.getMessageText());

            ResultCollectionMessage oldMsg = (ResultCollectionMessage) clientMessage;

            if (lastId != AppConfig.myServentInfo.getUuid()) {
                ResultCollectionMessage resultCollectionMessage = new ResultCollectionMessage(clientMessage.getSenderPort(),
                        AppConfig.chordState.getNextNodePort(), clientMessage.getMessageText(),
                        Stream.concat(AppConfig.jobWorker.getResults().stream(),
                                oldMsg.getResults().stream()).collect(Collectors.toList()));
                resultCollectionMessage.setSenderIp(clientMessage.getSenderIp());
                resultCollectionMessage.setReceiverIp(AppConfig.chordState.getNextNodeIp());
                MessageUtil.sendMessage(resultCollectionMessage);
            } else {
                int requestorId = -1;
                for (ServentInfo nodeInfo : AppConfig.chordState.getAllNodeInfo()) {
                    if (nodeInfo.getIpAddress().equals(clientMessage.getSenderIp()) && nodeInfo.getListenerPort() == clientMessage.getSenderPort()) {
                        requestorId = nodeInfo.getUuid();
                        break;
                    }
                }
                if (AppConfig.myServentInfo.getIpAddress().equals(clientMessage.getSenderIp()) && AppConfig.myServentInfo.getListenerPort() == clientMessage.getSenderPort()) {
                    requestorId = AppConfig.myServentInfo.getUuid();
                }
                ResultReplyMessage resultReplyMessage = new ResultReplyMessage(AppConfig.myServentInfo.getListenerPort(),
                        AppConfig.chordState.getNextNodeForKey(requestorId).getListenerPort(), Integer.toString(requestorId),
                        Stream.concat(AppConfig.jobWorker.getResults().stream(),
                                oldMsg.getResults().stream()).collect(Collectors.toList()), AppConfig.jobWorker.getJob(),
                        false, "");
                resultReplyMessage.setSenderIp(AppConfig.myServentInfo.getIpAddress());
                resultReplyMessage.setReceiverIp(AppConfig.chordState.getNextNodeForKey(requestorId).getIpAddress());
                MessageUtil.sendMessage(resultReplyMessage);
            }
        }
    }
}
