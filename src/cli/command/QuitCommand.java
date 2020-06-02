package cli.command;

import app.AppConfig;
import cli.CLIParser;
import mutex.LogicalTimestamp;
import servent.SimpleServentListener;
import servent.message.LeaveMessage;
import servent.message.MutexRequestMessage;
import servent.message.util.MessageUtil;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.*;
import java.util.concurrent.CountDownLatch;

public class QuitCommand implements CLICommand {

    private final CLIParser parser;
    private final SimpleServentListener listener;

    public QuitCommand(CLIParser parser, SimpleServentListener listener) {
        this.parser = parser;
        this.listener = listener;
    }

    @Override
    public String commandName() {
        return "quit";
    }

    @Override
    public void execute(String args) {
        contactBootstrap();

        AppConfig.lamportClock.tick();
        LogicalTimestamp myRequestLogicalTimestamp = new LogicalTimestamp(AppConfig.lamportClock.getValue(),
                AppConfig.myServentInfo.getUuid());

        AppConfig.replyLatch = new CountDownLatch(AppConfig.chordState.getNodeCount() - 1);

        if (AppConfig.chordState.getNodeCount() > 1) {
            MutexRequestMessage mutexRequestMessage = new MutexRequestMessage(AppConfig.myServentInfo.getListenerPort(),
                    AppConfig.chordState.getNextNodePort(), Integer.toString(AppConfig.myServentInfo.getUuid()),
                    myRequestLogicalTimestamp);
            MessageUtil.sendMessage(mutexRequestMessage);
        }

        AppConfig.requestQueue.add(myRequestLogicalTimestamp);

        try {
            AppConfig.replyLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        while (!AppConfig.requestQueue.peek().equals(myRequestLogicalTimestamp)) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        LeaveMessage leaveMessage = new LeaveMessage(AppConfig.myServentInfo.getListenerPort(),
                AppConfig.chordState.getNextNodePort(), Integer.toString(AppConfig.myServentInfo.getUuid()));
        MessageUtil.sendMessage(leaveMessage);

        parser.stop();
    }

    private void contactBootstrap() {
        int bsPort = AppConfig.BOOTSTRAP_PORT;
        String bsIp = AppConfig.BOOTSTRAP_IP;

        try {
            Socket bsSocket = new Socket(bsIp, bsPort);

            PrintWriter bsWriter = new PrintWriter(bsSocket.getOutputStream());
            bsWriter.write("Left\n" + AppConfig.myServentInfo.getListenerPort() + "\n");
            bsWriter.flush();

            bsSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
