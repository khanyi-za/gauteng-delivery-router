package com.khanyi.java.gautenglogistics.routing;


import com.khanyi.java.gautenglogistics.geo.Coordinate;
import com.khanyi.java.gautenglogistics.geo.Haversine;
import com.khanyi.java.gautenglogistics.geo.PolylineDecoder;
import com.khanyi.java.gautenglogistics.graph.Edge;
import com.khanyi.java.gautenglogistics.graph.EdgeGeometryLoader;
import com.khanyi.java.gautenglogistics.graph.GridIndex;
import com.khanyi.java.gautenglogistics.graph.RoadGraph;
import com.khanyi.java.gautenglogistics.graph.RoadNode;
import com.khanyi.java.gautenglogistics.graph.Snap;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Plans the fastest route between two GPS points. */
@Service
public class RouteService {

    private final RoadGraph graph;
    private final GridIndex gridIndex;
    private final EdgeGeometryLoader geometryLoader;

    public RouteService(RoadGraph graph, GridIndex gridIndex, EdgeGeometryLoader geometryLoader) {
        this.graph = graph;
        this.gridIndex = gridIndex;
        this.geometryLoader = geometryLoader;
    }

    public RoutePlan plan(Coordinate from, Coordinate to) {
        Snap start = snap(from);
        Snap end = snap(to);
        Route route = fastestRoute(start, end);
        return new RoutePlan(start, end, route, roadShape(start.node(), route));
    }

    /**
     * The fastest route between two GPS points, with the time a driver reaches each point on it.
     * Each road piece's driving time is shared out along its shape by distance.
     */
    public TimedPath timedPath(Coordinate from, Coordinate to) {
        Snap start = snap(from);
        Route route = fastestRoute(start, snap(to));
        Map<Long, List<Coordinate>> shapes = shapes(route);

        List<Coordinate> points = new ArrayList<>(List.of(start.node().location()));
        List<Double> timesS = new ArrayList<>(List.of(0.0));
        double pieceStartS = 0; // when the driver starts the current road piece
        for (Edge edge : route.edges()) {
            List<Coordinate> shape = shapes.get(edge.id());
            double pieceLengthM = lengthM(shape);
            double alongM = 0;
            for (int i = 1; i < shape.size(); i++) {
                alongM += Haversine.distanceMetres(shape.get(i - 1), shape.get(i));
                double share = pieceLengthM == 0 ? 1 : alongM / pieceLengthM;
                points.add(shape.get(i));
                timesS.add(pieceStartS + edge.travelTimeS() * share);
            }
            pieceStartS += edge.travelTimeS();
        }
        return new TimedPath(points, timesS);
    }

    /**
     * Joins the real shapes of the route's road pieces into one line, from start to end.
     * Store selection uses it too, to draw the delivery route.
     */
    List<Coordinate> roadShape(RoadNode start, Route route) {
        Map<Long, List<Coordinate>> shapes = shapes(route);

        List<Coordinate> path = new ArrayList<>();
        path.add(start.location());
        for (Edge edge : route.edges()) {
            List<Coordinate> points = shapes.get(edge.id());
            // Each piece starts where the one before it ended, so skip its first point
            path.addAll(points.subList(1, points.size()));
        }
        return path;
    }

    private Snap snap(Coordinate point) {
        return gridIndex.nearest(point).orElseThrow(() -> new NoRoadNearbyException(point));
    }

    private Route fastestRoute(Snap start, Snap end) {
        // Can't happen on a graph where every intersection can reach every other, so it's a bug if it does
        return Dijkstra.fastestRoute(graph, start.node().id(), end.node().id())
                .orElseThrow(() -> new IllegalStateException(
                        "No route between intersections " + start.node().id() + " and " + end.node().id()));
    }

    /** The real shape of each of the route's road pieces, by road piece id. */
    private Map<Long, List<Coordinate>> shapes(Route route) {
        Map<Long, String> geometries = geometryLoader.load(route.edges().stream().map(Edge::id).toList());

        Map<Long, List<Coordinate>> shapes = new HashMap<>();
        for (Edge edge : route.edges()) {
            String geometry = geometries.get(edge.id());
            if (geometry == null) {
                throw new IllegalStateException("No shape saved for road piece " + edge.id()
                        + ". Was the map imported again without restarting the app?");
            }
            shapes.put(edge.id(), PolylineDecoder.decode(geometry));
        }
        return shapes;
    }

    private static double lengthM(List<Coordinate> shape) {
        double lengthM = 0;
        for (int i = 1; i < shape.size(); i++) {
            lengthM += Haversine.distanceMetres(shape.get(i - 1), shape.get(i));
        }
        return lengthM;
    }
}