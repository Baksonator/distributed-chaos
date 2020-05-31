package servent.handler;

import app.AppConfig;
import app.ServentInfo;
import servent.message.*;
import servent.message.util.MessageUtil;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StatusCollectionHandler implements MessageHandler {

    private final Message clientMessage;

    public StatusCollectionHandler(Message clientMessage) {
        this.clientMessage = clientMessage;
    }

    @Override
    public void run() {
        if (clientMessage.getMessageType() == MessageType.STATUS_COLLECTION) {
            int lastId = Integer.parseInt(clientMessage.getMessageText());

            StatusCollectionMessage oldMsg = (StatusCollectionMessage) clientMessage;

            if (lastId != AppConfig.myServentInfo.getUuid()) {
                List<Integer> newResults = oldMsg.getResults();
                newResults.add(AppConfig.jobWorker.getResults().size());
                List<String> newIds = oldMsg.getIds();
                newIds.add(AppConfig.myServentInfo.getFractalId());
                StatusCollectionMessage statusCollectionMessage = new StatusCollectionMessage(clientMessage.getSenderPort(),
                        AppConfig.chordState.getNextNodePort(), clientMessage.getMessageText(), newResults, newIds);
                MessageUtil.sendMessage(statusCollectionMessage);
            } else {
                int requestorId = -1;
                for (ServentInfo nodeInfo : AppConfig.chordState.getAllNodeInfo()) {
                    // TODO Add IP Address as well
                    if (nodeInfo.getListenerPort() == clientMessage.getSenderPort()) {
                        requestorId = nodeInfo.getUuid();
                        break;
                    }
                }
                if (AppConfig.myServentInfo.getListenerPort() == clientMessage.getSenderPort()) {
                    requestorId = AppConfig.myServentInfo.getUuid();
                }
                List<Integer> newResults = oldMsg.getResults();
                newResults.add(AppConfig.jobWorker.getResults().size());
                List<String> newIds = oldMsg.getIds();
                newIds.add(AppConfig.myServentInfo.getFractalId());
                StatusReplyMessage statusReplyMessage = new StatusReplyMessage(AppConfig.myServentInfo.getListenerPort(),
                        AppConfig.chordState.getNextNodeForKey(requestorId).getListenerPort(), Integer.toString(requestorId),
                        newResults, newIds, AppConfig.myMainJob);
                MessageUtil.sendMessage(statusReplyMessage);
            }
        }
    }
}
