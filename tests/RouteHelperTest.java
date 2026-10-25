import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RouteHelperTest {
    static int failures = 0;

    static Map<String, Object> request(int n, int source, int[][] edges) {
        List<List<Integer>> rows = new ArrayList<>();
        for (int[] e : edges) {
            List<Integer> row = new ArrayList<>();
            for (int x : e) row.add(x);
            rows.add(row);
        }
        Map<String, Object> m = new HashMap<>();
        m.put("n", n);
        m.put("source", source);
        m.put("edges", rows);
        return m;
    }

    static void check(String name, boolean ok) {
        System.out.println((ok ? "PASS " : "FAIL ") + name);
        if (!ok) failures++;
    }

    static long pathCost(int[] path, int[] parentEdge, int[][] edges) {
        long total = 0;
        for (int i = 1; i < path.length; i++) {
            int e = parentEdge[path[i]];
            if (edges[e][0] != path[i - 1] || edges[e][1] != path[i]) return Long.MIN_VALUE;
            total += edges[e][2];
        }
        return total;
    }

    public static void main(String[] args) {
        int[][] e1 = {{0, 1, 3}, {0, 2, 8}, {1, 2, -7}, {2, 3, 4}};
        Map<String, Object> r1 = Algorithms.dag(request(5, 0, e1));
        int[] pe1 = (int[]) r1.get("parent_edge");
        Long[] d1 = (Long[]) r1.get("distance");
        System.out.println("distance    = " + Arrays.toString(d1));
        System.out.println("parent_edge = " + Arrays.toString(pe1));

        int[] toThree = Algorithms.reconstructPath(pe1, e1, 0, 3);
        System.out.println("route 0->3  = " + Arrays.toString(toThree));
        check("reachable target 3: route is [0,1,2,3]", Arrays.equals(toThree, new int[]{0, 1, 2, 3}));
        check("reachable target 3: cost 0 equals distance", pathCost(toThree, pe1, e1) == 0 && d1[3] == 0);

        int[] toSource = Algorithms.reconstructPath(pe1, e1, 0, 0);
        System.out.println("route 0->0  = " + Arrays.toString(toSource));
        check("source: route is [0]", Arrays.equals(toSource, new int[]{0}));

        int[] toUnreach = Algorithms.reconstructPath(pe1, e1, 0, 4);
        System.out.println("route 0->4  = " + Arrays.toString(toUnreach));
        check("unreachable target 4: empty route, distance null", toUnreach.length == 0 && d1[4] == null);


        int[][] e2 = {{0, 1, 5}, {0, 1, 5}, {0, 2, 10}, {1, 2, 5}};
        Map<String, Object> r2 = Algorithms.dag(request(3, 0, e2));
        int[] pe2 = (int[]) r2.get("parent_edge");
        Long[] d2 = (Long[]) r2.get("distance");
        System.out.println("distance    = " + Arrays.toString(d2));
        System.out.println("parent_edge = " + Arrays.toString(pe2));

        int[] toTwo = Algorithms.reconstructPath(pe2, e2, 0, 2);
        System.out.println("route 0->2  = " + Arrays.toString(toTwo));
        check("tied routes: path starts at source, ends at target",
                toTwo[0] == 0 && toTwo[toTwo.length - 1] == 2);
        check("tied routes: edge directions valid and cost 10 equals distance",
                pathCost(toTwo, pe2, e2) == 10 && d2[2] == 10);

        int[] toOne = Algorithms.reconstructPath(pe2, e2, 0, 1);
        System.out.println("route 0->1  = " + Arrays.toString(toOne));
        check("parallel edges: route to 1 is [0,1], cost 5",
                Arrays.equals(toOne, new int[]{0, 1}) && pathCost(toOne, pe2, e2) == 5 && d2[1] == 5);

        System.out.println(failures == 0 ? "ALL PASSED" : failures + " FAILED");
        if (failures > 0) System.exit(1);
    }
}