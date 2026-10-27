package com.khanyi.java.gautenglogistics.graph;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class KosarajuTest {

    /**
     * <pre>
     *   0 -> 1 -> 2 -> 0      a loop: one group
     *             2 -> 3      3 can't get back: a group on its own
     *   4 <-> 5               a two-way road on its own: one group
     * </pre>
     */
    @Test
    void groupsVerticesThatCanReachEachOther() {
        int[] from = {0, 1, 2, 2, 4, 5};
        int[] to = {1, 2, 0, 3, 5, 4};

        int[] components = Kosaraju.components(6, from, to);

        assertThat(components[1]).isEqualTo(components[0]);
        assertThat(components[2]).isEqualTo(components[0]);
        assertThat(components[3]).isNotEqualTo(components[0]);
        assertThat(components[5]).isEqualTo(components[4]);
        assertThat(components[4]).isNotIn(components[0], components[3]);
    }

    @Test
    void aOneWayChainIsAllSeparateComponents() {
        int[] components = Kosaraju.components(3, new int[]{0, 1}, new int[]{1, 2});

        assertThat(components).doesNotHaveDuplicates();
    }

    @Test
    void largestPicksTheComponentWithTheMostVertices() {
        int[] components = Kosaraju.components(6,
                new int[]{0, 1, 2, 2, 4, 5},
                new int[]{1, 2, 0, 3, 5, 4});

        assertThat(Kosaraju.largest(components)).isEqualTo(components[0]);
    }

    @Test
    void aLongPathDoesNotOverflowTheStack() {
        // A two-way road through a million points: a method that calls itself would crash here
        int n = 1_000_000;
        int[] from = new int[2 * (n - 1)];
        int[] to = new int[2 * (n - 1)];
        for (int i = 0; i < n - 1; i++) {
            from[2 * i] = i;
            to[2 * i] = i + 1;
            from[2 * i + 1] = i + 1;
            to[2 * i + 1] = i;
        }

        int[] components = Kosaraju.components(n, from, to);

        assertThat(components).containsOnly(0);
    }
}
