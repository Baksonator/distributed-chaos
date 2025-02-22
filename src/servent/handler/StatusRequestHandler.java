package servent.handler;

import app.AppConfig;
import app.ServentInfo;
import servent.message.*;
import servent.message.util.MessageUtil;

import java.util.ArrayList;

public class StatusRequestHandler implements MessageHandler {

    private final Message clientMessage;

    public StatusRequestHandler(Message clientMessage) {
        this.clientMessage = clientMessage;
    }

    @Override
    public void run() {
        if (clientMessage.getMessageType() == MessageType.STATUS_REQUEST) {
            int receiverId = Integer.parseInt(clientMessage.getMessageText().split(",")[0]);
            int lastId = Integer.parseInt(clientMessage.getMessageText().split(",")[1]);
            if (receiverId == AppConfig.myServentInfo.getUuid()) {
                if (lastId != AppConfig.myServentInfo.getUuid()) {
                    ArrayList<Integer> results = new ArrayList<>();
                    results.add(AppConfig.jobWorker.getResults().size());
                    ArrayList<String> ids = new ArrayList<>();
                    ids.add(AppConfig.myServentInfo.getFractalId());
                    StatusCollectionMessage statusCollectionMessage = new StatusCollectionMessage(clientMessage.getSenderPort(),
                            AppConfig.chordState.getNextNodePort(), Integer.toString(lastId),
                            results, ids);
                    statusCollectionMessage.setSenderIp(clientMessage.getSenderIp());
                    statusCollectionMessage.setReceiverIp(AppConfig.chordState.getNextNodeIp());
                    MessageUtil.sendMessage(statusCollectionMessage);
                } else {
                    int requestorId = -1;
                    for (ServentInfo nodeInfo : AppConfig.chordState.getAllNodeInfo()) {
                        if (nodeInfo.getIpAddress().equals(clientMessage.getSenderIp()) && nodeInfo.getListenerPort() == clientMessage.getSenderPort()) {
                            requestorId = nodeInfo.getUuid();
                        }
                    }
                    if (AppConfig.myServentInfo.getIpAddress().equals(clientMessage.getSenderIp()) && AppConfig.myServentInfo.getListenerPort() == clientMessage.getSenderPort()) {
                        requestorId = AppConfig.myServentInfo.getUuid();
                    }
                    ArrayList<Integer> result = new ArrayList<>();
                    result.add(AppConfig.jobWorker.getResults().size());
                    ArrayList<String> ids = new ArrayList<>();
                    ids.add(AppConfig.myServentInfo.getFractalId());
                    StatusReplyMessage statusReplyMessage = new StatusReplyMessage(AppConfig.myServentInfo.getListenerPort(),
                            AppConfig.chordState.getNextNodeForKey(requestorId).getListenerPort(), Integer.toString(requestorId),
                            result, ids, AppConfig.jobWorker.getJob());
                    statusReplyMessage.setSenderIp(AppConfig.myServentInfo.getIpAddress());
                    statusReplyMessage.setReceiverIp(AppConfig.chordState.getNextNodeForKey(receiverId).getIpAddress());
                    MessageUtil.sendMessage(statusReplyMessage);
                }
            } else {
                StatusRequestMessage statusRequestMessage = new StatusRequestMessage(clientMessage.getSenderPort(),
                        AppConfig.chordState.getNextNodeForKey(receiverId).getListenerPort(), receiverId + "," + lastId);
                statusRequestMessage.setSenderIp(clientMessage.getSenderIp());
                statusRequestMessage.setReceiverIp(AppConfig.chordState.getNextNodeForKey(receiverId).getIpAddress());
                MessageUtil.sendMessage(statusRequestMessage);
            }
        }
    }
}
