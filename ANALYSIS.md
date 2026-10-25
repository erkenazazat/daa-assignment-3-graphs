# Assignment 3 – Analysis

Language: Java (the code uses only features available in Java 17; it was run on Java 25.0.1).
All algorithm code is in `src/Java/Algorithms.java`. The supplied files `Main.java`, `Input.java`
and `Json.java` were not modified.

## 1. Build, run and test commands

Run from the repository root.

    python3 tools/run.py --setup
    python3 tools/run.py --check
    python3 tools/run.py --input tests/my_cases.jsonl | tee tests/actual_output.txt
    python3 tests/check_cases.py --selftest | tee tests/checker_selftest.txt
    python3 tests/check_cases.py tests/my_cases.jsonl tests/actual_output.txt | tee tests/check_result.txt
    python3 tests/gen_large.py
    time python3 tools/run.py --input tests/large_cases.jsonl > tests/large_output.txt
    python3 tests/check_cases.py tests/large_cases.jsonl tests/large_output.txt | tee tests/large_check_result.txt
    mkdir -p .build/tests
    javac -d .build/tests src/Java/*.java tests/RouteHelperTest.java
    java -cp .build/tests RouteHelperTest | tee tests/route_helper_output.txt
    javac -d .build/tests src/Java/*.java tests/Benchmark.java
    java -cp .build/tests Benchmark | tee tests/benchmark_output.txt

Public checks: `--check` reported PASS for all 36 public cases and for the "state reset" check
(two complete batches in one process) on my machine.
GitHub Actions “Public checks” passed in my repository

## 2. Method analysis

Notation: `n` vertices, `m` edges, `q` union operations. Adjacency lists are stored in arrays
(`head[v]` points to the first edge of `v`, `next[e]` to the following one), so no per-edge objects
are created and original edge IDs are kept as array positions.

### 2.1 DSU

**Steps.** `parent[i] = i` and `size[i] = 1` for every vertex. For each pair `[u, v]` find both roots.
If they differ, attach the smaller tree below the larger one, add the sizes, decrease the component
counter and update the largest size. Append `[components, largest]` after every operation
(also for repeated and self unions).

**Invariant.** The `parent` array is a forest in which every tree is exactly one connected component.
For every root `r`, `size[r]` is the number of vertices of its tree; `components` is the number of
roots; `largest` is the maximum root size seen so far.

**Correctness.** `find` returns the root, and path compression only redirects vertices to the root of
their own tree, so the partition is unchanged. A union changes the partition only when the roots
differ, and then exactly merges two components. Sizes are stored separately from any rank, so
`largest` is always a real vertex count. Component sizes only grow, so a running maximum is correct.

**Boundary cases.** `n = 0` gives `states = []` and `largest = 0`. A self-union `[3,3]` and a repeated
union have equal roots and change nothing, but a state is still recorded. With `n > 0` the initial
`largest` is 1.

**Complexity.** `O(n + q α(n))` time, `O(n + q)` memory (the states array has `q` entries).
`find` is iterative (two passes), so there is no recursion.

### 2.2 Kruskal

**Steps.** Sort the edge *indices* by weight (so original IDs are kept). Scan them in order; take an
edge when `find(u) != find(v)`, then union, add the weight to a `long` total and store the ID.
`components = n - (number of selected edges)`.

**Invariant.** After each processed edge, the selected edges are a subset of some minimum spanning
forest, and the DSU partition equals the connected components of the selected edges.

**Correctness.** An edge whose endpoints are already connected would close a cycle and is skipped.
An edge that connects two different trees is the lightest remaining edge leaving the component of `u`
(all lighter edges were already processed), so the cut property allows it. In a forest with `c`
components there are exactly `n - c` edges, which gives the component count.

**Boundary cases.** Self-loops are never selected (equal roots). Parallel edges: the lighter one
is processed first. Negative weights need no special handling. Isolated vertices count as components.
The total is a `long`; a test with five edges of weight 10^9 gives 5000000000 (> 2^32).
`n = 0` gives cost 0, no IDs, 0 components.

**Complexity.** `O(n + m log m)` time, `O(n + m)` memory. Sorting uses `Integer[]` with a comparator.

### 2.3 Prim

**Steps.** Build adjacency lists (self-loops are skipped). For every unvisited vertex `s`: count a new
component, mark `s` visited, push its edges into a priority queue ordered by weight (entries are
`{weight, edgeId, target}`). Repeatedly pop the lightest entry; if its target is already visited it is
a stale entry and is ignored; otherwise visit the target, add the weight and ID, and push the target's
edges that lead to unvisited vertices.

**Invariant.** Within one component, the visited vertices are connected by the selected edges, and
the selected edges are part of a minimum spanning tree of that component.

**Correctness.** The queue always contains every edge leaving the visited set (plus stale ones). The
lightest valid popped entry is the lightest edge crossing the cut (visited, unvisited), which the cut
property says is safe. The outer loop restarts from every unvisited vertex, so every component is
processed, and each restart increments `components`; isolated vertices form components of size one.

**Boundary cases.** Same as Kruskal (loops, parallel edges, negative weights, 64-bit totals, `n = 0`).

**Complexity.** Lazy heap: each half-edge is pushed at most once, so at most `2m` entries.
`O(n + m log m)` time, `O(n + m)` memory.

### 2.4 Strongly connected components (Kosaraju)

**Steps.** Build the graph and its transpose (arrays `head/next` and `rhead/rnext`, indexed by edge).
Pass 1: iterative DFS over the original graph; record vertices in order of finishing. Pass 2: iterate
vertices in decreasing finish order; each DFS on the transpose from an unlabelled vertex labels one
SCC. Condensation: for every edge with different labels, insert the pair `(a, b)` into a `HashSet`
(encoded as `a * c + b` in a `long`) and output each pair once.

**Invariant.** Pass 1: a vertex is recorded when all its outgoing edges were explored. Pass 2: a DFS
started from the unlabelled vertex with the largest finish time labels exactly one SCC.

**Correctness.** The standard property: if there is an edge from SCC `C` to a different SCC `C'`,
the largest finish time in `C` is greater than in `C'`. Hence the DFS in the transpose from the
highest-finishing unlabelled vertex can reach only its own SCC (edges leaving it in the original graph
point into already-labelled components, so they are missing in the transpose search). Labels are
therefore equal exactly inside an SCC.

**Boundary cases.** Isolated vertices are separate SCCs. Duplicate edges and self-loops are internal
or deduplicated and never produce condensation pairs. `n = 0` gives two empty arrays. A chain of
100000 vertices is handled without recursion.

**Complexity.** `O(n + m)` expected time (hash set), `O(n + m)` memory.

### 2.5 Topological order (DFS with three states)

**Steps.** Colours: 0 = unvisited, 1 = active (on the stack), 2 = finished. Iterative DFS from every
unvisited vertex. For an edge `u -> v`: colour 1 means a directed cycle (return "cyclic"); colour 0
means push `v`; colour 2 is ignored. When the edges of `u` are exhausted, colour it 2 and append it
to the post-order. The answer is the reverse post-order.

**Invariant.** The stack holds a directed path of active vertices; a vertex becomes finished only after
everything reachable from it is finished.

**Correctness.** An edge into an active vertex closes a cycle with the stack path, and every cycle
produces such an edge in some DFS. If no cycle exists, every edge `u -> v` finishes `v` before `u`, so
in the reverse post-order `u` comes before `v`. A finished vertex alone is not a cycle (this
matters for diamonds such as `0->1, 0->2, 1->3, 2->3`).

**Boundary cases.** A self-loop `u -> u` meets an active vertex and is reported as a cycle. A cycle in
a part of the graph unrelated to the first start vertex is found because every component is started.
`n = 0` gives `acyclic = true`, `order = []`. Duplicate edges are harmless.

**Complexity.** `O(n + m)` time and memory.

### 2.6 Shortest paths in a DAG

**Steps.** Run the same topological routine on the whole graph. If it finds a cycle (even one not
reachable from `source`), return `acyclic = false` with empty arrays. Otherwise set `dist[source] = 0`
(all others `null`), process vertices in topological order and relax each outgoing edge of a vertex that
has a distance, storing the **edge ID** in `parent_edge[v]` when `dist[v]` improves strictly.

**Invariant.** When a vertex `u` is processed, all of its incoming edges from reachable vertices have
already been relaxed (they come from earlier vertices in the order), so `dist[u]` is final.

**Correctness.** Any path respects the topological order, so the relaxation order sees every
edge of every path. The strict `<` keeps the first found edge among ties; the stored parent edge
always satisfies `dist[u] + w = dist[v]`, so following parent edges backwards reaches the source,
because the graph has no cycles. Vertices without a distance are never used for relaxation.

**Boundary cases.** Unreachable vertices keep `distance = null` and `parent_edge = -1`; `-1` is never
used as "unreachable" because it can be a real distance. The source has `parent_edge = -1`. Negative
weights are allowed. Distances are `long` (the deep-chain test reaches about -10^14). A self-loop makes
the graph cyclic. Parallel edges and tied routes are accepted in any valid form.

**Complexity.** `O(n + m)` time and memory.

### 2.7 Route-reconstruction helper

`reconstructPath(parentEdge, edges, source, target)` follows `parentEdge` from `target` back to
`source`, taking the tail `edges[e][0]` of each parent edge, and reverses the result.
Route to the source: `[source]`. **Documented choice for an unreachable target: an empty array.**
This applies to the helper only; JSON `distance` still uses `null`. Time and output space are `O(L)`
for a route with `L` vertices (plus a buffer of `n` integers).

## 3. State reset and deep DFS

**State reset.** `Algorithms` has no mutable static fields. Every array (parents, heads, colours,
stacks, results) is created inside the method call, so a request cannot influence the next one.
The public check "two complete batches in one process" passed.

**Deep DFS.** No method recurses. Each DFS uses an explicit stack array of size `n` and an array
`ptr[v]` holding the next unexplored edge of each vertex. A suspended vertex resumes from `ptr[v]`, so its
neighbours are never rescanned; every vertex is pushed once and every edge examined once. Chains of
100000 vertices (SCC, topological order, DAG) ran without stack failure.

## 4. Tests

Files in `tests/`:

| File | Content |
|---|---|
| `my_cases.jsonl` | 18 hand-written requests covering all six methods |
| `EXPECTED.md` | expected answers derived by hand, actual results, comparison |
| `actual_output.txt` | actual output for `my_cases.jsonl` |
| `check_cases.py` | semantic checker (see below) |
| `checker_selftest.txt`, `check_result.txt` | checker self-test and result on the 18 cases |
| `gen_large.py` | generator (seed 12345) of 9 large requests at the published limits |
| `large_check_result.txt` | checker result on the large requests |
| `RouteHelperTest.java`, `route_helper_output.txt` | tests of the route helper |
| `Benchmark.java`, `benchmark_output.txt` | Kruskal vs Prim benchmark |

The large input files are not stored (see `.gitignore`); `gen_large.py` reproduces them.

**Coverage of the 18 cases:** empty graphs (`n = 0`), a single vertex, isolated vertices, repeated and self
unions, self-loops, parallel and tied edges, negative weights, totals beyond 32 bits (5·10^9), duplicate
edges, a cycle in a part of the graph that does not contain vertex 0, a cycle unreachable from the DAG source,
and an unreachable vertex. All 18 matched the hand-derived answers. Kruskal and Prim returned
different edge sets with the same cost on case 4/5 (IDs {3,4,1} vs {1,3,5}; edges 4 and 5 both weigh 1).

**Large inputs (n = 100000, up to m = 250000):** 250000 random unions; Kruskal and Prim on the same random
graph; SCC on a 100000-vertex chain, on the same chain closed into one cycle and on a random graph;
topological order on the chain and on the cyclic chain; DAG with a chain of weight -10^9 plus random
forward edges. All 9 answers passed the checker. The whole batch ran in 1.358 s total
(`time`, including JVM start-up, JSON parsing and output, Apple M4).

**Checker (`check_cases.py`).** For non-unique answers it checks properties against independent references:
forest feasibility (distinct IDs, no loops, no cycles, `n - c` edges, cost equals the sum) and optimality
against a separate Kruskal in Python; SCC labels against an iterative Tarjan and the exact condensation set
without duplicates; topological precedence of every edge (cycles verified with Kahn's algorithm); DAG
distances against a reference and every parent edge ends at its vertex and lies on a shortest path.
The self-test shows that it **accepts** alternative valid answers (a different optimal edge set, different SCC
labels, a different topological order, a different parent of a tied vertex) and **rejects** invalid
ones (a non-optimal forest, a self-loop, a wrong cost, a duplicate condensation pair, two SCCs under one label,
a backward edge, a parent edge not on a shortest path). Output: `tests/checker_selftest.txt`.

**Route helper tests** (`tests/route_helper_output.txt`, all PASS). Edges are written as `id: u->v (w)`.

Graph A: `0: 0->1 (3)`, `1: 0->2 (8)`, `2: 1->2 (-7)`, `3: 2->3 (4)`; `n = 5`, source 0.
`dag` returned `distance = [0, 3, -4, 0, null]`, `parent_edge = [-1, 0, 2, 3, -1]`.

| Call | Result | Selected edge IDs | Check |
|---|---|---|---|
| route to 3 (reachable) | `[0, 1, 2, 3]` | 0, 2, 3 | directions 0->1, 1->2, 2->3 correct; 3 + (-7) + 4 = 0 = `distance[3]` |
| route to 0 (source) | `[0]` | none | route to the source is `[source]` |
| route to 4 (unreachable) | `[]` | none | `distance[4] = null`, `parent_edge[4] = -1` |

Graph B (parallel and tied edges): `0: 0->1 (5)`, `1: 0->1 (5)`, `2: 0->2 (10)`, `3: 1->2 (5)`; `n = 3`, source 0.
`dag` returned `distance = [0, 5, 10]`, `parent_edge = [-1, 1, 2]`.

| Call | Result | Selected edge IDs | Check |
|---|---|---|---|
| route to 1 | `[0, 1]` | 1 | edge 1 is 0->1, cost 5 = `distance[1]`; the parallel edge 0 would be equally valid |
| route to 2 | `[0, 2]` | 2 | edge 2 is 0->2, cost 10 = `distance[2]`; the tied route 0->1->2 (5 + 5) is equally valid |

Ties are resolved by the strict `<` in the relaxation: the first edge that reaches the best distance is kept.
Because the adjacency lists are linked lists built by prepending, edges of one vertex are scanned in reverse
input order, which is why edge 1 (not 0) and edge 2 (not 3) were kept.

## 5. Benchmark: Kruskal vs Prim

**Setup.** Both methods ran on the same in-memory graphs in a single JVM (`tests/Benchmark.java`).
Graphs: random endpoints, weights uniform in [-10^9, 10^9], `m = 2.5 n`, generated with
`java.util.Random(12345 + n)`. Sizes: n = 10000, 50000, 100000. Correctness was verified first: for every
size both outputs were valid forests with equal cost and equal component count (see `benchmark_output.txt`).
Warm-up: 2 untimed runs per method and size, then 5 timed runs. Timer: `System.nanoTime`.
Compilation is excluded. **Measured:** one call to `Algorithms.kruskal` or `Algorithms.prim`, which includes
the conversion of the request into arrays (`Input`) and the internal construction of sorted indices or
adjacency lists; **excluded:** graph generation, building the request map, JSON parsing and input/output.
The boundary is identical for both methods.

**Hardware and software.** Apple MacBook Air, Apple M4, 16 GiB RAM, macOS 15.5, Java HotSpot 64-Bit
Server VM 25.0.1 (build 25.0.1+8-LTS-27).

**Results (milliseconds).**

| n | m | Method | Run 1 | Run 2 | Run 3 | Run 4 | Run 5 | Median |
|---|---|---|---|---|---|---|---|---|
| 10000 | 25000 | Kruskal | 7.5 | 7.3 | 7.2 | 7.3 | 7.2 | 7.3 |
| 10000 | 25000 | Prim | 4.0 | 4.2 | 4.1 | 3.8 | 4.5 | 4.1 |
| 50000 | 125000 | Kruskal | 29.6 | 21.6 | 27.0 | 21.0 | 20.7 | 21.6 |
| 50000 | 125000 | Prim | 32.5 | 31.9 | 33.0 | 33.9 | 33.9 | 33.0 |
| 100000 | 250000 | Kruskal | 44.6 | 44.0 | 44.0 | 44.4 | 45.3 | 44.4 |
| 100000 | 250000 | Prim | 74.2 | 74.3 | 75.5 | 77.5 | 77.3 | 75.5 |

The graphs had 73, 352 and 725 connected components respectively.

**Asymptotic argument.** Both methods are `O(n + m log m)`. This predicts similar growth for both and
does not predict which is faster.

**Observations (measured).** At n = 10000 Prim was faster (4.1 ms vs 7.3 ms). At n = 50000 and n = 100000
Kruskal was faster (21.6 vs 33.0 ms and 44.4 vs 75.5 ms; Prim about 1.5 and 1.7 times slower).
Kruskal's times at n = 50000 varied noticeably between runs (20.7 to 29.6 ms).

**Possible explanations (not tested).** Kruskal sorts an `Integer[]` with a comparator, while this
Prim allocates one `long[]` per heap entry, so garbage collection and memory traffic may grow faster for Prim on
large graphs. The reversed result at n = 10000 may come from the short warm-up (2 runs) and JIT compilation
rather than from the algorithms. I did not profile the code, so these remain hypotheses.

**Limitations.** One machine, one seed per size, one graph family (random sparse graphs with many
small components), three sizes, five runs. The timed region includes input conversion. Results
for dense graphs, other weight distributions or an indexed heap may differ.

## 6. Sources and AI tools

I used Claude as a tutor and helper: it explained the assignment step by step and
helped to draft the algorithm code in `Algorithms.java`, the test files, the checker, the benchmark and this
document. The algorithms themselves are standard (DSU, Kruskal, Prim, Kosaraju, DFS topological sort,
DAG relaxation); no other external code was copied.