package com.khanyi.java.gautenglogistics.routing;


import com.khanyi.java.gautenglogistics.geo.Coordinate;
import com.khanyi.java.gautenglogistics.graph.Edge;
import com.khanyi.java.gautenglogistics.graph.GridIndex;
import com.khanyi.java.gautenglogistics.graph.RoadGraph;
import com.khanyi.java.gautenglogistics.graph.Snap;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/** Plans the fastest route between two GPS points. */
@Service
public class RouteService {

    private final RoadGraph graph;
    private final GridIndex gridIndex;

    public RouteService(RoadGraph graph, GridIndex gridIndex) {
        this.graph = graph;
        this.gridIndex = gridIndex;
    }

    public RoutePlan plan(Coordinate from, Coordinate to) {
        Snap start = gridIndex.nearest(from).orElseThrow(() -> new NoRoadNearbyException(from));
        Snap end = gridIndex.nearest(to).orElseThrow(() -> new NoRoadNearbyException(to));

        // Can't happen on a graph where every intersection can reach every other, so it's a bug if it does
        Route route = Dijkstra.fastestRoute(graph, start.node().id(), end.node().id())
                .orElseThrow(() -> new IllegalStateException(
                        "No route between intersections " + start.node().id() + " and " + end.node().id()));

        List<Coordinate> path = new ArrayList<>();
        path.add(start.node().location());
        for (Edge edge : route.edges()) {
            path.add(graph.node(edge.to()).location());
        }
        return new RoutePlan(start, end, route, path);
    }
}
