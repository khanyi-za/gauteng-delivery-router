package com.khanyi.java.gautenglogistics.graph;

import java.util.Arrays;

/**
 * Splits a one-way-street map into groups (Kosaraju's algorithm). Two points are in the same
 * group when you can travel from each one to the other and back.
 *
 * <p>Points are numbered 0, 1, 2, ... and connection i goes from edgeFrom[i] to edgeTo[i].
 * The searches keep their own to-do list (a stack) instead of calling themselves again and again,
 * because on a big road map that would run out of memory and crash.
 */
public final class Kosaraju {

    private Kosaraju() {
    }

    /** Returns the group number of every point. Groups are numbered from 0. */
    public static int[] components(int vertexCount, int[] edgeFrom, int[] edgeTo) {
        Adjacency forward = Adjacency.of(vertexCount, edgeFrom, edgeTo);
        Adjacency reverse = Adjacency.of(vertexCount, edgeTo, edgeFrom);
        int[] finishOrder = finishOrder(forward);
        return labelComponents(reverse, finishOrder);
    }

    /** Returns the number of the biggest group (the one with the most points). */
    public static int largest(int[] components) {
        int[] sizes = new int[Arrays.stream(components).max().orElse(-1) + 1];
        int largest = -1;
        for (int component : components) {
            sizes[component]++;
            if (largest == -1 || sizes[component] > sizes[largest]) {
                largest = component;
            }
        }
        return largest;
    }

    /**
     * Step 1: explore the map, going as deep as possible down each road first. A point is
     * written down once everything you can reach from it has been explored.
     */
    private static int[] finishOrder(Adjacency graph) {
        int n = graph.vertexCount();
        boolean[] visited = new boolean[n];
        int[] nextEdge = Arrays.copyOf(graph.offsets(), n); // which road to try next from each point
        int[] stack = new int[n];
        int[] order = new int[n];
        int finished = 0;

        for (int start = 0; start < n; start++) {
            if (visited[start]) {
                continue;
            }
            visited[start] = true;
            int top = 0;
            stack[top++] = start;
            while (top > 0) {
                int v = stack[top - 1];
                if (nextEdge[v] < graph.offsets()[v + 1]) {
                    int w = graph.targets()[nextEdge[v]++];
                    if (!visited[w]) {
                        visited[w] = true;
                        stack[top++] = w;
                    }
                } else {
                    top--;
                    order[finished++] = v;
                }
            }
        }
        return order;
    }

    /**
     * Step 2: explore the map with every road turned around, starting from the point that was
     * written down last. Everything one exploration reaches (that has no group yet) is one group.
     */
    private static int[] labelComponents(Adjacency reverse, int[] finishOrder) {
        int n = reverse.vertexCount();
        int[] component = new int[n];
        Arrays.fill(component, -1);
        int[] stack = new int[n];
        int count = 0;

        for (int i = n - 1; i >= 0; i--) {
            int start = finishOrder[i];
            if (component[start] != -1) {
                continue;
            }
            component[start] = count;
            int top = 0;
            stack[top++] = start;
            while (top > 0) {
                int v = stack[--top];
                for (int e = reverse.offsets()[v]; e < reverse.offsets()[v + 1]; e++) {
                    int w = reverse.targets()[e];
                    if (component[w] == -1) {
                        component[w] = count;
                        stack[top++] = w;
                    }
                }
            }
            count++;
        }
        return component;
    }

    /**
     * Which points each point connects to, packed into two arrays: the neighbours of point v
     * are targets[offsets[v]] up to targets[offsets[v + 1] - 1]. Much smaller than a list per point.
     */
    private record Adjacency(int[] offsets, int[] targets) {

        static Adjacency of(int vertexCount, int[] from, int[] to) {
            int[] offsets = new int[vertexCount + 1];
            for (int v : from) {
                offsets[v + 1]++;
            }
            for (int v = 0; v < vertexCount; v++) {
                offsets[v + 1] += offsets[v];
            }
            int[] targets = new int[from.length];
            int[] position = Arrays.copyOf(offsets, vertexCount);
            for (int i = 0; i < from.length; i++) {
                targets[position[from[i]]++] = to[i];
            }
            return new Adjacency(offsets, targets);
        }

        int vertexCount() {
            return offsets.length - 1;
        }
    }
}


