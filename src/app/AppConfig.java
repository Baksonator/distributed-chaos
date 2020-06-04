package app;

import mutex.LamportClock;
import mutex.LogicalTimestamp;
import servent.FIFOListener;
import servent.message.util.FifoSendWorker;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This class contains all the global application configuration stuff.
 * @author bmilojkovic
 *
 */
public class AppConfig {

	/**
	 * Convenience access for this servent's information
	 */
	public static ServentInfo myServentInfo;
	public static ArrayList<Job> jobs = new ArrayList<>();
	public static List<Job> activeJobs = new CopyOnWriteArrayList<>();
	
	/**
	 * Print a message to stdout with a timestamp
	 * @param message message to print
	 */
	public static void timestampedStandardPrint(String message) {
		DateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
		Date now = new Date();
		
		System.out.println(timeFormat.format(now) + " - " + message);
	}
	
	/**
	 * Print a message to stderr with a timestamp
	 * @param message message to print
	 */
	public static void timestampedErrorPrint(String message) {
		DateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
		Date now = new Date();
		
		System.err.println(timeFormat.format(now) + " - " + message);
	}
	
	public static boolean INITIALIZED = false;
	public static int BOOTSTRAP_PORT;
	public static String BOOTSTRAP_IP;
	public static int SERVENT_COUNT;
	public static int SOFT_FAILURE_TIME;
	public static int HARD_FAILURE_TIME;
	
	public static ChordState chordState;

	public static JobWorker jobWorker;

	public static String pendingResultJobName = "";

	public static Job myMainJob;

	public static BlockingQueue<List<Point>> incomingData = new LinkedBlockingQueue<>();

	public static BlockingQueue<StatusResult> statusResults = new LinkedBlockingQueue<>();

	public static volatile boolean  isSingleId = false;

	public static List<FifoSendWorker> fifoSendWorkers = new CopyOnWriteArrayList<>();

	public static LamportClock lamportClock;

	public static FIFOListener fifoListener;

	public static BlockingQueue<LogicalTimestamp> requestQueue = new PriorityBlockingQueue<>();

	public static CountDownLatch replyLatch;

	public static CountDownLatch jobLatch;

	public static final Object localLock = new Object();

	public static final Semaphore localSemaphore = new Semaphore(1, true);

	public static volatile boolean isDesignated = false;

	public static volatile int currId = 1;

	public static final Object pauseLock = new Object();
	public static AtomicBoolean paused = new AtomicBoolean(false);

	public static List<Point> backupSuccessor = new ArrayList<>();
	public static List<Point> backupPredecessor = new ArrayList<>();

	public static Pinger pinger;
	public static FailureDetector failureDetector;
	public static BackupWorker backupWorker;

	public static CountDownLatch diedLatch;

	public static AtomicInteger alsoDied = new AtomicInteger(-1);

	public static AtomicInteger myDied = new AtomicInteger(-1);

	public static AtomicInteger receivedBackups = new AtomicInteger(-1);

	public static BlockingQueue<List<Point>> backupsReceived = new LinkedBlockingQueue<>();
	public static BlockingQueue<Integer> backupsReceivedIds = new LinkedBlockingQueue<>();

	/**
	 * Reads a config file. Should be called once at start of app.
	 * The config file should be of the following format:
	 * <br/>
	 * <code><br/>
	 * servent_count=3 			- number of servents in the system <br/>
	 * chord_size=64			- maximum value for Chord keys <br/>
	 * bs.port=2000				- bootstrap server listener port <br/>
	 * servent0.port=1100 		- listener ports for each servent <br/>
	 * servent1.port=1200 <br/>
	 * servent2.port=1300 <br/>
	 * 
	 * </code>
	 * <br/>
	 * So in this case, we would have three servents, listening on ports:
	 * 1100, 1200, and 1300. A bootstrap server listening on port 2000, and Chord system with
	 * max 64 keys and 64 nodes.<br/>
	 * 
	 * @param configName name of configuration file
	 * @param serventId id of the servent, as used in the configuration file
	 */
	public static void readConfig(String configName, int serventId){
		Properties properties = new Properties();
		try {
			properties.load(new FileInputStream(new File(configName)));
			
		} catch (IOException e) {
			timestampedErrorPrint("Couldn't open properties file. Exiting...");
			System.exit(0);
		}
		
		try {
			BOOTSTRAP_PORT = Integer.parseInt(properties.getProperty("bs.port"));
		} catch (NumberFormatException e) {
			timestampedErrorPrint("Problem reading bootstrap_port. Exiting...");
			System.exit(0);
		}

		BOOTSTRAP_IP = properties.getProperty("bs.ip_address");
		
		try {
			SERVENT_COUNT = Integer.parseInt(properties.getProperty("servent_count"));
		} catch (NumberFormatException e) {
			timestampedErrorPrint("Problem reading servent_count. Exiting...");
			System.exit(0);
		}

		try {
			SOFT_FAILURE_TIME = Integer.parseInt(properties.getProperty("soft_failure_time"));
		} catch (NumberFormatException e) {
			timestampedErrorPrint("Problem reading soft_failure_time. Exiting...");
			System.exit(0);
		}

		try {
			HARD_FAILURE_TIME = Integer.parseInt(properties.getProperty("hard_failure_time"));
		} catch (NumberFormatException e) {
			timestampedErrorPrint("Problem reading hard_failure_time. Exiting...");
			System.exit(0);
		}

		int jobCount = -1;

		try {
			jobCount = Integer.parseInt(properties.getProperty("job_count"));
		} catch (NumberFormatException e) {
			timestampedErrorPrint("Problem reading job_count. Exiting...");
			System.exit(0);
		}

		for (int i = 1; i <= jobCount; i++) {
			try {
				String jobName = properties.getProperty("job" + i + ".name");
				int n = Integer.parseInt(properties.getProperty("job" + i + ".n"));
				double p = Double.parseDouble(properties.getProperty("job" + i + ".p"));
				int w = Integer.parseInt(properties.getProperty("job" + i + ".w"));
				int h = Integer.parseInt(properties.getProperty("job" + i + ".h"));
				ArrayList<Point> points = new ArrayList<>();
				String pointsString = properties.getProperty("job" + i + ".a");
				String[] splitPoints = pointsString.split(",");
				for (int j = 0; j < splitPoints.length; j += 2) {
					points.add(new Point(Integer.parseInt(splitPoints[j]), Integer.parseInt(splitPoints[j + 1])));
				}
				jobs.add(new Job(jobName, n, p, w, h, points));
			} catch (NumberFormatException e) {
				timestampedErrorPrint("Problem reading job" + i + ". Exiting...");
				System.exit(0);
			}
		}
		
		try {
			int chordSize = Integer.parseInt(properties.getProperty("chord_size"));
			
			ChordState.CHORD_SIZE = chordSize;
			chordState = new ChordState();
			
		} catch (NumberFormatException e) {
			timestampedErrorPrint("Problem reading chord_size. Must be a number that is a power of 2. Exiting...");
			System.exit(0);
		}
		
		String portProperty = "servent"+serventId+".port";
		
		int serventPort = -1;
		
		try {
			serventPort = Integer.parseInt(properties.getProperty(portProperty));
		} catch (NumberFormatException e) {
			timestampedErrorPrint("Problem reading " + portProperty + ". Exiting...");
			System.exit(0);
		}
		
		myServentInfo = new ServentInfo("localhost", serventPort);
	}
	
}
