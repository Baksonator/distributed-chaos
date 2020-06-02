package servent.message.util;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

import app.AppConfig;
import app.ServentInfo;
import servent.message.Message;

/**
 * For now, just the read and send implementation, based on Java serializing.
 * Not too smart. Doesn't even check the neighbor list, so it actually allows cheating.
 * 
 * Depending on the configuration it delegates sending either to a {@link DelayedMessageSender}
 * in a new thread (non-FIFO) or stores the message in a queue for the {@link FifoSendWorker} (FIFO).
 * 
 * When reading, if we are FIFO, we send an ACK message on the same socket, so the other side
 * knows they can send the next message.
 * @author bmilojkovic
 *
 */
public class MessageUtil {

	/**
	 * Normally this should be true, because it helps with debugging.
	 * Flip this to false to disable printing every message send / receive.
	 */
	public static final boolean MESSAGE_UTIL_PRINTING = true;

	public static Map<Integer, BlockingQueue<Message>> pendingMessages = new ConcurrentHashMap<>();
	
	public static Message readMessage(Socket socket) {
		
		Message clientMessage = null;
			
		try {
			ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
	
			clientMessage = (Message) ois.readObject();

			if (clientMessage.isFifo()) {
				String response = "ACK";
				ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
				oos.writeObject(response);
				oos.flush();
			}
			
			socket.close();
		} catch (IOException e) {
			AppConfig.timestampedErrorPrint("Error in reading socket on " +
					socket.getInetAddress() + ":" + socket.getPort());
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		
		if (MESSAGE_UTIL_PRINTING) {
			AppConfig.timestampedStandardPrint("Got message " + clientMessage);
		}
				
		return clientMessage;
	}
	
	public static void sendMessage(Message message) {
		if (message.isFifo()) {
			for (ServentInfo serventInfo : AppConfig.chordState.getAllNodeInfo()) {
				// TODO Add IP here as well
				if (serventInfo.getListenerPort() == message.getReceiverPort()) {
					try {
						pendingMessages.get(serventInfo.getUuid()).put(message);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					break;
				}
			}
		} else {
			Thread delayedSender = new Thread(new DelayedMessageSender(message));

			delayedSender.start();
		}
	}
}
