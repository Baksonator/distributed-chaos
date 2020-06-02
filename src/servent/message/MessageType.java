package servent.message;

public enum MessageType {
	NEW_NODE, WELCOME, SORRY, UPDATE, PUT, ASK_GET, TELL_GET, POISON, LEAVE, JOB, RESULT_REQUEST, RESULT_COLLECTION,
	RESULT_REPLY, JOB_MIGRATION, STATUS_REQUEST, STATUS_COLLECTION, STATUS_REPLY, JOB_STOP, MUTEX_REQUEST, MUTEX_REPLY,
	MUTEX_RELEASE, RELEASE_ENTRY, JOB_MESSAGE_RESPONSE
}
