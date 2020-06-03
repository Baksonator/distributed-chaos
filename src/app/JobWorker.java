package app;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

public class JobWorker implements Runnable {

    private final Job job;
    private List<Point> results;
    private volatile boolean working;

    public JobWorker(Job job) {
        this.job = job;
        this.results = new CopyOnWriteArrayList<>();
        this.working = true;
    }

    public JobWorker(Job job, List<Point> newData) {
        this.job = job;
        this.results = new CopyOnWriteArrayList<>(newData);
        this.working = true;
    }

    @Override
    public void run() {
        ArrayList<Point> boundingPoints = job.getPoints();
        double p = job.getP();

        Random random = new Random(System.currentTimeMillis());

        Point currentPoint = null;
        if (results.size() == 0) {
            currentPoint = new Point(random.nextInt(job.getWidth()), random.nextInt(job.getHeight()));
            results.add(currentPoint);
            AppConfig.timestampedStandardPrint("Started new work");
        } else {
            currentPoint = results.get(results.size() - 1);
            AppConfig.timestampedStandardPrint("Started someone else's work");
        }

        while (working) {
            Point chosenPoint = boundingPoints.get(random.nextInt(job.getN()));
            Point newPoint = Point.pointOnP(chosenPoint, currentPoint, p);
            results.add(newPoint);
            currentPoint = newPoint;
//            if (results.size() % 5 == 0) {
//                AppConfig.timestampedStandardPrint(results.size()+" SIZE");
//            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public List<Point> getResults() {
        return results;
    }

    public Job getJob() {
        return job;
    }

    public boolean isWorking() {
        return working;
    }

    public void stop() {
        working = false;
    }
}
