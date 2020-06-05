package cli.command;

import app.AppConfig;
import app.Job;
import app.JobCommandHandler;
import app.Point;
import mutex.LogicalTimestamp;
import servent.message.MutexReleaseMessage;
import servent.message.MutexRequestMessage;
import servent.message.util.MessageUtil;

import java.util.ArrayList;
import java.util.InputMismatchException;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;

public class StartCommand implements CLICommand {

    private final Scanner sc;

    public StartCommand(Scanner sc) {
        this.sc = sc;
    }

    @Override
    public String commandName() {
        return "start";
    }

    @Override
    public void execute(String args) {
        if (args != null) {
            Job job = AppConfig.jobs.stream().filter(job1 -> job1.getName().equals(args)).findFirst().get();
            if (job == null) {
                AppConfig.timestampedErrorPrint("No such job in configuration file!");
                return;
            }

            if (AppConfig.activeJobs.contains(job)) {
                AppConfig.timestampedErrorPrint("Job is already active!");
                return;
            }

            if (AppConfig.activeJobs.size() == AppConfig.chordState.getNodeCount()) {
                AppConfig.timestampedErrorPrint("Too many jobs aleready started!");
                return;
            }

            try {
                AppConfig.localSemaphore.acquire();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            AppConfig.lamportClock.tick();
            LogicalTimestamp myRequestLogicalTimestamp = new LogicalTimestamp(AppConfig.lamportClock.getValue(),
                    AppConfig.myServentInfo.getUuid());

            AppConfig.replyLatch = new CountDownLatch(AppConfig.chordState.getNodeCount() - 1);

            if (AppConfig.chordState.getNodeCount() > 1) {
                MutexRequestMessage mutexRequestMessage = new MutexRequestMessage(AppConfig.myServentInfo.getListenerPort(),
                        AppConfig.chordState.getNextNodePort(), Integer.toString(AppConfig.myServentInfo.getUuid()),
                        myRequestLogicalTimestamp);
                mutexRequestMessage.setSenderIp(AppConfig.myServentInfo.getIpAddress());
                mutexRequestMessage.setReceiverIp(AppConfig.chordState.getNextNodeIp());
                MessageUtil.sendMessage(mutexRequestMessage);
            }

            AppConfig.requestQueue.add(myRequestLogicalTimestamp);

            try {
                AppConfig.replyLatch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            while (!AppConfig.requestQueue.peek().equals(myRequestLogicalTimestamp)) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            AppConfig.isDesignated = false;

            AppConfig.jobLatch = new CountDownLatch(AppConfig.chordState.getNodeCount());
            JobCommandHandler.start(job);

            try {
                AppConfig.jobLatch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            AppConfig.lamportClock.tick();
            AppConfig.requestQueue.poll();
            MutexReleaseMessage mutexReleaseMessage = new MutexReleaseMessage(AppConfig.myServentInfo.getListenerPort(),
                    AppConfig.chordState.getNextNodePort(), Integer.toString(AppConfig.myServentInfo.getUuid()),
                    new LogicalTimestamp(AppConfig.lamportClock.getValue(), AppConfig.myServentInfo.getUuid()), false);
            mutexReleaseMessage.setSenderIp(AppConfig.myServentInfo.getIpAddress());
            mutexReleaseMessage.setReceiverIp(AppConfig.chordState.getNextNodeIp());
            MessageUtil.sendMessage(mutexReleaseMessage);

            AppConfig.localSemaphore.release();
        } else {
            if (AppConfig.activeJobs.size() == AppConfig.chordState.getNodeCount()) {
                AppConfig.timestampedErrorPrint("Too many jobs aleready started!");
                return;
            }

            Job newJob = null;
            try {
                System.out.println("Please enter job name: ");
                String jobName = sc.nextLine();
                System.out.println("Please enter number of points of fractal structure: ");
                int n = sc.nextInt();
                System.out.println("Please enter proportion for generating new points: ");
                double p = sc.nextDouble();
                System.out.println("Please enter a width for the picture: ");
                int width = sc.nextInt();
                System.out.println("Please enter a height for the picture: ");
                int height = sc.nextInt();
                System.out.println("Please enter the endpoints of the fractal structure: ");
                ArrayList<Point> points = new ArrayList<>();
                sc.nextLine();
                String pointsString = sc.nextLine();
                String[] splitPoints = pointsString.split(",");
                for (int j = 0; j < splitPoints.length; j += 2) {
                    points.add(new Point(Integer.parseInt(splitPoints[j]), Integer.parseInt(splitPoints[j + 1])));
                }

                newJob = new Job(jobName, n, p, width, height, points);
            } catch (InputMismatchException | NumberFormatException e) {
                AppConfig.timestampedErrorPrint("Wrong type!");
                return;
            }


            if (AppConfig.activeJobs.contains(newJob)) {
                AppConfig.timestampedErrorPrint("Job is already active!");
            } else {
                try {
                    AppConfig.localSemaphore.acquire();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                AppConfig.lamportClock.tick();
                LogicalTimestamp myRequestLogicalTimestamp = new LogicalTimestamp(AppConfig.lamportClock.getValue(),
                        AppConfig.myServentInfo.getUuid());

                AppConfig.replyLatch = new CountDownLatch(AppConfig.chordState.getNodeCount() - 1);

                if (AppConfig.chordState.getNodeCount() > 1) {
                    MutexRequestMessage mutexRequestMessage = new MutexRequestMessage(AppConfig.myServentInfo.getListenerPort(),
                            AppConfig.chordState.getNextNodePort(), Integer.toString(AppConfig.myServentInfo.getUuid()),
                            myRequestLogicalTimestamp);
                    mutexRequestMessage.setSenderIp(AppConfig.myServentInfo.getIpAddress());
                    mutexRequestMessage.setReceiverIp(AppConfig.chordState.getNextNodeIp());
                    MessageUtil.sendMessage(mutexRequestMessage);
                }

                AppConfig.requestQueue.add(myRequestLogicalTimestamp);

                try {
                    AppConfig.replyLatch.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                while (!AppConfig.requestQueue.peek().equals(myRequestLogicalTimestamp)) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                AppConfig.isDesignated = false;

                AppConfig.jobLatch = new CountDownLatch(AppConfig.chordState.getNodeCount());
                JobCommandHandler.start(newJob);

                try {
                    AppConfig.jobLatch.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                AppConfig.lamportClock.tick();
                AppConfig.requestQueue.poll();
                MutexReleaseMessage mutexReleaseMessage = new MutexReleaseMessage(AppConfig.myServentInfo.getListenerPort(),
                        AppConfig.chordState.getNextNodePort(), Integer.toString(AppConfig.myServentInfo.getUuid()),
                        new LogicalTimestamp(AppConfig.lamportClock.getValue(), AppConfig.myServentInfo.getUuid()), false);
                mutexReleaseMessage.setSenderIp(AppConfig.myServentInfo.getIpAddress());
                mutexReleaseMessage.setReceiverIp(AppConfig.chordState.getNextNodeIp());
                MessageUtil.sendMessage(mutexReleaseMessage);

                AppConfig.localSemaphore.release();
            }
        }
    }
}