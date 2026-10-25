import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class Benchmark {
    static final long SEED = 12345L;
    static final int[] SIZES = {10_000, 50_000, 100_000};
    static final int WARMUP = 2;
    static final int RUNS = 5;

    static Map<String, Object> request(String task, int n, int[][] edges) {
        List<List<Integer>> rows = new ArrayList<>(edges.length);
        for (int[] e : edges) rows.add(Arrays.asList(e[0], e[1], e[2]));
        Map<String, Object> m = new HashMap<>();
        m.put("task", task);
        m.put("n", n);
        m.put("edges", rows);
        return m;
    }

    static int find(int[] p, int x) {
        while (p[x] != x) {
            p[x] = p[p[x]];
            x = p[x];
        }
        return x;
    }

    static String verify(int n, int[][] edges, Map<String, Object> out) {
        int[] ids = (int[]) out.get("edge_ids");
        int comps = (Integer) out.get("components");
        long cost = (Long) out.get("cost");

        int[] q = new int[n];
        for (int i = 0; i < n; i++) q[i] = i;
        int realComps = n;
        for (int[] e : edges) {
            int a = find(q, e[0]), b = find(q, e[1]);
            if (a != b) { q[a] = b; realComps--; }
        }
        if (comps != realComps) return "wrong component count";
        if (ids.length != n - comps) return "wrong number of edges";

        int[] p = new int[n];
        for (int i = 0; i < n; i++) p[i] = i;
        boolean[] used = new boolean[edges.length];
        long sum = 0;
        for (int id : ids) {
            if (used[id]) return "repeated edge id";
            used[id] = true;
            int a = find(p, edges[id][0]), b = find(p, edges[id][1]);
            if (a == b) return "cycle or self-loop selected";
            p[a] = b;
            sum += edges[id][2];
        }
        if (sum != cost) return "cost is not the sum of selected edges";
        return null;
    }

    static double median(double[] x) {
        double[] c = x.clone();
        Arrays.sort(c);
        return c[c.length / 2];
    }

    static long sink = 0;

    static double timeMs(String which, Map<String, Object> req) {
        long t0 = System.nanoTime();
        Map<String, Object> out = which.equals("kruskal") ? Algorithms.kruskal(req) : Algorithms.prim(req);
        long t1 = System.nanoTime();
        sink += (Long) out.get("cost");
        return (t1 - t0) / 1e6;
    }

    public static void main(String[] args) {
        System.out.println("Seed: " + SEED + ", warm-up runs: " + WARMUP + ", timed runs: " + RUNS);
        System.out.println("Timer: System.nanoTime. Measured: one call to Algorithms.kruskal / Algorithms.prim");
        System.out.println("(includes Input conversion and internal graph construction; excludes graph generation, JSON, I/O).");
        System.out.println();

        for (int n : SIZES) {
            int m = (int) (2.5 * n);
            Random rnd = new Random(SEED + n);
            int[][] edges = new int[m][3];
            for (int i = 0; i < m; i++) {
                edges[i][0] = rnd.nextInt(n);
                edges[i][1] = rnd.nextInt(n);
                edges[i][2] = rnd.nextInt(2_000_000_001) - 1_000_000_000;
            }
            Map<String, Object> kReq = request("kruskal", n, edges);
            Map<String, Object> pReq = request("prim", n, edges);

            Map<String, Object> kOut = Algorithms.kruskal(kReq);
            Map<String, Object> pOut = Algorithms.prim(pReq);
            String kErr = verify(n, edges, kOut);
            String pErr = verify(n, edges, pOut);
            boolean sameCost = ((Long) kOut.get("cost")).equals(pOut.get("cost"));
            boolean sameComps = kOut.get("components").equals(pOut.get("components"));
            System.out.println("n=" + n + " m=" + m + " cost=" + kOut.get("cost")
                    + " components=" + kOut.get("components"));
            System.out.println("  kruskal valid: " + (kErr == null ? "yes" : kErr)
                    + ", prim valid: " + (pErr == null ? "yes" : pErr)
                    + ", same cost: " + sameCost + ", same components: " + sameComps);
            if (kErr != null || pErr != null || !sameCost || !sameComps) {
                System.out.println("  CORRECTNESS FAILED, skipping timings");
                continue;
            }

            for (int i = 0; i < WARMUP; i++) { timeMs("kruskal", kReq); timeMs("prim", pReq); }
            double[] kt = new double[RUNS];
            double[] pt = new double[RUNS];
            for (int i = 0; i < RUNS; i++) kt[i] = timeMs("kruskal", kReq);
            for (int i = 0; i < RUNS; i++) pt[i] = timeMs("prim", pReq);

            StringBuilder ks = new StringBuilder(), ps = new StringBuilder();
            for (int i = 0; i < RUNS; i++) {
                ks.append(String.format("%.1f ", kt[i]));
                ps.append(String.format("%.1f ", pt[i]));
            }
            System.out.println("  kruskal ms: " + ks + "| median " + String.format("%.1f", median(kt)));
            System.out.println("  prim    ms: " + ps + "| median " + String.format("%.1f", median(pt)));
            System.out.println();
        }
        System.out.println("(checksum " + sink + ")");
    }
}