package app;

import servent.message.JobMessage;
import servent.message.util.MessageUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class JobCommandHandler {

    static Map<Integer, String> fractalIds = new HashMap<>();
    static int overflowLevelNodes = 0;
    static int baseFractalLevel = 0;

    public static void start(Job job) {
        assignFractalsIds(job);

        ArrayList<Job> jobs = prepareJobs(job);

        int lastAssigned = 0;
        for (int i = 0; i < job.getN(); i++) {
            AppConfig.timestampedStandardPrint("Next node for key:" + lastAssigned + " is " + AppConfig.chordState.getNextNodeForKey(lastAssigned).getUuid());
            JobMessage jobMessage = new JobMessage(AppConfig.myServentInfo.getListenerPort(),
                    AppConfig.chordState.getNextNodeForKey(lastAssigned).getListenerPort(), Integer.toString(lastAssigned),
                    jobs.get(i), fractalIds, 1);
            MessageUtil.sendMessage(jobMessage);

            if (overflowLevelNodes > 0) {
                lastAssigned += (int)Math.pow(job.getN(), baseFractalLevel - 1);
                overflowLevelNodes -= job.getN();
            } else {
                lastAssigned += (int)Math.pow(job.getN(), baseFractalLevel - 2);
            }
        }
    }

    public static ArrayList<Job> prepareJobs(Job job) {
        ArrayList<Job> jobs = new ArrayList<>();
        for (int i = 0; i < job.getN(); i++) {
            ArrayList<Point> newPoints = new ArrayList<>();
            Point anchor = job.getPoints().get(i);
            newPoints.add(anchor);
            for (Point point : job.getPoints()) {
                if (!point.equals(anchor)) {
                    newPoints.add(Point.pointOnP(anchor, point, job.getP()));
                }
            }

            Job newJob = new Job(job.getName(), job.getN(), job.getP(), job.getWidth(), job.getHeight(), newPoints);
            jobs.add(newJob);
        }
        return jobs;
    }

    private static void assignFractalsIds(Job job) {
        // TODO Special case when only one node needed
        // TODO Multiple jobs
        int nodeCount = AppConfig.chordState.getNodeCount();
//        int nodeCount = 10;
        int dots = job.getN();

        int needed = 1;
        int increment = dots - 1;
        for (; needed + increment <= nodeCount; needed += increment);

        baseFractalLevel = 1;
        while (dots <= needed) {
            baseFractalLevel++;
            dots *= dots;
        }

        overflowLevelNodes = (needed - ((int)Math.pow(job.getN(), baseFractalLevel - 1)));
        overflowLevelNodes += (overflowLevelNodes / increment);
        fractalIds = new HashMap<>();

        int copyOfOverflowLevelNodes = overflowLevelNodes;

        String prefix = job.getName() + "0";
        int lastAssigned = 0;
        for (int i = 0; i < job.getN(); i++) {
            if (copyOfOverflowLevelNodes > 0) {
                assign(prefix + i, lastAssigned, lastAssigned + (int)Math.pow(job.getN(), baseFractalLevel - 1) - 1, job.getN());
                lastAssigned += (int)Math.pow(job.getN(), baseFractalLevel - 1);
                copyOfOverflowLevelNodes -= job.getN();
            } else {
                assign(prefix + i, lastAssigned, lastAssigned + (int)Math.pow(job.getN(), baseFractalLevel - 2) - 1, job.getN());
                lastAssigned += (int)Math.pow(job.getN(), baseFractalLevel - 2);
            }
        }
    }


    private static void assign(String prefix, int left, int right, int dots) {
        if (left == right) {
            fractalIds.put(left, prefix);
        } else {
            int incrementLevel = (int)Math.pow(dots, (int)((right - left + 1) / dots - 1));
            int suffix = 0;
            for (int i = left; i <= right - incrementLevel + 1; i += incrementLevel) {
                assign(prefix + suffix, i, i + incrementLevel - 1, dots);
                suffix++;
            }
        }
    }

//    public static void main(String[] args) {
//        Job job = new Job("x", 3, 0.5, 800, 800, new ArrayList<>());
//        start(job);
//        System.out.println(fractalIds.toString());
//    }

}
