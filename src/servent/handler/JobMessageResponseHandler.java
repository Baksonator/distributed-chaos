package servent.handler;

import app.AppConfig;
import mutex.LogicalTimestamp;
import servent.message.*;
import servent.message.util.MessageUtil;

public class JobMessageResponseHandler implements MessageHandler {

    private final Message clientMessage;

    public JobMessageResponseHandler(Message clientMessage) {
        this.clientMessage = clientMessage;
    }

    @Override
    public void run() {
        if (clientMessage.getMessageType() == MessageType.JOB_MESSAGE_RESPONSE) {
            JobMessageResponse jobMessageResponse = (JobMessageResponse) clientMessage;
            int receiverId = Integer.parseInt(jobMessageResponse.getMessageText());
            if (receiverId == AppConfig.myServentInfo.getUuid()) {
                AppConfig.jobLatch.countDown();
                if (AppConfig.isDesignated) {
                    if (AppConfig.jobLatch.getCount() == 0) {
                        AppConfig.isDesignated = false;
                        AppConfig.lamportClock.tick();
                        AppConfig.requestQueue.poll();
                        MutexReleaseMessage mutexReleaseMessage = new MutexReleaseMessage(AppConfig.myServentInfo.getListenerPort(),
                                AppConfig.chordState.getNextNodePort(), Integer.toString(AppConfig.myServentInfo.getUuid()),
                                new LogicalTimestamp(AppConfig.lamportClock.getValue(), AppConfig.myServentInfo.getUuid()), false);
                        mutexReleaseMessage.setSenderIp(AppConfig.myServentInfo.getIpAddress());
                        mutexReleaseMessage.setReceiverIp(AppConfig.chordState.getNextNodeIp());
                        MessageUtil.sendMessage(mutexReleaseMessage);
                    }
                }
            } else {
                JobMessageResponse newJobMessageResponse = new JobMessageResponse(AppConfig.myServentInfo.getListenerPort(),
                        AppConfig.chordState.getNextNodeForKey(receiverId).getListenerPort(),
                        clientMessage.getMessageText());
                newJobMessageResponse.setSenderIp(AppConfig.myServentInfo.getIpAddress());
                newJobMessageResponse.setReceiverIp(AppConfig.chordState.getNextNodeForKey(receiverId).getIpAddress());
                MessageUtil.sendMessage(newJobMessageResponse);
            }
        }
    }
}
