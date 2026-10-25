import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * Implement the six methods and add your route-reconstruction helper.
 * Input extraction is provided; all algorithm state belongs to the current request.
 * See ../../README.md for contracts and Java guidance.
 */
public final class Algorithms {
    private static int find(int[] parent, int x) {
        int root = x;
        while (parent[root] != root) root = parent[root];
        while (parent[x] != root) {
            int next = parent[x];
            parent[x] = root;
            x = next;
        }
        return root;
    }

    /**
     * DSU with path compression and union by size/rank; return states.
     */
    static Map<String, Object> dsu(Map<String, Object> input) {
        int n = Input.vertexCount(input);
        int[][] operations = Input.operations(input);

        int[] parent = new int[n];
        int[] size = new int[n];
        for (int i = 0; i < n; i++) {
            parent[i] = i;
            size[i] = 1;
        }
        int components = n;
        int largest = n > 0 ? 1 : 0;

        int[][] states = new int[operations.length][2];
        for (int i = 0; i < operations.length; i++) {
            int rootA = find(parent, operations[i][0]);
            int rootB = find(parent, operations[i][1]);
            if (rootA != rootB) {
                if (size[rootA] < size[rootB]) {
                    int tmp = rootA;
                    rootA = rootB;
                    rootB = tmp;
                }
                parent[rootB] = rootA;
                size[rootA] += size[rootB];
                components--;
                if (size[rootA] > largest) largest = size[rootA];
            }
            states[i][0] = components;
            states[i][1] = largest;
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("states", states);
        return result;
    }

    /**
     * Separate Kruskal implementation; return cost, edge_ids, components.
     */
    static Map<String, Object> kruskal(Map<String, Object> input) {
        int n = Input.vertexCount(input);
        int[][] edges = Input.edges(input);
        int m = edges.length;

        Integer[] order = new Integer[m];
        for (int i = 0; i < m; i++) order[i] = i;
        Arrays.sort(order, (a, b) -> Integer.compare(edges[a][2], edges[b][2]));

        int[] parent = new int[n];
        int[] size = new int[n];
        for (int i = 0; i < n; i++) {
            parent[i] = i;
            size[i] = 1;
        }

        long cost = 0;
        int[] chosen = new int[Math.max(n - 1, 0)];
        int count = 0;

        for (int idx = 0; idx < m; idx++) {
            int id = order[idx];
            int rootA = find(parent, edges[id][0]);
            int rootB = find(parent, edges[id][1]);
            if (rootA == rootB) continue;

            if (size[rootA] < size[rootB]) {
                int tmp = rootA;
                rootA = rootB;
                rootB = tmp;
            }
            parent[rootB] = rootA;
            size[rootA] += size[rootB];

            cost += edges[id][2];
            chosen[count++] = id;
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("cost", cost);
        result.put("edge_ids", Arrays.copyOf(chosen, count));
        result.put("components", n - count);
        return result;
    }

    /**
     * Separate Prim implementation; restart for every unvisited component.
     */
    static Map<String, Object> prim(Map<String, Object> input) {
        int n = Input.vertexCount(input);
        int[][] edges = Input.edges(input);
        int m = edges.length;

        int[] head = new int[n];
        Arrays.fill(head, -1);
        int[] next = new int[2 * m];
        int[] to = new int[2 * m];
        int[] eid = new int[2 * m];
        int cnt = 0;
        for (int i = 0; i < m; i++) {
            int u = edges[i][0];
            int v = edges[i][1];
            if (u == v) continue;
            to[cnt] = v;
            eid[cnt] = i;
            next[cnt] = head[u];
            head[u] = cnt++;
            to[cnt] = u;
            eid[cnt] = i;
            next[cnt] = head[v];
            head[v] = cnt++;
        }

        boolean[] visited = new boolean[n];
        PriorityQueue<long[]> pq = new PriorityQueue<>((a, b) -> Long.compare(a[0], b[0]));

        long cost = 0;
        int[] chosen = new int[Math.max(n - 1, 0)];
        int count = 0;
        int components = 0;

        for (int s = 0; s < n; s++) {
            if (visited[s]) continue;
            components++;
            visited[s] = true;
            for (int e = head[s]; e != -1; e = next[e]) {
                if (!visited[to[e]]) {
                    pq.add(new long[]{edges[eid[e]][2], eid[e], to[e]});
                }
            }
            while (!pq.isEmpty()) {
                long[] top = pq.poll();
                int v = (int) top[2];
                if (visited[v]) continue;
                visited[v] = true;
                cost += top[0];
                chosen[count++] = (int) top[1];
                for (int e = head[v]; e != -1; e = next[e]) {
                    if (!visited[to[e]]) {
                        pq.add(new long[]{edges[eid[e]][2], eid[e], to[e]});
                    }
                }
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("cost", cost);
        result.put("edge_ids", Arrays.copyOf(chosen, count));
        result.put("components", components);
        return result;
    }

    /**
     * Kosaraju's two DFS passes; return component and exact condensation.
     */
    static Map<String, Object> scc(Map<String, Object> input) {
        int n = Input.vertexCount(input);
        int[][] edges = Input.edges(input);
        int m = edges.length;

        int[] head = new int[n];
        int[] rhead = new int[n];
        Arrays.fill(head, -1);
        Arrays.fill(rhead, -1);
        int[] next = new int[m];
        int[] rnext = new int[m];
        for (int i = 0; i < m; i++) {
            int u = edges[i][0];
            int v = edges[i][1];
            next[i] = head[u];
            head[u] = i;
            rnext[i] = rhead[v];
            rhead[v] = i;
        }

        boolean[] seen = new boolean[n];
        int[] ptr = head.clone();
        int[] stack = new int[n];
        int[] order = new int[n];
        int oc = 0;
        for (int s = 0; s < n; s++) {
            if (seen[s]) continue;
            seen[s] = true;
            int sp = 0;
            stack[sp++] = s;
            while (sp > 0) {
                int u = stack[sp - 1];
                int e = ptr[u];
                if (e != -1) {
                    ptr[u] = next[e];
                    int v = edges[e][1];
                    if (!seen[v]) {
                        seen[v] = true;
                        stack[sp++] = v;
                    }
                } else {
                    sp--;
                    order[oc++] = u;
                }
            }
        }

        int[] comp = new int[n];
        Arrays.fill(comp, -1);
        int c = 0;
        for (int i = n - 1; i >= 0; i--) {
            int s = order[i];
            if (comp[s] != -1) continue;
            comp[s] = c;
            int sp = 0;
            stack[sp++] = s;
            while (sp > 0) {
                int u = stack[--sp];
                for (int e = rhead[u]; e != -1; e = rnext[e]) {
                    int v = edges[e][0];
                    if (comp[v] == -1) {
                        comp[v] = c;
                        stack[sp++] = v;
                    }
                }
            }
            c++;
        }

        HashSet<Long> seenPairs = new HashSet<>();
        List<int[]> cond = new ArrayList<>();
        for (int i = 0; i < m; i++) {
            int a = comp[edges[i][0]];
            int b = comp[edges[i][1]];
            if (a == b) continue;
            long key = (long) a * c + b;
            if (seenPairs.add(key)) cond.add(new int[]{a, b});
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("component", comp);
        result.put("condensation", cond.toArray(new int[0][]));
        return result;
    }

    private static int[] topoOrder(int n, int[][] edges) {
        int m = edges.length;
        int[] head = new int[n];
        Arrays.fill(head, -1);
        int[] next = new int[m];
        for (int i = 0; i < m; i++) {
            next[i] = head[edges[i][0]];
            head[edges[i][0]] = i;
        }

        int[] color = new int[n];
        int[] ptr = head.clone();
        int[] stack = new int[n];
        int[] post = new int[n];
        int pc = 0;

        for (int s = 0; s < n; s++) {
            if (color[s] != 0) continue;
            int sp = 0;
            stack[sp++] = s;
            color[s] = 1;
            while (sp > 0) {
                int u = stack[sp - 1];
                int e = ptr[u];
                if (e != -1) {
                    ptr[u] = next[e];
                    int v = edges[e][1];
                    if (color[v] == 1) return null;
                    if (color[v] == 0) {
                        color[v] = 1;
                        stack[sp++] = v;
                    }
                } else {
                    color[u] = 2;
                    sp--;
                    post[pc++] = u;
                }
            }
        }

        int[] order = new int[n];
        for (int i = 0; i < n; i++) order[i] = post[n - 1 - i];
        return order;
    }

    /**
     * Three-state DFS and reverse finish order; return acyclic and order.
     */
    static Map<String, Object> topo(Map<String, Object> input) {
        int n = Input.vertexCount(input);
        int[][] edges = Input.edges(input);

        int[] order = topoOrder(n, edges);

        Map<String, Object> result = new LinkedHashMap<>();
        if (order == null) {
            result.put("acyclic", false);
            result.put("order", new int[0]);
        } else {
            result.put("acyclic", true);
            result.put("order", order);
        }
        return result;
    }

    /**
     * Whole-graph cycle check and DAG relaxation; return acyclic, distance, parent_edge.
     */
    static Map<String, Object> dag(Map<String, Object> input) {
        int n = Input.vertexCount(input);
        int source = Input.source(input);
        int[][] edges = Input.edges(input);
        int m = edges.length;

        Map<String, Object> result = new LinkedHashMap<>();

        int[] order = topoOrder(n, edges);
        if (order == null) {
            result.put("acyclic", false);
            result.put("distance", new Long[0]);
            result.put("parent_edge", new int[0]);
            return result;
        }

        int[] head = new int[n];
        Arrays.fill(head, -1);
        int[] next = new int[m];
        for (int i = 0; i < m; i++) {
            next[i] = head[edges[i][0]];
            head[edges[i][0]] = i;
        }

        Long[] dist = new Long[n];
        int[] parentEdge = new int[n];
        Arrays.fill(parentEdge, -1);
        dist[source] = 0L;

        for (int idx = 0; idx < n; idx++) {
            int u = order[idx];
            if (dist[u] == null) continue;
            for (int e = head[u]; e != -1; e = next[e]) {
                int v = edges[e][1];
                long nd = dist[u] + edges[e][2];
                if (dist[v] == null || nd < dist[v]) {
                    dist[v] = nd;
                    parentEdge[v] = e;
                }
            }
        }

        result.put("acyclic", true);
        result.put("distance", dist);
        result.put("parent_edge", parentEdge);
        return result;
    }

    /**
     * Route-reconstruction helper (not a JSON task).
     * Returns the vertex sequence from source to target following parentEdge.
     * Route to the source itself is [source].
     * If target is unreachable, returns an EMPTY array (documented choice).
     */
    static int[] reconstructPath(int[] parentEdge, int[][] edges, int source, int target) {
        if (target == source) return new int[]{source};
        if (parentEdge[target] == -1) return new int[0];

        int[] buf = new int[parentEdge.length];
        int len = 0;
        int v = target;
        buf[len++] = v;
        while (v != source) {
            if (len >= buf.length) return new int[0];
            int e = parentEdge[v];
            if (e == -1) return new int[0];
            v = edges[e][0];
            buf[len++] = v;
        }

        int[] path = new int[len];
        for (int i = 0; i < len; i++) path[i] = buf[len - 1 - i];
        return path;
    }
}
