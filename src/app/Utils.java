package app;

import java.awt.*;
import java.util.*;
import java.util.List;

public class Utils {

    public static  <T, E> T getKeyByValue(Map<T, E> map, E value) {
        for (Map.Entry<T, E> entry : map.entrySet()) {
            if (Objects.equals(value, entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }

    public static  <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map) {
        List<Map.Entry<K, V>> list = new ArrayList<>(map.entrySet());
        list.sort(Map.Entry.comparingByValue());

        Map<K, V> result = new LinkedHashMap<>();
        for (Map.Entry<K, V> entry : list) {
            result.put(entry.getKey(), entry.getValue());
        }

        return result;
    }

    public static List<Point> inPolygon(Job job, List<Point> allPoints) {
        List<Point> toReturn = new ArrayList<>();

        int[] xPoints = new int[job.getN()];
        int[] yPoints = new int[job.getN()];

        int i = 0;
        for (Point point : job.getPoints()) {
            xPoints[i] = (int) point.getX();
            yPoints[i++] = (int) point.getY();
        }

        Polygon polygon = new Polygon(xPoints, yPoints, xPoints.length);
        for (Point point : allPoints) {
            if (polygon.contains(point.getX(), point.getY())) {
                toReturn.add(point);
            }
        }

        return toReturn;
    }

}
