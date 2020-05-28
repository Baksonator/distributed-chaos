package app;

import java.util.ArrayList;

public class Job {

    private final String name;
    private final int n;
    private final double p;
    private final int width;
    private final int height;
    private final ArrayList<Point> points;

    public Job(String name, int n, double p, int width, int height, ArrayList<Point> points) {
        this.name = name;
        this.n = n;
        this.p = p;
        this.width = width;
        this.height = height;
        this.points = points;
    }

    public String getName() {
        return name;
    }

    public int getN() {
        return n;
    }

    public double getP() {
        return p;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public ArrayList<Point> getPoints() {
        return points;
    }

}
