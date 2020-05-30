package app;

import servent.message.JobMessage;
import servent.message.util.MessageUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class JobCommandHandler {

    // TODO Sestica ne salje nista kod migracije a ni trojka, vidi ovde je greska prilikom slanje jobMessage-a
    // TODO Mada sestica ne dobija uopste job poruku, a trojka je dobije ali ne zna sta ce sa njom, ovde si sjebao
    public static Map<Integer, String> fractalIds = new HashMap<>();
    static int overflowLevelNodes = 0;
    static int baseFractalLevel = 0;
    static ArrayList<Integer> overflowLevelNodesByJob = new ArrayList<>();
    static ArrayList<Integer> baseFractalLevelsByJob = new ArrayList<>();

    static ArrayList<Job> helperActiveJobs = new ArrayList<>();

    public static void start(Job job) {
//        if (!helperActiveJobs.isEmpty()) {
        if (!AppConfig.activeJobs.isEmpty()) {
            Map<Integer, String> oldFractalIds = fractalIds;

            ArrayList<Job> newJobs = new ArrayList<>(AppConfig.activeJobs);
//            ArrayList<Job> newJobs = new ArrayList<>(helperActiveJobs);
            newJobs.add(job);
            Collections.sort(newJobs);

            int jobCount = newJobs.size();
            int nodeCount = AppConfig.chordState.getNodeCount();
//            int nodeCount = 9;
            int nodesByJob = nodeCount / jobCount;
            int extraNodes = nodeCount % jobCount;

            fractalIds = new HashMap<>();
            overflowLevelNodesByJob = new ArrayList<>();
            baseFractalLevelsByJob = new ArrayList<>();
            int nextToAssign = 0;
            for (Job newJob : newJobs) {
                if (extraNodes > 0) {
                    assignFractalsIds(newJob, nodesByJob + 1, nextToAssign);
                    nextToAssign += (nodesByJob + 1);
                    extraNodes--;
                } else {
                    assignFractalsIds(newJob, nodesByJob, nextToAssign);
                    nextToAssign += nodesByJob;
                }
            }

            AppConfig.timestampedStandardPrint(fractalIds.toString());

//            assignFractalsIds(job);

            ArrayList<ArrayList<Job>> jobs = new ArrayList<>();
            for (Job newJob : newJobs) {
                jobs.add(prepareJobs(newJob));
            }

            AppConfig.timestampedStandardPrint(jobs.toString());

            int jobNameLen = job.getName().length();

//            ArrayList<Job> jobs = prepareJobs(job);
            Map<String, String> fractalIdMapping = new HashMap<>();
            for (String fractalIdName : fractalIds.values()) {
                if (fractalIdName.substring(0, jobNameLen).equals(job.getName())) {
                    fractalIdMapping.put(fractalIdName, "");
                }
            }

            for (String newFractalIdName : fractalIds.values()) {
                for (String oldFractalIdName : oldFractalIds.values()) {
                    int newNameLen = newFractalIdName.length();
                    if (oldFractalIdName.substring(0, Math.min(oldFractalIdName.length(), newNameLen)).equals(newFractalIdName)) {
                        fractalIdMapping.put(oldFractalIdName, newFractalIdName);
                    }
                }
            }

            AppConfig.timestampedStandardPrint(fractalIdMapping.toString());

//            System.out.println(oldFractalIds.toString());
//            System.out.println(fractalIds.toString());
//            System.out.println(fractalIdMapping.toString());

            int lastAssigned = 0;
            int j = 0;
            for (Job newJob : newJobs) {
                AppConfig.timestampedStandardPrint(newJob.getName());
                for (int i = 0; i < newJob.getN(); i++) {
                    AppConfig.timestampedStandardPrint("Next node for key:" + lastAssigned + " is " + AppConfig.chordState.getNextNodeForKey(lastAssigned).getUuid());
                    JobMessage jobMessage = new JobMessage(AppConfig.myServentInfo.getListenerPort(),
                            AppConfig.chordState.getNextNodeForKey(lastAssigned).getListenerPort(), Integer.toString(lastAssigned),
                            jobs.get(j).get(i), fractalIds, 1, job, fractalIdMapping);
                    MessageUtil.sendMessage(jobMessage);

                    int currOverflowLevelNodes = overflowLevelNodesByJob.get(j);
                    int currBaseFractalLevel = baseFractalLevelsByJob.get(j);
                    if (currOverflowLevelNodes > 0) {
                        lastAssigned += (int) Math.pow(newJob.getN(), currBaseFractalLevel - 1);
                        currOverflowLevelNodes -= newJob.getN();
                    } else {
                        lastAssigned += (int) Math.pow(newJob.getN(), currBaseFractalLevel - 2);
                    }
                }
                j++;
            }

        } else {
            assignFractalsIds(job, AppConfig.chordState.getNodeCount(), 0);
//            assignFractalsIds(job, 9, 0);

            AppConfig.timestampedStandardPrint(fractalIds.toString());
//            helperActiveJobs.add(job);
            ArrayList<Job> jobs = prepareJobs(job);

            AppConfig.timestampedStandardPrint(jobs.toString());
//            System.out.println(fractalIds.toString());

            int lastAssigned = 0;
            for (int i = 0; i < job.getN(); i++) {
                AppConfig.timestampedStandardPrint("Next node for key:" + lastAssigned + " is " + AppConfig.chordState.getNextNodeForKey(lastAssigned).getUuid());
                JobMessage jobMessage = new JobMessage(AppConfig.myServentInfo.getListenerPort(),
                        AppConfig.chordState.getNextNodeForKey(lastAssigned).getListenerPort(), Integer.toString(lastAssigned),
                        jobs.get(i), fractalIds, 1, job, null);
                MessageUtil.sendMessage(jobMessage);

                if (overflowLevelNodes > 0) {
                    lastAssigned += (int) Math.pow(job.getN(), baseFractalLevel - 1);
                    overflowLevelNodes -= job.getN();
                } else {
                    lastAssigned += (int) Math.pow(job.getN(), baseFractalLevel - 2);
                }
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

    private static void assignFractalsIds(Job job, int nodeCount, int lastAssigned) {
        // TODO Special case when only one node needed
//        int nodeCount = AppConfig.chordState.getNodeCount();
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
        baseFractalLevelsByJob.add(baseFractalLevel);

        overflowLevelNodes = (needed - ((int)Math.pow(job.getN(), baseFractalLevel - 1)));
        overflowLevelNodes += (overflowLevelNodes / increment);
        overflowLevelNodesByJob.add(overflowLevelNodes);
//        fractalIds = new HashMap<>();

        int copyOfOverflowLevelNodes = overflowLevelNodes;

        String prefix = job.getName() + "0";
//        int lastAssigned = 0;
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
//        Job job2 = new Job("y", 3, 0.5, 800, 800, new ArrayList<>());
//        start(job2);
////        System.out.println(fractalIds.toString());
//    }

}
