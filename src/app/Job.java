package app;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Objects;

public class Job implements Serializable, Comparable<Job> {

    private static final long serialVersionUID = 2704538302672546227L;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Job job = (Job) o;
        return Objects.equals(name, job.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public int compareTo(Job o) {
        return name.compareTo(o.name);
    }

    @Override
    public String toString() {
        return points.toString();
    }
}
