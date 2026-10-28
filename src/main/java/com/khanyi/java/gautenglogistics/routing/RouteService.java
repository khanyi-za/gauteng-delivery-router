package com.khanyi.java.gautenglogistics.routing;


import com.khanyi.java.gautenglogistics.geo.Coordinate;
import com.khanyi.java.gautenglogistics.geo.PolylineDecoder;
import com.khanyi.java.gautenglogistics.graph.Edge;
import com.khanyi.java.gautenglogistics.graph.EdgeGeometryLoader;
import com.khanyi.java.gautenglogistics.graph.GridIndex;
import com.khanyi.java.gautenglogistics.graph.RoadGraph;
import com.khanyi.java.gautenglogistics.graph.Snap;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
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
        Snap start = gridIndex.nearest(from).orElseThrow(() -> new NoRoadNearbyException(from));
        Snap end = gridIndex.nearest(to).orElseThrow(() -> new NoRoadNearbyException(to));

        // Can't happen on a graph where every intersection can reach every other, so it's a bug if it does
        Route route = Dijkstra.fastestRoute(graph, start.node().id(), end.node().id())
                .orElseThrow(() -> new IllegalStateException(
                        "No route between intersections " + start.node().id() + " and " + end.node().id()));

        return new RoutePlan(start, end, route, roadShape(start, route));
    }

    /** Joins the real shapes of the route's road pieces into one line, from start to end. */
    private List<Coordinate> roadShape(Snap start, Route route) {
        Map<Long, String> geometries = geometryLoader.load(route.edges().stream().map(Edge::id).toList());

        List<Coordinate> path = new ArrayList<>();
        path.add(start.node().location());
        for (Edge edge : route.edges()) {
            String geometry = geometries.get(edge.id());
            if (geometry == null) {
                throw new IllegalStateException("No shape saved for road piece " + edge.id()
                        + ". Was the map imported again without restarting the app?");
            }
            List<Coordinate> points = PolylineDecoder.decode(geometry);
            // Each piece starts where the one before it ended, so skip its first point
            path.addAll(points.subList(1, points.size()));
        }
        return path;
    }
}
