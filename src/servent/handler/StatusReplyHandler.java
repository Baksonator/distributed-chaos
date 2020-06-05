package servent.handler;

import app.AppConfig;
import app.Job;
import app.StatusResult;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.StatusReplyMessage;
import servent.message.util.MessageUtil;

import java.util.List;

public class StatusReplyHandler implements MessageHandler {

    private final Message clientMessage;

    public StatusReplyHandler(Message clientMessage) {
        this.clientMessage = clientMessage;
    }

    @Override
    public void run() {
        if (clientMessage.getMessageType() == MessageType.STATUS_REPLY) {
            int requestorId = Integer.parseInt(clientMessage.getMessageText());
            StatusReplyMessage statusReplyMessage = (StatusReplyMessage) clientMessage;
            if (requestorId == AppConfig.myServentInfo.getUuid()) {
                List<Integer> results = statusReplyMessage.getResults();
                List<String> ids = statusReplyMessage.getIds();
                Job myJob = statusReplyMessage.getJob();
                try {
                    AppConfig.statusResults.put(new StatusResult(results, ids, myJob));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else {
                StatusReplyMessage statusReplyMessage1 = new StatusReplyMessage(AppConfig.myServentInfo.getListenerPort(),
                        AppConfig.chordState.getNextNodeForKey(requestorId).getListenerPort(), Integer.toString(requestorId),
                        statusReplyMessage.getResults(), statusReplyMessage.getIds(), statusReplyMessage.getJob());
                statusReplyMessage1.setSenderIp(AppConfig.myServentInfo.getIpAddress());
                statusReplyMessage1.setReceiverIp(AppConfig.chordState.getNextNodeForKey(requestorId).getIpAddress());
                MessageUtil.sendMessage(statusReplyMessage1);
            }
        }
    }
}
