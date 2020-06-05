package servent.handler;

import app.AppConfig;
import app.ServentInfo;
import servent.message.DiedReplyMessage;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.util.FifoSendWorker;
import servent.message.util.MessageUtil;

public class DiedHandler implements MessageHandler {

    private final Message clientMessage;

    public DiedHandler(Message clientMessage) {
        this.clientMessage = clientMessage;
    }

    @Override
    public void run() {
        if (clientMessage.getMessageType() == MessageType.DIED) {
            int alsoDied = Integer.parseInt(clientMessage.getMessageText());

            ServentInfo leaverInfo = null;
            for (ServentInfo serventInfo : AppConfig.chordState.getAllNodeInfo()) {
                if (serventInfo.getUuid() == alsoDied) {
                    leaverInfo = serventInfo;
                    break;
                }
            }

            leaverInfo.setUuid(alsoDied);

            AppConfig.chordState.decrementNodeCount();
            AppConfig.chordState.updateLogLevel();

            AppConfig.chordState.removeNode(leaverInfo);
            AppConfig.chordState.getAllNodeInfoHelper().remove(leaverInfo);
            AppConfig.chordState.getSuspiciousMap().remove(alsoDied);
            AppConfig.chordState.getLastHeardMap().remove(alsoDied);

            for (FifoSendWorker sendWorker : AppConfig.fifoSendWorkers) {
                if (sendWorker.getNeighbor() == alsoDied) {
                    sendWorker.stop();
                    AppConfig.fifoSendWorkers.remove(sendWorker);
                    break;
                }
            }

            DiedReplyMessage diedReplyMessage = new DiedReplyMessage(AppConfig.myServentInfo.getListenerPort(),
                    clientMessage.getSenderPort(), Integer.toString(AppConfig.myDied.get()), AppConfig.myServentInfo.getUuid());
            diedReplyMessage.setSenderIp(AppConfig.myServentInfo.getIpAddress());
            diedReplyMessage.setReceiverIp(clientMessage.getSenderIp());
            MessageUtil.sendMessage(diedReplyMessage);
        }
    }
}
