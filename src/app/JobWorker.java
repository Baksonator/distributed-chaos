package app;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class JobWorker implements Runnable {

    private final Job job;
    private List<Point> results;
    private volatile boolean working;

    public JobWorker(Job job) {
        this.job = job;
        this.results = new ArrayList<>();
        this.working = true;
    }

    @Override
    public void run() {
        ArrayList<Point> boundingPoints = job.getPoints();
        double p = job.getP();

        AppConfig.timestampedStandardPrint("Started work");

        Random random = new Random(System.currentTimeMillis());

        Point currentPoint = new Point(random.nextInt(job.getWidth()), random.nextInt(job.getHeight()));
        results.add(currentPoint);

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

    public void stop() {
        working = false;
    }
}
