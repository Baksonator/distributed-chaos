package app;

import java.io.Serializable;
import java.util.Objects;

public class Point implements Serializable {

    private final double x;
    private final double y;

    public Point(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public static Point pointOnP(Point first, Point second, double p) {
        // TODO Change this
        double newX = first.x + p * (second.x - first.x);
        double newY = first.y + p * (second.y - first.y);
        return new Point(newX, newY);
//        return new Point(p * (first.x + second.x), p * (first.y + second.y));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Point point = (Point) o;
        return x == point.x &&
                y == point.y;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }

    @Override
    public String toString() {
        return "[" + x + "|" + y + "]";
    }
}
