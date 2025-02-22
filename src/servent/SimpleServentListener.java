package servent;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import app.AppConfig;
import app.Cancellable;
import cli.CLIParser;
import servent.handler.*;
import servent.message.Message;
import servent.message.util.MessageUtil;

public class SimpleServentListener implements Runnable, Cancellable {

	private volatile boolean working = true;

	private CLIParser cliParser;
	
	public SimpleServentListener() {
		
	}

	public void setCliParser(CLIParser cliParser) {
		this.cliParser = cliParser;
	}

	/*
	 * Thread pool for executing the handlers. Each client will get it's own handler thread.
	 */
	private final ExecutorService threadPool = Executors.newWorkStealingPool();
	
	@Override
	public void run() {
		ServerSocket listenerSocket = null;
		try {
			listenerSocket = new ServerSocket(AppConfig.myServentInfo.getListenerPort(), 100);
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
				
				MessageHandler messageHandler = new NullHandler(clientMessage);
				
				/*
				 * Each message type has it's own handler.
				 * If we can get away with stateless handlers, we will,
				 * because that way is much simpler and less error prone.
				 */
				switch (clientMessage.getMessageType()) {
				case NEW_NODE:
					messageHandler = new NewNodeHandler(clientMessage);
					break;
				case WELCOME:
					messageHandler = new WelcomeHandler(clientMessage);
					break;
				case SORRY:
					messageHandler = new SorryHandler(clientMessage);
					break;
				case UPDATE:
					messageHandler = new UpdateHandler(clientMessage);
					break;
				case PUT:
					messageHandler = new PutHandler(clientMessage);
					break;
				case ASK_GET:
					messageHandler = new AskGetHandler(clientMessage);
					break;
				case TELL_GET:
					messageHandler = new TellGetHandler(clientMessage);
					break;
				case LEAVE:
					messageHandler = new LeaveHandler(clientMessage, cliParser, this);
					break;
				case JOB:
					messageHandler = new JobHandler(clientMessage);
					break;
				case RESULT_REQUEST:
					messageHandler = new ResultRequestHandler(clientMessage);
					break;
				case RESULT_COLLECTION:
					messageHandler = new ResultCollectionHandler(clientMessage);
					break;
				case RESULT_REPLY:
					messageHandler = new ResultReplyHandler(clientMessage);
					break;
				case JOB_MIGRATION:
					messageHandler = new JobMigrationHandler(clientMessage);
					break;
				case STATUS_REQUEST:
					messageHandler = new StatusRequestHandler(clientMessage);
					break;
				case STATUS_COLLECTION:
					messageHandler = new StatusCollectionHandler(clientMessage);
					break;
				case STATUS_REPLY:
					messageHandler = new StatusReplyHandler(clientMessage);
					break;
				case JOB_STOP:
					messageHandler = new JobStopHandler(clientMessage);
					break;
				case RELEASE_ENTRY:
					messageHandler = new ReleaseEntryHandler();
					break;
				case JOB_MESSAGE_RESPONSE:
					messageHandler = new JobMessageResponseHandler(clientMessage);
					break;
				case PING:
					messageHandler = new PingHandler(clientMessage);
					break;
				case PONG:
					messageHandler = new PongHandler(clientMessage);
					break;
				case SUSPICION_REQUEST:
					messageHandler = new SuspicionRequestHandler(clientMessage);
					break;
				case SUSPICION_REPLY:
					messageHandler = new SuspicionReplyHandler(clientMessage);
					break;
				case BACKUP:
					messageHandler = new BackupHandler(clientMessage);
					break;
				case DIED:
					messageHandler = new DiedHandler(clientMessage);
					break;
				case DIED_REPLY:
					messageHandler = new DiedReplyHandler(clientMessage);
					break;
				case BACKUP_REQUEST:
					messageHandler = new BackupRequestHandler(clientMessage);
					break;
				case BACKUP_REPLY:
					messageHandler = new BackupReplyHandler(clientMessage);
					break;
				case POISON:
					break;
				}
				
				threadPool.submit(messageHandler);
			} catch (SocketTimeoutException timeoutEx) {
				//Uncomment the next line to see that we are waking up every second.
//				AppConfig.timedStandardPrint("Waiting...");
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
