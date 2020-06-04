package app;

import servent.message.PingMessage;
import servent.message.util.MessageUtil;

public class Pinger implements Runnable, Cancellable {

    private volatile boolean working = true;

    @Override
    public void run() {
        while (working) {
            synchronized (AppConfig.pauseLock) {
                if (AppConfig.paused.get()) {
                    try {
                        AppConfig.pauseLock.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        return;
                    }

                    if (!working) {
                        break;
                    }
                }
            }

            try {
                Thread.sleep(AppConfig.SOFT_FAILURE_TIME - 250);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if (!working) {
                break;
            }

            if (AppConfig.chordState.getNodeCount() > 1) {

                if (AppConfig.chordState.getNodeCount() == 2) {
                    PingMessage pingMessage = new PingMessage(AppConfig.myServentInfo.getListenerPort(),
                            AppConfig.chordState.getNextNodePort());
                    MessageUtil.sendMessage(pingMessage);

                } else {

                    for (ServentInfo neighbor : AppConfig.chordState.getSuccessorTableAlt()) {
                        PingMessage pingMessage = new PingMessage(AppConfig.myServentInfo.getListenerPort(),
                                neighbor.getListenerPort());
                        MessageUtil.sendMessage(pingMessage);
                    }

                    PingMessage pingMessage1 = new PingMessage(AppConfig.myServentInfo.getListenerPort(),
                            AppConfig.chordState.getPredecessor().getListenerPort());
                    MessageUtil.sendMessage(pingMessage1);

                    PingMessage pingMessage2 = new PingMessage(AppConfig.myServentInfo.getListenerPort(),
                            AppConfig.chordState.getAllNodeInfo().get(AppConfig.chordState.getNodeCount() - 3).getListenerPort());
                    MessageUtil.sendMessage(pingMessage2);

                }

            }
        }
    }

    @Override
    public void stop() {
        this.working = false;
    }

}
