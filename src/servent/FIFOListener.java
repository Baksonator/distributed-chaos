package servent;

import app.AppConfig;
import app.Cancellable;
import cli.CLIParser;
import servent.message.Message;
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

                switch (clientMessage.getMessageType()) {
                    case MUTEX_REQUEST:
                        break;
                    case MUTEX_REPLY:
                        break;
                    case MUTEX_RELEASE:
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
