package com.khanyi.java.gautenglogistics.routing;

import com.khanyi.java.gautenglogistics.geo.Coordinate;

import java.util.Arrays;
import java.util.List;

/**
 * The shape of a route with the time a driver reaches each point on it, so you can ask where
 * they are a number of seconds after they set off.
 */
public final class TimedPath {

    private final List<Coordinate> points;
    private final double[] timesS; // seconds after setting off that each point is reached, smallest first

    public TimedPath(List<Coordinate> points, List<Double> timesS) {
        if (points.isEmpty() || points.size() != timesS.size()) {
            throw new IllegalArgumentException("A path needs at least one point, and a time for every point");
        }
        this.points = List.copyOf(points);
        this.timesS = timesS.stream().mapToDouble(Double::doubleValue).toArray();
    }

    /** How long the whole drive takes, in seconds. */
    public double durationS() {
        return timesS[timesS.length - 1];
    }

    /**
     * Where the driver is the given number of seconds after setting off. Before they leave it's
     * the start, after they arrive it's the end, and in between it's on the straight line between
     * the last point they passed and the next one.
     */
    public Coordinate positionAt(double timeS) {
        if (timeS <= timesS[0]) {
            return points.getFirst();
        }
        if (timeS >= durationS()) {
            return points.getLast();
        }
        // When the time isn't found exactly, binarySearch returns -(where it would go) - 1
        int found = Arrays.binarySearch(timesS, timeS);
        if (found >= 0) {
            return points.get(found);
        }
        int next = -found - 1;
        int previous = next - 1;
        double fraction = (timeS - timesS[previous]) / (timesS[next] - timesS[previous]);
        Coordinate a = points.get(previous);
        Coordinate b = points.get(next);
        return new Coordinate(a.lat() + (b.lat() - a.lat()) * fraction, a.lon() + (b.lon() - a.lon()) * fraction);
    }
}
