package servent.handler;

import app.AppConfig;
import servent.message.JobMessageResponse;
import servent.message.Message;
import servent.message.MessageType;
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
            } else {
                JobMessageResponse newJobMessageResponse = new JobMessageResponse(AppConfig.myServentInfo.getListenerPort(),
                        AppConfig.chordState.getNextNodeForKey(receiverId).getListenerPort(),
                        clientMessage.getMessageText());
                MessageUtil.sendMessage(newJobMessageResponse);
            }
        }
    }
}
