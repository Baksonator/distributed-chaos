package servent.message;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import app.ChordState;

/**
 * A default message implementation. This should cover most situations.
 * If you want to add stuff, remember to think about the modificator methods.
 * If you don't override the modificators, you might drop stuff.
 * @author bmilojkovic
 *
 */
public class BasicMessage implements Message {

	private static final long serialVersionUID = -9075856313609777945L;
	private final MessageType type;
	private final int senderPort;
	private final int receiverPort;
	private final String messageText;
	private boolean isFifo;
	private String senderIp;
	private String receiverIp;
	
	//This gives us a unique id - incremented in every natural constructor.
	private static AtomicInteger messageCounter = new AtomicInteger(0);
	private final int messageId;
	
	public BasicMessage(MessageType type, int senderPort, int receiverPort) {
		this.type = type;
		this.senderPort = senderPort;
		this.receiverPort = receiverPort;
		this.messageText = "";
		this.isFifo = false;
		this.senderIp = "";
		this.receiverIp = "";
		
		this.messageId = messageCounter.getAndIncrement();
	}
	
	public BasicMessage(MessageType type, int senderPort, int receiverPort, String messageText) {
		this.type = type;
		this.senderPort = senderPort;
		this.receiverPort = receiverPort;
		this.messageText = messageText;
		this.isFifo = false;
		this.senderIp = "";
		this.receiverIp = "";
		
		this.messageId = messageCounter.getAndIncrement();
	}
	
	@Override
	public MessageType getMessageType() {
		return type;
	}
	
	@Override
	public int getReceiverPort() {
		return receiverPort;
	}
	
	@Override
	public String getReceiverIpAddress() {
		return receiverIp;
	}
	
	@Override
	public int getSenderPort() {
		return senderPort;
	}

	@Override
	public String getMessageText() {
		return messageText;
	}

	@Override
	public boolean isFifo() {
		return isFifo;
	}

	@Override
	public void setFifo(boolean fifo) {
		isFifo = fifo;
	}

	@Override
	public int getMessageId() {
		return messageId;
	}

	@Override
	public String getSenderIp() {
		return senderIp;
	}

	@Override
	public void setSenderIp(String senderIp) {
		this.senderIp = senderIp;
	}

	@Override
	public String getReceiverIp() {
		return receiverIp;
	}

	@Override
	public void setReceiverIp(String receiverIp) {
		this.receiverIp = receiverIp;
	}

	/**
	 * Comparing messages is based on their unique id and the original sender port.
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof BasicMessage) {
			BasicMessage other = (BasicMessage)obj;
			
			if (getMessageId() == other.getMessageId() &&
				getSenderPort() == other.getSenderPort()) {
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Hash needs to mirror equals, especially if we are gonna keep this object
	 * in a set or a map. So, this is based on message id and original sender id also.
	 */
	@Override
	public int hashCode() {
		return Objects.hash(getMessageId(), getSenderPort());
	}
	
	/**
	 * Returns the message in the format: <code>[sender_id|sender_port|message_id|text|type|receiver_port|receiver_id]</code>
	 */
	@Override
	public String toString() {
		return "[" + ChordState.chordHash(getSenderPort()) + "|" + getSenderPort() + "|" + getMessageId() + "|" +
					getMessageText() + "|" + getMessageType() + "|" +
					getReceiverPort() + "|" + ChordState.chordHash(getReceiverPort()) + "]";
	}

}
