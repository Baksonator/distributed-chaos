package servent;

import app.AppConfig;
import app.Cancellable;
import cli.CLIParser;
import mutex.LogicalTimestamp;
import servent.message.Message;
import servent.message.MutexReleaseMessage;
import servent.message.MutexReplyMessage;
import servent.message.MutexRequestMessage;
import servent.message.util.MessageUtil;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class FIFOListener implements Runnable, Cancellable {

    private volatile boolean working = true;

    private CLIParser cliParser;

    @Override
    public void run() {
        ServerSocket listenerSocket = null;
        try {
            listenerSocket = new ServerSocket(AppConfig.myServentInfo.getListenerPort() + 1, 100);
            /*
             * If there is no connection after 1s, wake up and see if we should terminate.
             */
            listenerSocket.setSoTimeout(1000);
        } catch (IOException e) {
            AppConfig.timestampedErrorPrint("Couldn't open listener socket on: " + AppConfig.myServentInfo.getListenerPort());
            System.exit(0);
        }

        while (working) {
            try {
                Message clientMessage;

                Socket clientSocket = listenerSocket.accept();

                //GOT A MESSAGE! <3
                clientMessage = MessageUtil.readMessage(clientSocket);

                int senderId = -1;
                switch (clientMessage.getMessageType()) {
                    case MUTEX_REQUEST:
                        AppConfig.paused.set(true);
                        senderId = Integer.parseInt(clientMessage.getMessageText());
                        if (senderId != AppConfig.myServentInfo.getUuid()) {
                            MutexRequestMessage mutexRequestMessage = (MutexRequestMessage) clientMessage;
                            AppConfig.lamportClock.receiveAction(mutexRequestMessage.getLogicalTimestamp().getClock());
                            AppConfig.requestQueue.add(mutexRequestMessage.getLogicalTimestamp());
                            MutexRequestMessage newMutexRequestMessage = new MutexRequestMessage(AppConfig.myServentInfo.getListenerPort(),
                                    AppConfig.chordState.getNextNodePort(), clientMessage.getMessageText(),
                                    mutexRequestMessage.getLogicalTimestamp());
                            MessageUtil.sendMessage(newMutexRequestMessage);

                            AppConfig.lamportClock.tick();
                            MutexReplyMessage mutexReplyMessage = new MutexReplyMessage(AppConfig.myServentInfo.getListenerPort(),
                                    AppConfig.chordState.getNextNodeForKey(senderId).getListenerPort(), clientMessage.getMessageText(),
                                    new LogicalTimestamp(AppConfig.lamportClock.getValue(), AppConfig.myServentInfo.getUuid()));
                            MessageUtil.sendMessage(mutexReplyMessage);
                        }
                        break;
                    case MUTEX_REPLY:
                        int receiverId = Integer.parseInt(clientMessage.getMessageText());
                        if (receiverId == AppConfig.myServentInfo.getUuid()) {
                            MutexReplyMessage mutexReplyMessage = (MutexReplyMessage) clientMessage;
                            AppConfig.lamportClock.receiveAction(mutexReplyMessage.getLogicalTimestamp().getClock());
                            AppConfig.replyLatch.countDown();
                            AppConfig.paused.set(true);
                        } else {
                            MutexReplyMessage mutexReplyMessage = (MutexReplyMessage) clientMessage;
                            MutexReplyMessage newMutexReplyMessage = new MutexReplyMessage(AppConfig.myServentInfo.getListenerPort(),
                                    AppConfig.chordState.getNextNodeForKey(receiverId).getListenerPort(), clientMessage.getMessageText(),
                                    mutexReplyMessage.getLogicalTimestamp());
                            MessageUtil.sendMessage(newMutexReplyMessage);
                        }
                        break;
                    case MUTEX_RELEASE:
                        senderId = Integer.parseInt(clientMessage.getMessageText());
                        MutexReleaseMessage mutexReleaseMessage = (MutexReleaseMessage) clientMessage;
                        if (senderId != AppConfig.myServentInfo.getUuid()) {
                            AppConfig.lamportClock.receiveAction(mutexReleaseMessage.getLogicalTimestamp().getClock());
                            AppConfig.requestQueue.poll();
                            MutexReleaseMessage newMutexReleaseMessage = new MutexReleaseMessage(AppConfig.myServentInfo.getListenerPort(),
                                    AppConfig.chordState.getNextNodePort(), clientMessage.getMessageText(),
                                    mutexReleaseMessage.getLogicalTimestamp(), mutexReleaseMessage.isFlag());
                            MessageUtil.sendMessage(newMutexReleaseMessage);
                        } else if (mutexReleaseMessage.isFlag()) {
                            AppConfig.lamportClock.receiveAction(mutexReleaseMessage.getLogicalTimestamp().getClock());
                            AppConfig.requestQueue.poll();
                        }
                        if (AppConfig.requestQueue.size() == 0) {
                            AppConfig.paused.set(false);
                            synchronized (AppConfig.pauseLock) {
                                AppConfig.pauseLock.notifyAll();
                            }
                        }

                        break;
                }

            } catch (SocketTimeoutException e1) {
               // SMTH
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void stop() {
        this.working = false;
    }

}
