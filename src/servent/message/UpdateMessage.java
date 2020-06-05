package servent.message;

import app.Job;

import java.util.List;
import java.util.Map;

public class UpdateMessage extends BasicMessage {

	private static final long serialVersionUID = 3586102505319194978L;

	private final Map<Integer, String> fractalIds;
	private final List<Job> activeJobs;
	private final int newId;
	private final List<String> ips;
	private final List<Integer> ids;

	public UpdateMessage(int senderPort, int receiverPort, String text, Map<Integer, String> fractalIds,
						 List<Job> activeJobs, int newId, List<String> ips, List<Integer> ids) {
		super(MessageType.UPDATE, senderPort, receiverPort, text);

		this.fractalIds = fractalIds;
		this.activeJobs = activeJobs;
		this.newId = newId;
		this.ips = ips;
		this.ids = ids;
	}

	public Map<Integer, String> getFractalIds() {
		return fractalIds;
	}

	public List<Job> getActiveJobs() {
		return activeJobs;
	}

	public int getNewId() {
		return newId;
	}

	public List<String> getIps() {
		return ips;
	}

	public List<Integer> getIds() {
		return ids;
	}
}
