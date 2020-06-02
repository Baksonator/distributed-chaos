package servent.handler;

import app.AppConfig;
import mutex.LogicalTimestamp;
import servent.message.MutexReleaseMessage;
import servent.message.util.MessageUtil;

public class ReleaseEntryHandler implements MessageHandler {

    @Override
    public void run() {
        AppConfig.lamportClock.tick();
        AppConfig.requestQueue.poll();
        MutexReleaseMessage mutexReleaseMessage = new MutexReleaseMessage(AppConfig.myServentInfo.getListenerPort(),
                AppConfig.chordState.getNextNodePort(), Integer.toString(AppConfig.myServentInfo.getUuid()),
                new LogicalTimestamp(AppConfig.lamportClock.getValue(), AppConfig.myServentInfo.getUuid()), false);
        MessageUtil.sendMessage(mutexReleaseMessage);
        AppConfig.localSemaphore.release();
    }
}
