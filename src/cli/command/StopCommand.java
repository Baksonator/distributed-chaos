package cli.command;

import app.AppConfig;
import cli.CLIParser;
import servent.SimpleServentListener;
import servent.message.util.FifoSendWorker;
import servent.message.util.MessageUtil;

public class StopCommand implements CLICommand {

	private final CLIParser parser;
	private final SimpleServentListener listener;
	
	public StopCommand(CLIParser parser, SimpleServentListener listener) {
		this.parser = parser;
		this.listener = listener;
	}
	
	@Override
	public String commandName() {
		return "stopForce";
	}

	@Override
	public void execute(String args) {
		AppConfig.timestampedStandardPrint("Stopping...");
		if (AppConfig.jobWorker != null) {
			AppConfig.jobWorker.stop();
		}
		for (FifoSendWorker senderWorker : AppConfig.fifoSendWorkers) {
			senderWorker.stop();
		}
		parser.stop();
		AppConfig.fifoListener.stop();
		listener.stop();
		AppConfig.backupWorker.stop();
		AppConfig.pinger.stop();
		AppConfig.failureDetector.stop();
	}

}
