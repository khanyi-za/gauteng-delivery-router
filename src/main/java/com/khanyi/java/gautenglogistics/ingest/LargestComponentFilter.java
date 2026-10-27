package com.khanyi.java.gautenglogistics.ingest;

import com.khanyi.java.gautenglogistics.graph.Kosaraju;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Keeps only the biggest part of the road map where you can drive from any intersection to any
 * other and back. Cut-off pieces (a car park whose exit is outside the map, a one-way street
 * entered the wrong way round in the map data) are removed, so a delivery never gets "no route".
 */
public final class LargestComponentFilter {

    private LargestComponentFilter() {
    }

    public static List<RawEdge> keepLargestComponent(List<RawEdge> edges) {
        // Give each intersection a number 0, 1, 2, ... so we can use arrays instead of maps
        Map<Long, Integer> index = new HashMap<>();
        int[] from = new int[edges.size()];
        int[] to = new int[edges.size()];
        for (int i = 0; i < edges.size(); i++) {
            from[i] = index.computeIfAbsent(edges.get(i).fromNode(), id -> index.size());
            to[i] = index.computeIfAbsent(edges.get(i).toNode(), id -> index.size());
        }

        int[] components = Kosaraju.components(index.size(), from, to);
        int largest = Kosaraju.largest(components);

        // Keep a road piece only if both of its ends are in the biggest group
        List<RawEdge> kept = new ArrayList<>();
        for (int i = 0; i < edges.size(); i++) {
            if (components[from[i]] == largest && components[to[i]] == largest) {
                kept.add(edges.get(i));
            }
        }
        return kept;
    }
}
