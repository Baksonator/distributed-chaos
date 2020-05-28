package cli.command;

import app.AppConfig;
import cli.CLIParser;
import servent.SimpleServentListener;
import servent.message.LeaveMessage;
import servent.message.util.MessageUtil;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.*;

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

        LeaveMessage leaveMessage = new LeaveMessage(AppConfig.myServentInfo.getListenerPort(),
                AppConfig.chordState.getNextNodePort(), Integer.toString(AppConfig.myServentInfo.getUuid()));
        MessageUtil.sendMessage(leaveMessage);

        AppConfig.timestampedStandardPrint("Stopping...");
        parser.stop();
        listener.stop();
    }

    private void contactBootstrap() {
        int bsPort = AppConfig.BOOTSTRAP_PORT;
        String bsIp = AppConfig.BOOTSTRAP_IP;

        String ip = null;

        try(final Socket socket = new Socket()){
            socket.connect(new InetSocketAddress("google.com", 80));
            ip = socket.getLocalAddress().getHostAddress();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            Socket bsSocket = new Socket(bsIp, bsPort);

            PrintWriter bsWriter = new PrintWriter(bsSocket.getOutputStream());
            bsWriter.write("Left\n" + ip + "\n" + AppConfig.myServentInfo.getListenerPort() + "\n");
            bsWriter.flush();

            bsSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
