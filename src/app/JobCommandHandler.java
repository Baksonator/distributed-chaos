package app;

import servent.message.*;
import servent.message.util.FifoSendWorker;
import servent.message.util.MessageUtil;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class JobCommandHandler {

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

            for (int i = 0; i < AppConfig.chordState.getNodeCount(); i++) {
                int key = AppConfig.chordState.getAllNodeInfoHelper().get(i).getUuid();
                if (!fractalIds.containsKey(key)) {
                    fractalIds.put(key, "");
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
//            for (String fractalIdName : fractalIds.values()) {
//                if (fractalIdName.equals("")) {
//                    continue;
//                }
//                String onlyFractalIdName = fractalIdName.substring(0, fractalIdName.indexOf("0"));
//                if (onlyFractalIdName.equals(job.getName())) {
//                    fractalIdMapping.put(fractalIdName, "");
//                }
//            }

            for (String newFractalIdName : fractalIds.values()) {
                for (String oldFractalIdName : oldFractalIds.values()) {
                    if (oldFractalIdName.equals("") || newFractalIdName.equals("")) {
                        continue;
                    }
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
            int next = 0;
            extraNodes = nodeCount % jobCount;
            int j = 0;
            for (Job newJob : newJobs) {
                AppConfig.timestampedStandardPrint(newJob.getName());
                lastAssigned = next;
//                lastAssigned = AppConfig.chordState.getAllNodeInfoHelper().get(next).getUuid();
                int tempNodesByJob = nodesByJob;
                if (extraNodes > 0) {
                    tempNodesByJob++;
                }
                if (tempNodesByJob < newJob.getN()) {
                    int receiverId = AppConfig.chordState.getAllNodeInfoHelper().get(lastAssigned).getUuid();
                    AppConfig.timestampedStandardPrint("Next node for key:" + receiverId + " is " + AppConfig.chordState.getNextNodeForKey(receiverId).getUuid());
                    JobMessage jobMessage = new JobMessage(AppConfig.myServentInfo.getListenerPort(),
                            AppConfig.chordState.getNextNodeForKey(receiverId).getListenerPort(), Integer.toString(receiverId),
                            newJob, fractalIds, 0, job, fractalIdMapping, AppConfig.myServentInfo.getUuid());
                    MessageUtil.sendMessage(jobMessage);
                } else {
                    int currOverflowLevelNodes = overflowLevelNodesByJob.get(j);
                    int currBaseFractalLevel = baseFractalLevelsByJob.get(j);
                    for (int i = 0; i < newJob.getN(); i++) {
                        int receiverId = AppConfig.chordState.getAllNodeInfoHelper().get(lastAssigned).getUuid();
                        AppConfig.timestampedStandardPrint("Next node for key:" + receiverId + " is " + AppConfig.chordState.getNextNodeForKey(receiverId).getUuid());
                        JobMessage jobMessage = new JobMessage(AppConfig.myServentInfo.getListenerPort(),
                                AppConfig.chordState.getNextNodeForKey(receiverId).getListenerPort(), Integer.toString(receiverId),
                                jobs.get(j).get(i), fractalIds, 1, job, fractalIdMapping, AppConfig.myServentInfo.getUuid());
                        MessageUtil.sendMessage(jobMessage);

                        if (currOverflowLevelNodes > 0) {
                            lastAssigned += (int) Math.pow(newJob.getN(), currBaseFractalLevel - 1);
                            currOverflowLevelNodes -= newJob.getN();
                        } else {
                            lastAssigned += (int) Math.pow(newJob.getN(), currBaseFractalLevel - 2);
                        }
                    }
                }
                j++;
                next += nodesByJob;
                if (extraNodes > 0) {
                    next++;
                    extraNodes--;
                }
            }

            for (Map.Entry<Integer, String> entry : fractalIds.entrySet()) {
                if (entry.getValue().equals("")) {
                    JobMessage jobMessage = new JobMessage(AppConfig.myServentInfo.getListenerPort(),
                            AppConfig.chordState.getNextNodeForKey(entry.getKey()).getListenerPort(), Integer.toString(entry.getKey()),
                            null, fractalIds, 1, job, fractalIdMapping, AppConfig.myServentInfo.getUuid());
                    MessageUtil.sendMessage(jobMessage);
                }
            }

        } else {
            fractalIds.clear();

            assignFractalsIds(job, AppConfig.chordState.getNodeCount(), 0);
//            assignFractalsIds(job, 9, 0);
            for (int i = 0; i < AppConfig.chordState.getNodeCount(); i++) {
                int key = AppConfig.chordState.getAllNodeInfoHelper().get(i).getUuid();
                if (!fractalIds.containsKey(key)) {
                    fractalIds.put(key, "");
                }
            }

            AppConfig.timestampedStandardPrint(fractalIds.toString());
//            helperActiveJobs.add(job);
            ArrayList<Job> jobs = prepareJobs(job);

            AppConfig.timestampedStandardPrint(jobs.toString());
//            System.out.println(fractalIds.toString());

            if (AppConfig.chordState.getNodeCount() < job.getN()) {
                int receiverId = AppConfig.chordState.getAllNodeInfoHelper().get(0).getUuid();
                AppConfig.timestampedStandardPrint("Next node for key:" + receiverId + " is " + AppConfig.chordState.getNextNodeForKey(receiverId).getUuid());
                JobMessage jobMessage = new JobMessage(AppConfig.myServentInfo.getListenerPort(),
                        AppConfig.chordState.getNextNodeForKey(receiverId).getListenerPort(), Integer.toString(receiverId),
                        job, fractalIds, 0, job, null, AppConfig.myServentInfo.getUuid());
                MessageUtil.sendMessage(jobMessage);
            } else {
                int lastAssigned = 0;
//                int lastAssigned = AppConfig.chordState.getAllNodeInfoHelper().get(0).getUuid();
                for (int i = 0; i < job.getN(); i++) {
                    int receiverId = AppConfig.chordState.getAllNodeInfoHelper().get(lastAssigned).getUuid();
                    AppConfig.timestampedStandardPrint("Next node for key:" + receiverId + " is " + AppConfig.chordState.getNextNodeForKey(receiverId).getUuid());
                    JobMessage jobMessage = new JobMessage(AppConfig.myServentInfo.getListenerPort(),
                            AppConfig.chordState.getNextNodeForKey(receiverId).getListenerPort(), Integer.toString(receiverId),
                            jobs.get(i), fractalIds, 1, job, null, AppConfig.myServentInfo.getUuid());
                    MessageUtil.sendMessage(jobMessage);

                    if (overflowLevelNodes > 0) {
                        lastAssigned += (int) Math.pow(job.getN(), baseFractalLevel - 1);
                        overflowLevelNodes -= job.getN();
                    } else {
                        lastAssigned += (int) Math.pow(job.getN(), baseFractalLevel - 2);
                    }
                }
            }

            for (Map.Entry<Integer, String> entry : fractalIds.entrySet()) {
                if (entry.getValue().equals("")) {
                    JobMessage jobMessage = new JobMessage(AppConfig.myServentInfo.getListenerPort(),
                            AppConfig.chordState.getNextNodeForKey(entry.getKey()).getListenerPort(), Integer.toString(entry.getKey()),
                            null, fractalIds, 1, job, null, AppConfig.myServentInfo.getUuid());
                    MessageUtil.sendMessage(jobMessage);
                }
            }

        }
    }

    public static void stop(Job job) {
//        if (!helperActiveJobs.isEmpty()) {
        if (AppConfig.activeJobs.size() > 1) {
            Map<Integer, String> oldFractalIds = fractalIds;

            ArrayList<Job> newJobs = new ArrayList<>(AppConfig.activeJobs);
//            ArrayList<Job> newJobs = new ArrayList<>(helperActiveJobs);
            newJobs.remove(job);
            Collections.sort(newJobs);

            int jobCount = newJobs.size();
            int nodeCount = AppConfig.chordState.getNodeCount();
//            int nodeCount = 7;
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

            for (int i = 0; i < AppConfig.chordState.getNodeCount(); i++) {
                int key = AppConfig.chordState.getAllNodeInfoHelper().get(i).getUuid();
                if (!fractalIds.containsKey(key)) {
                    fractalIds.put(key, "");
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
//            for (String fractalIdName : fractalIds.values()) {
//                if (fractalIdName.equals("")) {
//                    continue;
//                }
//                String onlyFractalIdName = fractalIdName.substring(0, fractalIdName.indexOf("0"));
//                if (onlyFractalIdName.equals(job.getName())) {
//                    fractalIdMapping.put(fractalIdName, "");
//                }
//            }
            for (String oldFractalIdName : oldFractalIds.values()) {
                for (String newFractalIdName : fractalIds.values()) {
                    if (oldFractalIdName.equals("") || newFractalIdName.equals("")) {
                        continue;
                    }
                    int oldNameLen = oldFractalIdName.length();
                    if (newFractalIdName.substring(0, Math.min(newFractalIdName.length(), oldNameLen)).equals(oldFractalIdName)) {
                        fractalIdMapping.put(newFractalIdName, oldFractalIdName);
                    }
                }
            }

            AppConfig.timestampedStandardPrint(fractalIdMapping.toString());

//            System.out.println(oldFractalIds.toString());
//            System.out.println(fractalIds.toString());
//            System.out.println(fractalIdMapping.toString());

            int lastAssigned = 0;
            int next = 0;
            extraNodes = nodeCount % jobCount;
            int j = 0;
            for (Job newJob : newJobs) {
                AppConfig.timestampedStandardPrint(newJob.getName());
                lastAssigned = next;
//                lastAssigned = AppConfig.chordState.getAllNodeInfoHelper().get(next).getUuid();
                int tempNodesByJob = nodesByJob;
                if (extraNodes > 0) {
                    tempNodesByJob++;
                }
                if (tempNodesByJob < newJob.getN()) {
                    int receiverId = AppConfig.chordState.getAllNodeInfoHelper().get(lastAssigned).getUuid();
                    AppConfig.timestampedStandardPrint("Next node for key:" + receiverId + " is " + AppConfig.chordState.getNextNodeForKey(receiverId).getUuid());
                    JobStopMessage jobMessage = new JobStopMessage(AppConfig.myServentInfo.getListenerPort(),
                            AppConfig.chordState.getNextNodeForKey(receiverId).getListenerPort(), Integer.toString(receiverId),
                            newJob, fractalIds, 0, job, fractalIdMapping, AppConfig.myServentInfo.getUuid());
                    MessageUtil.sendMessage(jobMessage);
                } else {
                    int currOverflowLevelNodes = overflowLevelNodesByJob.get(j);
                    int currBaseFractalLevel = baseFractalLevelsByJob.get(j);
                    for (int i = 0; i < newJob.getN(); i++) {
                        int receiverId = AppConfig.chordState.getAllNodeInfoHelper().get(lastAssigned).getUuid();
                        AppConfig.timestampedStandardPrint("Next node for key:" + receiverId + " is " + AppConfig.chordState.getNextNodeForKey(receiverId).getUuid());
                        JobStopMessage jobMessage = new JobStopMessage(AppConfig.myServentInfo.getListenerPort(),
                                AppConfig.chordState.getNextNodeForKey(receiverId).getListenerPort(), Integer.toString(receiverId),
                                jobs.get(j).get(i), fractalIds, 1, job, fractalIdMapping, AppConfig.myServentInfo.getUuid());
                        MessageUtil.sendMessage(jobMessage);

                        if (currOverflowLevelNodes > 0) {
                            lastAssigned += (int) Math.pow(newJob.getN(), currBaseFractalLevel - 1);
                            currOverflowLevelNodes -= newJob.getN();
                        } else {
                            lastAssigned += (int) Math.pow(newJob.getN(), currBaseFractalLevel - 2);
                        }
                    }
                }
                j++;
                next += nodesByJob;
                if (extraNodes > 0) {
                    next++;
                    extraNodes--;
                }
            }

            for (Map.Entry<Integer, String> entry : fractalIds.entrySet()) {
                if (entry.getValue().equals("")) {
                    JobStopMessage jobMessage = new JobStopMessage(AppConfig.myServentInfo.getListenerPort(),
                            AppConfig.chordState.getNextNodeForKey(entry.getKey()).getListenerPort(), Integer.toString(entry.getKey()),
                            null, fractalIds, 1, job, fractalIdMapping, AppConfig.myServentInfo.getUuid());
                    MessageUtil.sendMessage(jobMessage);
                }
            }

        } else {
//            assignFractalsIds(job, AppConfig.chordState.getNodeCount(), 0);
//            assignFractalsIds(job, 9, 0);
            for (int i = 0; i < AppConfig.chordState.getNodeCount(); i++) {
//                if (!fractalIds.containsKey(i)) {
                int key = AppConfig.chordState.getAllNodeInfoHelper().get(i).getUuid();
                fractalIds.put(key, "");
//                }
            }

            AppConfig.timestampedStandardPrint(fractalIds.toString());
//            helperActiveJobs.add(job);
//            ArrayList<Job> jobs = prepareJobs(job);

//            AppConfig.timestampedStandardPrint(jobs.toString());
//            System.out.println(fractalIds.toString());

//            if (AppConfig.chordState.getNodeCount() < job.getN()) {
//                AppConfig.timestampedStandardPrint("Next node for key:" + 0 + " is " + AppConfig.chordState.getNextNodeForKey(0).getUuid());
//                JobStopMessage jobMessage = new JobStopMessage(AppConfig.myServentInfo.getListenerPort(),
//                        AppConfig.chordState.getNextNodeForKey(0).getListenerPort(), Integer.toString(0),
//                        job, fractalIds, 0, job, null);
//                MessageUtil.sendMessage(jobMessage);
//            } else {
//                int lastAssigned = 0;
//                for (int i = 0; i < job.getN(); i++) {
//                    AppConfig.timestampedStandardPrint("Next node for key:" + lastAssigned + " is " + AppConfig.chordState.getNextNodeForKey(lastAssigned).getUuid());
//                    JobStopMessage jobMessage = new JobStopMessage(AppConfig.myServentInfo.getListenerPort(),
//                            AppConfig.chordState.getNextNodeForKey(lastAssigned).getListenerPort(), Integer.toString(lastAssigned),
//                            jobs.get(i), fractalIds, 1, job, null);
//                    MessageUtil.sendMessage(jobMessage);
//
//                    if (overflowLevelNodes > 0) {
//                        lastAssigned += (int) Math.pow(job.getN(), baseFractalLevel - 1);
//                        overflowLevelNodes -= job.getN();
//                    } else {
//                        lastAssigned += (int) Math.pow(job.getN(), baseFractalLevel - 2);
//                    }
//                }
//            }

            for (Map.Entry<Integer, String> entry : fractalIds.entrySet()) {
                if (entry.getValue().equals("")) {
                    JobStopMessage jobMessage = new JobStopMessage(AppConfig.myServentInfo.getListenerPort(),
                            AppConfig.chordState.getNextNodeForKey(entry.getKey()).getListenerPort(), Integer.toString(entry.getKey()),
                            null, fractalIds, 1, job, null, AppConfig.myServentInfo.getUuid());
                    MessageUtil.sendMessage(jobMessage);
                }
            }

        }
    }

    public static void restructureEntry() {
        Map<Integer, String> oldFractalIds = fractalIds;

        ArrayList<Job> newJobs = new ArrayList<>(AppConfig.activeJobs);
//            ArrayList<Job> newJobs = new ArrayList<>(helperActiveJobs);
//        newJobs.remove(job);
        Collections.sort(newJobs);

        int jobCount = newJobs.size();
        int nodeCount = AppConfig.chordState.getNodeCount();
//            int nodeCount = 7;
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

        for (int i = 0; i < AppConfig.chordState.getNodeCount(); i++) {
            int key = AppConfig.chordState.getAllNodeInfoHelper().get(i).getUuid();
            if (!fractalIds.containsKey(key)) {
                fractalIds.put(key, "");
            }
        }

        AppConfig.timestampedStandardPrint(fractalIds.toString());

//            assignFractalsIds(job);

        ArrayList<ArrayList<Job>> jobs = new ArrayList<>();
        for (Job newJob : newJobs) {
            jobs.add(prepareJobs(newJob));
        }

        AppConfig.timestampedStandardPrint(jobs.toString());

//        int jobNameLen = job.getName().length();

//            ArrayList<Job> jobs = prepareJobs(job);
        Map<String, String> fractalIdMapping = new HashMap<>();
//            for (String fractalIdName : fractalIds.values()) {
//                if (fractalIdName.equals("")) {
//                    continue;
//                }
//                String onlyFractalIdName = fractalIdName.substring(0, fractalIdName.indexOf("0"));
//                if (onlyFractalIdName.equals(job.getName())) {
//                    fractalIdMapping.put(fractalIdName, "");
//                }
//            }
        for (String oldFractalIdName : oldFractalIds.values()) {
            for (String newFractalIdName : fractalIds.values()) {
                if (oldFractalIdName.equals("") || newFractalIdName.equals("")) {
                    continue;
                }
                int oldNameLen = oldFractalIdName.length();
                if (newFractalIdName.substring(0, Math.min(newFractalIdName.length(), oldNameLen)).equals(oldFractalIdName)) {
                    fractalIdMapping.put(newFractalIdName, oldFractalIdName);
                }
            }
        }

        AppConfig.timestampedStandardPrint(fractalIdMapping.toString());

//            System.out.println(oldFractalIds.toString());
//            System.out.println(fractalIds.toString());
//            System.out.println(fractalIdMapping.toString());

        int lastAssigned = 0;
        int next = 0;
        extraNodes = nodeCount % jobCount;
        int j = 0;
        for (Job newJob : newJobs) {
            AppConfig.timestampedStandardPrint(newJob.getName());
            lastAssigned = next;
//            lastAssigned = AppConfig.chordState.getAllNodeInfoHelper().get(next).getUuid();
            int tempNodesByJob = nodesByJob;
            if (extraNodes > 0) {
                tempNodesByJob++;
            }
            if (tempNodesByJob < newJob.getN()) {
                int receiverId = AppConfig.chordState.getAllNodeInfoHelper().get(lastAssigned).getUuid();
                AppConfig.timestampedStandardPrint("Next node for key:" + receiverId + " is " + AppConfig.chordState.getNextNodeForKey(receiverId).getUuid());
                JobStopMessage jobMessage = new JobStopMessage(AppConfig.myServentInfo.getListenerPort(),
                        AppConfig.chordState.getNextNodeForKey(receiverId).getListenerPort(), Integer.toString(receiverId),
                        newJob, fractalIds, 0, null, fractalIdMapping, AppConfig.myServentInfo.getUuid());
                MessageUtil.sendMessage(jobMessage);
            } else {
                int currOverflowLevelNodes = overflowLevelNodesByJob.get(j);
                int currBaseFractalLevel = baseFractalLevelsByJob.get(j);
                for (int i = 0; i < newJob.getN(); i++) {
                    int receiverId = AppConfig.chordState.getAllNodeInfoHelper().get(lastAssigned).getUuid();
                    AppConfig.timestampedStandardPrint("Next node for key:" + receiverId + " is " + AppConfig.chordState.getNextNodeForKey(receiverId).getUuid());
                    JobStopMessage jobMessage = new JobStopMessage(AppConfig.myServentInfo.getListenerPort(),
                            AppConfig.chordState.getNextNodeForKey(receiverId).getListenerPort(), Integer.toString(receiverId),
                            jobs.get(j).get(i), fractalIds, 1, null, fractalIdMapping, AppConfig.myServentInfo.getUuid());
                    MessageUtil.sendMessage(jobMessage);

                    if (currOverflowLevelNodes > 0) {
                        lastAssigned += (int) Math.pow(newJob.getN(), currBaseFractalLevel - 1);
                        currOverflowLevelNodes -= newJob.getN();
                    } else {
                        lastAssigned += (int) Math.pow(newJob.getN(), currBaseFractalLevel - 2);
                    }
                }
            }
            j++;
            next += nodesByJob;
            if (extraNodes > 0) {
                next++;
                extraNodes--;
            }
        }

        for (Map.Entry<Integer, String> entry : fractalIds.entrySet()) {
            if (entry.getValue().equals("")) {
                JobStopMessage jobMessage = new JobStopMessage(AppConfig.myServentInfo.getListenerPort(),
                        AppConfig.chordState.getNextNodeForKey(entry.getKey()).getListenerPort(), Integer.toString(entry.getKey()),
                        null, fractalIds, 1, null, fractalIdMapping, AppConfig.myServentInfo.getUuid());
                MessageUtil.sendMessage(jobMessage);
            }
        }
    }

    public static void restructureDeparture() {
        Map<Integer, String> oldFractalIds = fractalIds;

        ArrayList<Job> newJobs = new ArrayList<>(AppConfig.activeJobs);
//            ArrayList<Job> newJobs = new ArrayList<>(helperActiveJobs);
//        newJobs.add(job);
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

        for (int i = 0; i < AppConfig.chordState.getNodeCount(); i++) {
            int key = AppConfig.chordState.getAllNodeInfoHelper().get(i).getUuid();
            if (!fractalIds.containsKey(key)) {
                fractalIds.put(key, "");
            }
        }

        AppConfig.timestampedStandardPrint(fractalIds.toString());

//            assignFractalsIds(job);

        ArrayList<ArrayList<Job>> jobs = new ArrayList<>();
        for (Job newJob : newJobs) {
            jobs.add(prepareJobs(newJob));
        }

        AppConfig.timestampedStandardPrint(jobs.toString());

//        int jobNameLen = job.getName().length();

//            ArrayList<Job> jobs = prepareJobs(job);
        Map<String, String> fractalIdMapping = new HashMap<>();
//            for (String fractalIdName : fractalIds.values()) {
//                if (fractalIdName.equals("")) {
//                    continue;
//                }
//                String onlyFractalIdName = fractalIdName.substring(0, fractalIdName.indexOf("0"));
//                if (onlyFractalIdName.equals(job.getName())) {
//                    fractalIdMapping.put(fractalIdName, "");
//                }
//            }

        for (String newFractalIdName : fractalIds.values()) {
            for (String oldFractalIdName : oldFractalIds.values()) {
                if (oldFractalIdName.equals("") || newFractalIdName.equals("")) {
                    continue;
                }
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
        int next = 0;
        extraNodes = nodeCount % jobCount;
        int j = 0;
        for (Job newJob : newJobs) {
            AppConfig.timestampedStandardPrint(newJob.getName());
            lastAssigned = next;
//            lastAssigned = AppConfig.chordState.getAllNodeInfoHelper().get(next).getUuid();
            AppConfig.timestampedStandardPrint(AppConfig.chordState.getAllNodeInfoHelper().toString());
            int tempNodesByJob = nodesByJob;
            if (extraNodes > 0) {
                tempNodesByJob++;
            }
            if (tempNodesByJob < newJob.getN()) {
                int receiverId = AppConfig.chordState.getAllNodeInfoHelper().get(lastAssigned).getUuid();
                AppConfig.timestampedStandardPrint("Next node for key:" + receiverId + " is " + AppConfig.chordState.getNextNodeForKey(receiverId).getUuid());
                JobMessage jobMessage = new JobMessage(AppConfig.myServentInfo.getListenerPort(),
                        AppConfig.chordState.getNextNodeForKey(receiverId).getListenerPort(), Integer.toString(receiverId),
                        newJob, fractalIds, 0, null, fractalIdMapping, AppConfig.chordState.getSuccessorTableAlt().get(0).getUuid());
                MessageUtil.sendMessage(jobMessage);
            } else {
                int currOverflowLevelNodes = overflowLevelNodesByJob.get(j);
                int currBaseFractalLevel = baseFractalLevelsByJob.get(j);
                for (int i = 0; i < newJob.getN(); i++) {
                    int receiverId = AppConfig.chordState.getAllNodeInfoHelper().get(lastAssigned).getUuid();
                    AppConfig.timestampedStandardPrint("Next node for key:" + receiverId + " is " + AppConfig.chordState.getNextNodeForKey(receiverId).getUuid());
                    JobMessage jobMessage = new JobMessage(AppConfig.myServentInfo.getListenerPort(),
                            AppConfig.chordState.getNextNodeForKey(receiverId).getListenerPort(), Integer.toString(receiverId),
                            jobs.get(j).get(i), fractalIds, 1, null, fractalIdMapping, AppConfig.chordState.getSuccessorTableAlt().get(0).getUuid());
                    MessageUtil.sendMessage(jobMessage);

                    if (currOverflowLevelNodes > 0) {
                        lastAssigned += (int) Math.pow(newJob.getN(), currBaseFractalLevel - 1);
                        currOverflowLevelNodes -= newJob.getN();
                    } else {
                        lastAssigned += (int) Math.pow(newJob.getN(), currBaseFractalLevel - 2);
                    }
                }
            }
            j++;
            next += nodesByJob;
            if (extraNodes > 0) {
                next++;
                extraNodes--;
            }
        }

        for (Map.Entry<Integer, String> entry : fractalIds.entrySet()) {
            if (entry.getValue().equals("")) {
                JobMessage jobMessage = new JobMessage(AppConfig.myServentInfo.getListenerPort(),
                        AppConfig.chordState.getNextNodeForKey(entry.getKey()).getListenerPort(), Integer.toString(entry.getKey()),
                        null, fractalIds, 1, null, fractalIdMapping, AppConfig.chordState.getSuccessorTableAlt().get(0).getUuid());
                MessageUtil.sendMessage(jobMessage);
            }
        }

        String myOldFractalId = AppConfig.myServentInfo.getFractalId();
        for (Map.Entry<String, String> entry : fractalIdMapping.entrySet()) {
            if (entry.getKey().equals(myOldFractalId)) {
                Integer key = Utils.getKeyByValue(fractalIds, entry.getValue());
                JobMigrationMessage jobMigrationMessage = new JobMigrationMessage(
                        AppConfig.myServentInfo.getListenerPort(),
                        AppConfig.chordState.getNextNodeForKey(key).getListenerPort(),
                        Integer.toString(key), AppConfig.jobWorker.getResults());
                MessageUtil.sendMessage(jobMigrationMessage);
            }
        }
    }

    public static void failure(int diedId) {
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


        ServentInfo leaverInfo = null;
        for (ServentInfo serventInfo : AppConfig.chordState.getAllNodeInfo()) {
            if (serventInfo.getUuid() == diedId) {
                leaverInfo = serventInfo;
                break;
            }
        }

        leaverInfo.setUuid(diedId);

        AppConfig.chordState.decrementNodeCount();
        AppConfig.chordState.updateLogLevel();

        contactBootstrap(leaverInfo.getListenerPort());
        AppConfig.chordState.removeNode(leaverInfo);
        AppConfig.chordState.getAllNodeInfoHelper().remove(leaverInfo);
        AppConfig.chordState.getSuspiciousMap().remove(diedId);
        AppConfig.chordState.getLastHeardMap().remove(diedId);

        for (FifoSendWorker sendWorker : AppConfig.fifoSendWorkers) {
            if (sendWorker.getNeighbor() == diedId) {
                sendWorker.stop();
                AppConfig.fifoSendWorkers.remove(sendWorker);
                break;
            }
        }

        for (ServentInfo serventInfo : AppConfig.chordState.getAllNodeInfo()) {
            DiedMessage diedMessage = new DiedMessage(AppConfig.myServentInfo.getListenerPort(), serventInfo.getListenerPort(),
                    Integer.toString(diedId));
            MessageUtil.sendMessage(diedMessage);
        }

//        AppConfig.diedLatch = new CountDownLatch(AppConfig.chordState.getNodeCount() - 1);

//        AppConfig.timestampedStandardPrint("STIZE");
//
//        AppConfig.timestampedStandardPrint("LATCH " + AppConfig.diedLatch.getCount());

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        try {
            AppConfig.diedLatch.await(5000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        int alsoDied = AppConfig.alsoDied.get();
        if (alsoDied != -1) {
            AppConfig.alsoDied.set(-1);
        }

        if (alsoDied == -1) {

//            BackupRequestMessage backupRequestMessage = new BackupRequestMessage(AppConfig.myServentInfo.getListenerPort(),
//                    AppConfig.chordState.getNextNodePort(), Integer.toString(diedId));
//            MessageUtil.sendMessage(backupRequestMessage);

            List<Point> myList = new ArrayList<>(AppConfig.backupSuccessor);
            List<Point> alsoDiedList = new ArrayList<>();

            AppConfig.edgeCaseLatch = new CountDownLatch(1);

            if (AppConfig.chordState.getSuspiciousMap().get(AppConfig.chordState.getSuccessorTableAlt().get(0).getUuid())) {
                if (AppConfig.chordState.getNodeCount() == 3) {
                    SuspicionRequestMessage suspicionRequestMessage = new SuspicionRequestMessage(AppConfig.myServentInfo.getListenerPort(),
                            AppConfig.chordState.getPredecessor().getListenerPort(), Integer.toString(AppConfig.chordState.getPredecessor().getUuid()),
                            AppConfig.myServentInfo.getUuid(), AppConfig.chordState.getSuccessorTableAlt().get(0).getUuid());
                    MessageUtil.sendMessage(suspicionRequestMessage);
                } else {
                    SuspicionRequestMessage suspicionRequestMessage = new SuspicionRequestMessage(AppConfig.myServentInfo.getListenerPort(),
                            AppConfig.chordState.getSuccessorTableAlt().get(1).getListenerPort(), Integer.toString(AppConfig.chordState.getPredecessor().getUuid()),
                            AppConfig.myServentInfo.getUuid(), AppConfig.chordState.getSuccessorTableAlt().get(0).getUuid());
                    MessageUtil.sendMessage(suspicionRequestMessage);
                }

                try {
                    AppConfig.edgeCaseLatch.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                boolean reallyReallySuspicious = false;
                if (AppConfig.chordState.getReallySuspucious().containsKey(AppConfig.chordState.getSuccessorTableAlt().get(0).getUuid())) {
                    try {
                        Thread.sleep(AppConfig.HARD_FAILURE_TIME);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    if (AppConfig.chordState.getSuspiciousMap().get(AppConfig.chordState.getSuccessorTableAlt().get(0).getUuid())) {
                        reallyReallySuspicious = true;
                    }
                }

                if (reallyReallySuspicious) {
                    alsoDied = AppConfig.chordState.getSuccessorTableAlt().get(0).getUuid();

                    if (AppConfig.chordState.getNodeCount() == 3) {
                        BackupRequestMessage backupRequestMessage = new BackupRequestMessage(AppConfig.myServentInfo.getListenerPort(),
                                AppConfig.chordState.getPredecessor().getListenerPort(), Integer.toString(alsoDied), true);
                        MessageUtil.sendMessage(backupRequestMessage);
                    } else {
                        BackupRequestMessage backupRequestMessage = new BackupRequestMessage(AppConfig.myServentInfo.getListenerPort(),
                                AppConfig.chordState.getSuccessorTableAlt().get(1).getListenerPort(), Integer.toString(alsoDied), true);
                        MessageUtil.sendMessage(backupRequestMessage);
                    }

                    try {
                        alsoDiedList = AppConfig.backupsReceived.take();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    leaverInfo = null;
                    for (ServentInfo serventInfo : AppConfig.chordState.getAllNodeInfo()) {
                        if (serventInfo.getUuid() == alsoDied) {
                            leaverInfo = serventInfo;
                            break;
                        }
                    }

                    leaverInfo.setUuid(alsoDied);

                    AppConfig.chordState.decrementNodeCount();
                    AppConfig.chordState.updateLogLevel();

                    contactBootstrap(leaverInfo.getListenerPort());
                    AppConfig.chordState.removeNode(leaverInfo);
                    AppConfig.chordState.getAllNodeInfoHelper().remove(leaverInfo);
                    AppConfig.chordState.getSuspiciousMap().remove(alsoDied);
                    AppConfig.chordState.getLastHeardMap().remove(alsoDied);

                    for (FifoSendWorker sendWorker : AppConfig.fifoSendWorkers) {
                        if (sendWorker.getNeighbor() == alsoDied) {
                            sendWorker.stop();
                            AppConfig.fifoSendWorkers.remove(sendWorker);
                            break;
                        }
                    }

                    for (ServentInfo serventInfo : AppConfig.chordState.getAllNodeInfo()) {
                        DiedMessage diedMessage = new DiedMessage(AppConfig.myServentInfo.getListenerPort(), serventInfo.getListenerPort(),
                                Integer.toString(alsoDied));
                        MessageUtil.sendMessage(diedMessage);
                    }
                }
            }

            if (AppConfig.activeJobs.size() == 0) {
                return;
            }

            //////////////////////////////////////////
            Map<Integer, String> oldFractalIds = fractalIds;

            ArrayList<Job> newJobs = new ArrayList<>(AppConfig.activeJobs);
//            ArrayList<Job> newJobs = new ArrayList<>(helperActiveJobs);
//        newJobs.add(job);
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

            for (int i = 0; i < AppConfig.chordState.getNodeCount(); i++) {
                int key = AppConfig.chordState.getAllNodeInfoHelper().get(i).getUuid();
                if (!fractalIds.containsKey(key)) {
                    fractalIds.put(key, "");
                }
            }

            AppConfig.timestampedStandardPrint(fractalIds.toString());

//            assignFractalsIds(job);

            ArrayList<ArrayList<Job>> jobs = new ArrayList<>();
            for (Job newJob : newJobs) {
                jobs.add(prepareJobs(newJob));
            }

            AppConfig.timestampedStandardPrint(jobs.toString());

//        int jobNameLen = job.getName().length();

//            ArrayList<Job> jobs = prepareJobs(job);
            Map<String, String> fractalIdMapping = new HashMap<>();
//            for (String fractalIdName : fractalIds.values()) {
//                if (fractalIdName.equals("")) {
//                    continue;
//                }
//                String onlyFractalIdName = fractalIdName.substring(0, fractalIdName.indexOf("0"));
//                if (onlyFractalIdName.equals(job.getName())) {
//                    fractalIdMapping.put(fractalIdName, "");
//                }
//            }

            for (String newFractalIdName : fractalIds.values()) {
                for (String oldFractalIdName : oldFractalIds.values()) {
                    if (oldFractalIdName.equals("") || newFractalIdName.equals("")) {
                        continue;
                    }
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
            int next = 0;
            extraNodes = nodeCount % jobCount;
            int j = 0;
            for (Job newJob : newJobs) {
                AppConfig.timestampedStandardPrint(newJob.getName());
                lastAssigned = next;
//            lastAssigned = AppConfig.chordState.getAllNodeInfoHelper().get(next).getUuid();
                AppConfig.timestampedStandardPrint(AppConfig.chordState.getAllNodeInfoHelper().toString());
                int tempNodesByJob = nodesByJob;
                if (extraNodes > 0) {
                    tempNodesByJob++;
                }
                if (tempNodesByJob < newJob.getN()) {
                    int receiverId = AppConfig.chordState.getAllNodeInfoHelper().get(lastAssigned).getUuid();
                    AppConfig.timestampedStandardPrint("Next node for key:" + receiverId + " is " + AppConfig.chordState.getNextNodeForKey(receiverId).getUuid());
                    JobMessage jobMessage = new JobMessage(AppConfig.myServentInfo.getListenerPort(),
                            AppConfig.chordState.getNextNodeForKey(receiverId).getListenerPort(), Integer.toString(receiverId),
                            newJob, fractalIds, 0, null, fractalIdMapping, AppConfig.myServentInfo.getUuid());
                    MessageUtil.sendMessage(jobMessage);
                } else {
                    int currOverflowLevelNodes = overflowLevelNodesByJob.get(j);
                    int currBaseFractalLevel = baseFractalLevelsByJob.get(j);
                    for (int i = 0; i < newJob.getN(); i++) {
                        int receiverId = AppConfig.chordState.getAllNodeInfoHelper().get(lastAssigned).getUuid();
                        AppConfig.timestampedStandardPrint("Next node for key:" + receiverId + " is " + AppConfig.chordState.getNextNodeForKey(receiverId).getUuid());
                        JobMessage jobMessage = new JobMessage(AppConfig.myServentInfo.getListenerPort(),
                                AppConfig.chordState.getNextNodeForKey(receiverId).getListenerPort(), Integer.toString(receiverId),
                                jobs.get(j).get(i), fractalIds, 1, null, fractalIdMapping, AppConfig.myServentInfo.getUuid());
                        MessageUtil.sendMessage(jobMessage);

                        if (currOverflowLevelNodes > 0) {
                            lastAssigned += (int) Math.pow(newJob.getN(), currBaseFractalLevel - 1);
                            currOverflowLevelNodes -= newJob.getN();
                        } else {
                            lastAssigned += (int) Math.pow(newJob.getN(), currBaseFractalLevel - 2);
                        }
                    }
                }
                j++;
                next += nodesByJob;
                if (extraNodes > 0) {
                    next++;
                    extraNodes--;
                }
            }

            for (Map.Entry<Integer, String> entry : fractalIds.entrySet()) {
                if (entry.getValue().equals("")) {
                    JobMessage jobMessage = new JobMessage(AppConfig.myServentInfo.getListenerPort(),
                            AppConfig.chordState.getNextNodeForKey(entry.getKey()).getListenerPort(), Integer.toString(entry.getKey()),
                            null, fractalIds, 1, null, fractalIdMapping, AppConfig.myServentInfo.getUuid());
                    MessageUtil.sendMessage(jobMessage);
                }
            }

            String firstFractalId = oldFractalIds.get(diedId);
            for (Map.Entry<String, String> entry : fractalIdMapping.entrySet()) {
                if (entry.getKey().equals(firstFractalId)) {
                    Integer key = Utils.getKeyByValue(fractalIds, entry.getValue());
                    JobMigrationMessage jobMigrationMessage = new JobMigrationMessage(
                            AppConfig.myServentInfo.getListenerPort(),
                            AppConfig.chordState.getNextNodeForKey(key).getListenerPort(),
                            Integer.toString(key), myList);
                    MessageUtil.sendMessage(jobMigrationMessage);
                }
            }

            if (alsoDied != -1) {
                String secondFractalId = oldFractalIds.get(alsoDied);
                for (Map.Entry<String, String> entry : fractalIdMapping.entrySet()) {
                    if (entry.getKey().equals(secondFractalId)) {
                        Integer key = Utils.getKeyByValue(fractalIds, entry.getValue());
                        JobMigrationMessage jobMigrationMessage = new JobMigrationMessage(
                                AppConfig.myServentInfo.getListenerPort(),
                                AppConfig.chordState.getNextNodeForKey(key).getListenerPort(),
                                Integer.toString(key), alsoDiedList);
                        MessageUtil.sendMessage(jobMigrationMessage);
                    }
                }
            }


        } else {
            if (AppConfig.whoNoticed.get() < AppConfig.myServentInfo.getUuid()) {
                return;
            }

            if (AppConfig.activeJobs.size() == 0) {
                return;
            }

            ServentInfo last = null;
            int idToTarget = -1;
            for (ServentInfo serventInfo : AppConfig.chordState.getAllNodeInfoHelper()) {
                if (serventInfo.getUuid() > alsoDied) {
                    if (last != null) {
                        idToTarget = last.getUuid();
                        break;
                    }
                    idToTarget = -2;
                    break;
                }
                last = serventInfo;
            }

            if (idToTarget == -2) {
                last = AppConfig.chordState.getAllNodeInfoHelper().get(AppConfig.chordState.getNodeCount() - 1);
                idToTarget = AppConfig.chordState.getAllNodeInfoHelper().get(AppConfig.chordState.getNodeCount() - 1).getUuid();
            } else if (idToTarget == -1) {
                idToTarget = last.getUuid();
//                last = AppConfig.chordState.getAllNodeInfoHelper().get(AppConfig.chordState.getNodeCount() - 2);
            }

            BackupRequestMessage backupRequestMessage = new BackupRequestMessage(AppConfig.myServentInfo.getListenerPort(),
                    last.getListenerPort(), Integer.toString(alsoDied), false);
            MessageUtil.sendMessage(backupRequestMessage);

            List<Point> myList = new ArrayList<>(AppConfig.backupSuccessor);
            List<Point> alsoDiedList = new ArrayList<>();

            try {
                alsoDiedList = AppConfig.backupsReceived.take();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            //////////////////////////////////////////
            Map<Integer, String> oldFractalIds = fractalIds;

            ArrayList<Job> newJobs = new ArrayList<>(AppConfig.activeJobs);
//            ArrayList<Job> newJobs = new ArrayList<>(helperActiveJobs);
//        newJobs.add(job);
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

            for (int i = 0; i < AppConfig.chordState.getNodeCount(); i++) {
                int key = AppConfig.chordState.getAllNodeInfoHelper().get(i).getUuid();
                if (!fractalIds.containsKey(key)) {
                    fractalIds.put(key, "");
                }
            }

            AppConfig.timestampedStandardPrint(fractalIds.toString());

//            assignFractalsIds(job);

            ArrayList<ArrayList<Job>> jobs = new ArrayList<>();
            for (Job newJob : newJobs) {
                jobs.add(prepareJobs(newJob));
            }

            AppConfig.timestampedStandardPrint(jobs.toString());

//        int jobNameLen = job.getName().length();

//            ArrayList<Job> jobs = prepareJobs(job);
            Map<String, String> fractalIdMapping = new HashMap<>();
//            for (String fractalIdName : fractalIds.values()) {
//                if (fractalIdName.equals("")) {
//                    continue;
//                }
//                String onlyFractalIdName = fractalIdName.substring(0, fractalIdName.indexOf("0"));
//                if (onlyFractalIdName.equals(job.getName())) {
//                    fractalIdMapping.put(fractalIdName, "");
//                }
//            }

            for (String newFractalIdName : fractalIds.values()) {
                for (String oldFractalIdName : oldFractalIds.values()) {
                    if (oldFractalIdName.equals("") || newFractalIdName.equals("")) {
                        continue;
                    }
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
            int next = 0;
            extraNodes = nodeCount % jobCount;
            int j = 0;
            for (Job newJob : newJobs) {
                AppConfig.timestampedStandardPrint(newJob.getName());
                lastAssigned = next;
//            lastAssigned = AppConfig.chordState.getAllNodeInfoHelper().get(next).getUuid();
                int tempNodesByJob = nodesByJob;
                if (extraNodes > 0) {
                    tempNodesByJob++;
                }
                if (tempNodesByJob < newJob.getN()) {
                    int receiverId = AppConfig.chordState.getAllNodeInfoHelper().get(lastAssigned).getUuid();
                    AppConfig.timestampedStandardPrint("Next node for key:" + receiverId + " is " + AppConfig.chordState.getNextNodeForKey(receiverId).getUuid());
                    JobMessage jobMessage = new JobMessage(AppConfig.myServentInfo.getListenerPort(),
                            AppConfig.chordState.getNextNodeForKey(receiverId).getListenerPort(), Integer.toString(receiverId),
                            newJob, fractalIds, 0, null, fractalIdMapping, AppConfig.myServentInfo.getUuid());
                    MessageUtil.sendMessage(jobMessage);
                } else {
                    int currOverflowLevelNodes = overflowLevelNodesByJob.get(j);
                    int currBaseFractalLevel = baseFractalLevelsByJob.get(j);
                    for (int i = 0; i < newJob.getN(); i++) {
                        int receiverId = AppConfig.chordState.getAllNodeInfoHelper().get(lastAssigned).getUuid();
                        AppConfig.timestampedStandardPrint("Next node for key:" + receiverId + " is " + AppConfig.chordState.getNextNodeForKey(receiverId).getUuid());
                        JobMessage jobMessage = new JobMessage(AppConfig.myServentInfo.getListenerPort(),
                                AppConfig.chordState.getNextNodeForKey(receiverId).getListenerPort(), Integer.toString(receiverId),
                                jobs.get(j).get(i), fractalIds, 1, null, fractalIdMapping, AppConfig.myServentInfo.getUuid());
                        MessageUtil.sendMessage(jobMessage);

                        if (currOverflowLevelNodes > 0) {
                            lastAssigned += (int) Math.pow(newJob.getN(), currBaseFractalLevel - 1);
                            currOverflowLevelNodes -= newJob.getN();
                        } else {
                            lastAssigned += (int) Math.pow(newJob.getN(), currBaseFractalLevel - 2);
                        }
                    }
                }
                j++;
                next += nodesByJob;
                if (extraNodes > 0) {
                    next++;
                    extraNodes--;
                }
            }

            for (Map.Entry<Integer, String> entry : fractalIds.entrySet()) {
                if (entry.getValue().equals("")) {
                    JobMessage jobMessage = new JobMessage(AppConfig.myServentInfo.getListenerPort(),
                            AppConfig.chordState.getNextNodeForKey(entry.getKey()).getListenerPort(), Integer.toString(entry.getKey()),
                            null, fractalIds, 1, null, fractalIdMapping, AppConfig.myServentInfo.getUuid());
                    MessageUtil.sendMessage(jobMessage);
                }
            }

            String firstFractalId = oldFractalIds.get(diedId);
            for (Map.Entry<String, String> entry : fractalIdMapping.entrySet()) {
                if (entry.getKey().equals(firstFractalId)) {
                    Integer key = Utils.getKeyByValue(fractalIds, entry.getValue());
                    JobMigrationMessage jobMigrationMessage = new JobMigrationMessage(
                            AppConfig.myServentInfo.getListenerPort(),
                            AppConfig.chordState.getNextNodeForKey(key).getListenerPort(),
                            Integer.toString(key), myList);
                    MessageUtil.sendMessage(jobMigrationMessage);
                }
            }

            String secondFractalId = oldFractalIds.get(alsoDied);
            for (Map.Entry<String, String> entry : fractalIdMapping.entrySet()) {
                if (entry.getKey().equals(secondFractalId)) {
                    Integer key = Utils.getKeyByValue(fractalIds, entry.getValue());
                    JobMigrationMessage jobMigrationMessage = new JobMigrationMessage(
                            AppConfig.myServentInfo.getListenerPort(),
                            AppConfig.chordState.getNextNodeForKey(key).getListenerPort(),
                            Integer.toString(key), alsoDiedList);
                    MessageUtil.sendMessage(jobMigrationMessage);
                }
            }
        }
        AppConfig.chordState.getReallySuspucious().clear();
    }

    public static void failure2_3() {
        int idFirst = AppConfig.chordState.getAllNodeInfo().get(0).getUuid();
        int idSecond = AppConfig.chordState.getAllNodeInfo().get(1).getUuid();

        ServentInfo leaverInfo = null;
        for (ServentInfo serventInfo : AppConfig.chordState.getAllNodeInfo()) {
            if (serventInfo.getUuid() == idFirst) {
                leaverInfo = serventInfo;
                break;
            }
        }

        leaverInfo.setUuid(idFirst);

        AppConfig.chordState.decrementNodeCount();
        AppConfig.chordState.updateLogLevel();

        contactBootstrap(leaverInfo.getListenerPort());
        AppConfig.chordState.removeNode(leaverInfo);
        AppConfig.chordState.getAllNodeInfoHelper().remove(leaverInfo);
        AppConfig.chordState.getSuspiciousMap().remove(idFirst);
        AppConfig.chordState.getLastHeardMap().remove(idFirst);

        for (FifoSendWorker sendWorker : AppConfig.fifoSendWorkers) {
            if (sendWorker.getNeighbor() == idFirst) {
                sendWorker.stop();
                AppConfig.fifoSendWorkers.remove(sendWorker);
                break;
            }
        }

        leaverInfo = null;
        for (ServentInfo serventInfo : AppConfig.chordState.getAllNodeInfo()) {
            if (serventInfo.getUuid() == idSecond) {
                leaverInfo = serventInfo;
                break;
            }
        }

        leaverInfo.setUuid(idSecond);

        AppConfig.chordState.decrementNodeCount();
        AppConfig.chordState.updateLogLevel();

        contactBootstrap(leaverInfo.getListenerPort());
        AppConfig.chordState.removeNode(leaverInfo);
        AppConfig.chordState.getAllNodeInfoHelper().remove(leaverInfo);
        AppConfig.chordState.getSuspiciousMap().remove(idSecond);
        AppConfig.chordState.getLastHeardMap().remove(idSecond);

        for (FifoSendWorker sendWorker : AppConfig.fifoSendWorkers) {
            if (sendWorker.getNeighbor() == idSecond) {
                sendWorker.stop();
                AppConfig.fifoSendWorkers.remove(sendWorker);
                break;
            }
        }

        if (AppConfig.activeJobs.size() == 0) {
            return;
        }

        Map<Integer, String> oldFractalIds = fractalIds;

        ArrayList<Job> newJobs = new ArrayList<>(AppConfig.activeJobs);
//            ArrayList<Job> newJobs = new ArrayList<>(helperActiveJobs);
//        newJobs.add(job);
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

        for (int i = 0; i < AppConfig.chordState.getNodeCount(); i++) {
            int key = AppConfig.chordState.getAllNodeInfoHelper().get(i).getUuid();
            if (!fractalIds.containsKey(key)) {
                fractalIds.put(key, "");
            }
        }

        AppConfig.timestampedStandardPrint(fractalIds.toString());

//            assignFractalsIds(job);

        ArrayList<ArrayList<Job>> jobs = new ArrayList<>();
        for (Job newJob : newJobs) {
            jobs.add(prepareJobs(newJob));
        }

        AppConfig.timestampedStandardPrint(jobs.toString());

//        int jobNameLen = job.getName().length();

//            ArrayList<Job> jobs = prepareJobs(job);
        Map<String, String> fractalIdMapping = new HashMap<>();
//            for (String fractalIdName : fractalIds.values()) {
//                if (fractalIdName.equals("")) {
//                    continue;
//                }
//                String onlyFractalIdName = fractalIdName.substring(0, fractalIdName.indexOf("0"));
//                if (onlyFractalIdName.equals(job.getName())) {
//                    fractalIdMapping.put(fractalIdName, "");
//                }
//            }

        for (String newFractalIdName : fractalIds.values()) {
            for (String oldFractalIdName : oldFractalIds.values()) {
                if (oldFractalIdName.equals("") || newFractalIdName.equals("")) {
                    continue;
                }
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
        int next = 0;
        extraNodes = nodeCount % jobCount;
        int j = 0;
        for (Job newJob : newJobs) {
            AppConfig.timestampedStandardPrint(newJob.getName());
            lastAssigned = next;
//            lastAssigned = AppConfig.chordState.getAllNodeInfoHelper().get(next).getUuid();
            AppConfig.timestampedStandardPrint(AppConfig.chordState.getAllNodeInfoHelper().toString());
            int tempNodesByJob = nodesByJob;
            if (extraNodes > 0) {
                tempNodesByJob++;
            }
            if (tempNodesByJob < newJob.getN()) {
                int receiverId = AppConfig.chordState.getAllNodeInfoHelper().get(lastAssigned).getUuid();
                AppConfig.timestampedStandardPrint("Next node for key:" + receiverId + " is " + AppConfig.chordState.getNextNodeForKey(receiverId).getUuid());
                JobMessage jobMessage = new JobMessage(AppConfig.myServentInfo.getListenerPort(),
                        AppConfig.chordState.getNextNodeForKey(receiverId).getListenerPort(), Integer.toString(receiverId),
                        newJob, fractalIds, 0, null, fractalIdMapping, AppConfig.myServentInfo.getUuid());
                MessageUtil.sendMessage(jobMessage);
            } else {
                int currOverflowLevelNodes = overflowLevelNodesByJob.get(j);
                int currBaseFractalLevel = baseFractalLevelsByJob.get(j);
                for (int i = 0; i < newJob.getN(); i++) {
                    int receiverId = AppConfig.chordState.getAllNodeInfoHelper().get(lastAssigned).getUuid();
                    AppConfig.timestampedStandardPrint("Next node for key:" + receiverId + " is " + AppConfig.chordState.getNextNodeForKey(receiverId).getUuid());
                    JobMessage jobMessage = new JobMessage(AppConfig.myServentInfo.getListenerPort(),
                            AppConfig.chordState.getNextNodeForKey(receiverId).getListenerPort(), Integer.toString(receiverId),
                            jobs.get(j).get(i), fractalIds, 1, null, fractalIdMapping, AppConfig.myServentInfo.getUuid());
                    MessageUtil.sendMessage(jobMessage);

                    if (currOverflowLevelNodes > 0) {
                        lastAssigned += (int) Math.pow(newJob.getN(), currBaseFractalLevel - 1);
                        currOverflowLevelNodes -= newJob.getN();
                    } else {
                        lastAssigned += (int) Math.pow(newJob.getN(), currBaseFractalLevel - 2);
                    }
                }
            }
            j++;
            next += nodesByJob;
            if (extraNodes > 0) {
                next++;
                extraNodes--;
            }
        }

        for (Map.Entry<Integer, String> entry : fractalIds.entrySet()) {
            if (entry.getValue().equals("")) {
                JobMessage jobMessage = new JobMessage(AppConfig.myServentInfo.getListenerPort(),
                        AppConfig.chordState.getNextNodeForKey(entry.getKey()).getListenerPort(), Integer.toString(entry.getKey()),
                        null, fractalIds, 1, null, fractalIdMapping, AppConfig.myServentInfo.getUuid());
                MessageUtil.sendMessage(jobMessage);
            }
        }

        String firstFractalId = oldFractalIds.get(idFirst);
        for (Map.Entry<String, String> entry : fractalIdMapping.entrySet()) {
            if (entry.getKey().equals(firstFractalId)) {
                Integer key = Utils.getKeyByValue(fractalIds, entry.getValue());
                JobMigrationMessage jobMigrationMessage = new JobMigrationMessage(
                        AppConfig.myServentInfo.getListenerPort(),
                        AppConfig.chordState.getNextNodeForKey(key).getListenerPort(),
                        Integer.toString(key), AppConfig.backupSuccessor);
                MessageUtil.sendMessage(jobMigrationMessage);
            }
        }

        String secondFractalId = oldFractalIds.get(idSecond);
        for (Map.Entry<String, String> entry : fractalIdMapping.entrySet()) {
            if (entry.getKey().equals(secondFractalId)) {
                Integer key = Utils.getKeyByValue(fractalIds, entry.getValue());
                JobMigrationMessage jobMigrationMessage = new JobMigrationMessage(
                        AppConfig.myServentInfo.getListenerPort(),
                        AppConfig.chordState.getNextNodeForKey(key).getListenerPort(),
                        Integer.toString(key), AppConfig.backupPredecessor);
                MessageUtil.sendMessage(jobMigrationMessage);
            }
        }
    }

    public static void failure3_1(int diedId) {
        ServentInfo leaverInfo = null;
        for (ServentInfo serventInfo : AppConfig.chordState.getAllNodeInfo()) {
            if (serventInfo.getUuid() == diedId) {
                leaverInfo = serventInfo;
                break;
            }
        }

        leaverInfo.setUuid(diedId);

        AppConfig.chordState.decrementNodeCount();
        AppConfig.chordState.updateLogLevel();

        contactBootstrap(leaverInfo.getListenerPort());
        AppConfig.chordState.removeNode(leaverInfo);
        AppConfig.chordState.getAllNodeInfoHelper().remove(leaverInfo);
        AppConfig.chordState.getSuspiciousMap().remove(diedId);
        AppConfig.chordState.getLastHeardMap().remove(diedId);

        for (FifoSendWorker sendWorker : AppConfig.fifoSendWorkers) {
            if (sendWorker.getNeighbor() == diedId) {
                sendWorker.stop();
                AppConfig.fifoSendWorkers.remove(sendWorker);
                break;
            }
        }

        DiedMessage diedMessage = new DiedMessage(AppConfig.myServentInfo.getListenerPort(),
                AppConfig.chordState.getPredecessor().getListenerPort(), Integer.toString(diedId));
        MessageUtil.sendMessage(diedMessage);

        if (AppConfig.activeJobs.size() == 0) {
            return;
        }

        Map<Integer, String> oldFractalIds = fractalIds;

        ArrayList<Job> newJobs = new ArrayList<>(AppConfig.activeJobs);
//            ArrayList<Job> newJobs = new ArrayList<>(helperActiveJobs);
//        newJobs.add(job);
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

        for (int i = 0; i < AppConfig.chordState.getNodeCount(); i++) {
            int key = AppConfig.chordState.getAllNodeInfoHelper().get(i).getUuid();
            if (!fractalIds.containsKey(key)) {
                fractalIds.put(key, "");
            }
        }

        AppConfig.timestampedStandardPrint(fractalIds.toString());

//            assignFractalsIds(job);

        ArrayList<ArrayList<Job>> jobs = new ArrayList<>();
        for (Job newJob : newJobs) {
            jobs.add(prepareJobs(newJob));
        }

        AppConfig.timestampedStandardPrint(jobs.toString());

//        int jobNameLen = job.getName().length();

//            ArrayList<Job> jobs = prepareJobs(job);
        Map<String, String> fractalIdMapping = new HashMap<>();
//            for (String fractalIdName : fractalIds.values()) {
//                if (fractalIdName.equals("")) {
//                    continue;
//                }
//                String onlyFractalIdName = fractalIdName.substring(0, fractalIdName.indexOf("0"));
//                if (onlyFractalIdName.equals(job.getName())) {
//                    fractalIdMapping.put(fractalIdName, "");
//                }
//            }

        for (String newFractalIdName : fractalIds.values()) {
            for (String oldFractalIdName : oldFractalIds.values()) {
                if (oldFractalIdName.equals("") || newFractalIdName.equals("")) {
                    continue;
                }
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
        int next = 0;
        extraNodes = nodeCount % jobCount;
        int j = 0;
        for (Job newJob : newJobs) {
            AppConfig.timestampedStandardPrint(newJob.getName());
            lastAssigned = next;
//            lastAssigned = AppConfig.chordState.getAllNodeInfoHelper().get(next).getUuid();
            AppConfig.timestampedStandardPrint(AppConfig.chordState.getAllNodeInfoHelper().toString());
            int tempNodesByJob = nodesByJob;
            if (extraNodes > 0) {
                tempNodesByJob++;
            }
            if (tempNodesByJob < newJob.getN()) {
                int receiverId = AppConfig.chordState.getAllNodeInfoHelper().get(lastAssigned).getUuid();
                AppConfig.timestampedStandardPrint("Next node for key:" + receiverId + " is " + AppConfig.chordState.getNextNodeForKey(receiverId).getUuid());
                JobMessage jobMessage = new JobMessage(AppConfig.myServentInfo.getListenerPort(),
                        AppConfig.chordState.getNextNodeForKey(receiverId).getListenerPort(), Integer.toString(receiverId),
                        newJob, fractalIds, 0, null, fractalIdMapping, AppConfig.myServentInfo.getUuid());
                MessageUtil.sendMessage(jobMessage);
            } else {
                int currOverflowLevelNodes = overflowLevelNodesByJob.get(j);
                int currBaseFractalLevel = baseFractalLevelsByJob.get(j);
                for (int i = 0; i < newJob.getN(); i++) {
                    int receiverId = AppConfig.chordState.getAllNodeInfoHelper().get(lastAssigned).getUuid();
                    AppConfig.timestampedStandardPrint("Next node for key:" + receiverId + " is " + AppConfig.chordState.getNextNodeForKey(receiverId).getUuid());
                    JobMessage jobMessage = new JobMessage(AppConfig.myServentInfo.getListenerPort(),
                            AppConfig.chordState.getNextNodeForKey(receiverId).getListenerPort(), Integer.toString(receiverId),
                            jobs.get(j).get(i), fractalIds, 1, null, fractalIdMapping, AppConfig.myServentInfo.getUuid());
                    MessageUtil.sendMessage(jobMessage);

                    if (currOverflowLevelNodes > 0) {
                        lastAssigned += (int) Math.pow(newJob.getN(), currBaseFractalLevel - 1);
                        currOverflowLevelNodes -= newJob.getN();
                    } else {
                        lastAssigned += (int) Math.pow(newJob.getN(), currBaseFractalLevel - 2);
                    }
                }
            }
            j++;
            next += nodesByJob;
            if (extraNodes > 0) {
                next++;
                extraNodes--;
            }
        }

        for (Map.Entry<Integer, String> entry : fractalIds.entrySet()) {
            if (entry.getValue().equals("")) {
                JobMessage jobMessage = new JobMessage(AppConfig.myServentInfo.getListenerPort(),
                        AppConfig.chordState.getNextNodeForKey(entry.getKey()).getListenerPort(), Integer.toString(entry.getKey()),
                        null, fractalIds, 1, null, fractalIdMapping, AppConfig.myServentInfo.getUuid());
                MessageUtil.sendMessage(jobMessage);
            }
        }

        String firstFractalId = oldFractalIds.get(diedId);
        for (Map.Entry<String, String> entry : fractalIdMapping.entrySet()) {
            if (entry.getKey().equals(firstFractalId)) {
                Integer key = Utils.getKeyByValue(fractalIds, entry.getValue());
                JobMigrationMessage jobMigrationMessage = new JobMigrationMessage(
                        AppConfig.myServentInfo.getListenerPort(),
                        AppConfig.chordState.getNextNodeForKey(key).getListenerPort(),
                        Integer.toString(key), AppConfig.backupSuccessor);
                MessageUtil.sendMessage(jobMigrationMessage);
            }
        }
    }

    public static void failure2_1() {
        int diedId = AppConfig.chordState.getSuccessorTableAlt().get(0).getUuid();

        ServentInfo leaverInfo = null;
        for (ServentInfo serventInfo : AppConfig.chordState.getAllNodeInfo()) {
            if (serventInfo.getUuid() == diedId) {
                leaverInfo = serventInfo;
                break;
            }
        }

        leaverInfo.setUuid(diedId);

        AppConfig.chordState.decrementNodeCount();
        AppConfig.chordState.updateLogLevel();

        contactBootstrap(leaverInfo.getListenerPort());
        AppConfig.chordState.removeNode(leaverInfo);
        AppConfig.chordState.getAllNodeInfoHelper().remove(leaverInfo);
        AppConfig.chordState.getSuspiciousMap().remove(diedId);
        AppConfig.chordState.getLastHeardMap().remove(diedId);

        for (FifoSendWorker sendWorker : AppConfig.fifoSendWorkers) {
            if (sendWorker.getNeighbor() == diedId) {
                sendWorker.stop();
                AppConfig.fifoSendWorkers.remove(sendWorker);
                break;
            }
        }

        if (AppConfig.activeJobs.size() == 0) {
            return;
        }

        Map<Integer, String> oldFractalIds = fractalIds;

        ArrayList<Job> newJobs = new ArrayList<>(AppConfig.activeJobs);
//            ArrayList<Job> newJobs = new ArrayList<>(helperActiveJobs);
//        newJobs.add(job);
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

        for (int i = 0; i < AppConfig.chordState.getNodeCount(); i++) {
            int key = AppConfig.chordState.getAllNodeInfoHelper().get(i).getUuid();
            if (!fractalIds.containsKey(key)) {
                fractalIds.put(key, "");
            }
        }

        AppConfig.timestampedStandardPrint(fractalIds.toString());

//            assignFractalsIds(job);

        ArrayList<ArrayList<Job>> jobs = new ArrayList<>();
        for (Job newJob : newJobs) {
            jobs.add(prepareJobs(newJob));
        }

        AppConfig.timestampedStandardPrint(jobs.toString());

//        int jobNameLen = job.getName().length();

//            ArrayList<Job> jobs = prepareJobs(job);
        Map<String, String> fractalIdMapping = new HashMap<>();
//            for (String fractalIdName : fractalIds.values()) {
//                if (fractalIdName.equals("")) {
//                    continue;
//                }
//                String onlyFractalIdName = fractalIdName.substring(0, fractalIdName.indexOf("0"));
//                if (onlyFractalIdName.equals(job.getName())) {
//                    fractalIdMapping.put(fractalIdName, "");
//                }
//            }

        for (String newFractalIdName : fractalIds.values()) {
            for (String oldFractalIdName : oldFractalIds.values()) {
                if (oldFractalIdName.equals("") || newFractalIdName.equals("")) {
                    continue;
                }
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
        int next = 0;
        extraNodes = nodeCount % jobCount;
        int j = 0;
        for (Job newJob : newJobs) {
            AppConfig.timestampedStandardPrint(newJob.getName());
            lastAssigned = next;
//            lastAssigned = AppConfig.chordState.getAllNodeInfoHelper().get(next).getUuid();
            AppConfig.timestampedStandardPrint(AppConfig.chordState.getAllNodeInfoHelper().toString());
            int tempNodesByJob = nodesByJob;
            if (extraNodes > 0) {
                tempNodesByJob++;
            }
            if (tempNodesByJob < newJob.getN()) {
                int receiverId = AppConfig.chordState.getAllNodeInfoHelper().get(lastAssigned).getUuid();
                AppConfig.timestampedStandardPrint("Next node for key:" + receiverId + " is " + AppConfig.chordState.getNextNodeForKey(receiverId).getUuid());
                JobMessage jobMessage = new JobMessage(AppConfig.myServentInfo.getListenerPort(),
                        AppConfig.chordState.getNextNodeForKey(receiverId).getListenerPort(), Integer.toString(receiverId),
                        newJob, fractalIds, 0, null, fractalIdMapping, AppConfig.myServentInfo.getUuid());
                MessageUtil.sendMessage(jobMessage);
            } else {
                int currOverflowLevelNodes = overflowLevelNodesByJob.get(j);
                int currBaseFractalLevel = baseFractalLevelsByJob.get(j);
                for (int i = 0; i < newJob.getN(); i++) {
                    int receiverId = AppConfig.chordState.getAllNodeInfoHelper().get(lastAssigned).getUuid();
                    AppConfig.timestampedStandardPrint("Next node for key:" + receiverId + " is " + AppConfig.chordState.getNextNodeForKey(receiverId).getUuid());
                    JobMessage jobMessage = new JobMessage(AppConfig.myServentInfo.getListenerPort(),
                            AppConfig.chordState.getNextNodeForKey(receiverId).getListenerPort(), Integer.toString(receiverId),
                            jobs.get(j).get(i), fractalIds, 1, null, fractalIdMapping, AppConfig.myServentInfo.getUuid());
                    MessageUtil.sendMessage(jobMessage);

                    if (currOverflowLevelNodes > 0) {
                        lastAssigned += (int) Math.pow(newJob.getN(), currBaseFractalLevel - 1);
                        currOverflowLevelNodes -= newJob.getN();
                    } else {
                        lastAssigned += (int) Math.pow(newJob.getN(), currBaseFractalLevel - 2);
                    }
                }
            }
            j++;
            next += nodesByJob;
            if (extraNodes > 0) {
                next++;
                extraNodes--;
            }
        }

        for (Map.Entry<Integer, String> entry : fractalIds.entrySet()) {
            if (entry.getValue().equals("")) {
                JobMessage jobMessage = new JobMessage(AppConfig.myServentInfo.getListenerPort(),
                        AppConfig.chordState.getNextNodeForKey(entry.getKey()).getListenerPort(), Integer.toString(entry.getKey()),
                        null, fractalIds, 1, null, fractalIdMapping, AppConfig.myServentInfo.getUuid());
                MessageUtil.sendMessage(jobMessage);
            }
        }

        String firstFractalId = oldFractalIds.get(diedId);
        for (Map.Entry<String, String> entry : fractalIdMapping.entrySet()) {
            if (entry.getKey().equals(firstFractalId)) {
                Integer key = Utils.getKeyByValue(fractalIds, entry.getValue());
                JobMigrationMessage jobMigrationMessage = new JobMigrationMessage(
                        AppConfig.myServentInfo.getListenerPort(),
                        AppConfig.chordState.getNextNodeForKey(key).getListenerPort(),
                        Integer.toString(key), AppConfig.backupSuccessor);
                MessageUtil.sendMessage(jobMigrationMessage);
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
//        int nodeCount = AppConfig.chordState.getNodeCount();
//        int nodeCount = 10;
        if (nodeCount < job.getN()) {
            int receiverId = AppConfig.chordState.getAllNodeInfoHelper().get(lastAssigned).getUuid();
            fractalIds.put(receiverId, job.getName() + "0");
            baseFractalLevel = 1;
            baseFractalLevelsByJob.add(baseFractalLevel);
            overflowLevelNodes = 0;
            overflowLevelNodesByJob.add(overflowLevelNodes);
            return;
        }

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
            int receiverId = AppConfig.chordState.getAllNodeInfoHelper().get(left).getUuid();
            fractalIds.put(receiverId, prefix);
        } else {
            int incrementLevel = (int)Math.pow(dots, (int)((right - left + 1) / dots - 1));
            int suffix = 0;
            for (int i = left; i <= right - incrementLevel + 1; i += incrementLevel) {
                assign(prefix + suffix, i, i + incrementLevel - 1, dots);
                suffix++;
            }
        }
    }

    private static void contactBootstrap(int listenerPort) {
        int bsPort = AppConfig.BOOTSTRAP_PORT;
        String bsIp = AppConfig.BOOTSTRAP_IP;

        try {
            Socket bsSocket = new Socket(bsIp, bsPort);

            PrintWriter bsWriter = new PrintWriter(bsSocket.getOutputStream());
            bsWriter.write("Left\n" + listenerPort + "\n");
            bsWriter.flush();

            bsSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

//    public static void main(String[] args) {
//        Job job = new Job("x", 3, 0.5, 800, 800, new ArrayList<>());
//        start(job);
//        System.out.println(fractalIds.toString());
//        Job job2 = new Job("y", 3, 0.5, 800, 800, new ArrayList<>());
//        start(job2);
//        System.out.println(fractalIds.toString());
//        stop(job2);
//        System.out.println(fractalIds.toString());
//    }

}
