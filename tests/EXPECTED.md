# Test cases: expected vs actual

Line numbers refer to `tests/my_cases.jsonl`. Expected answers were derived by hand.
Actual answers are in `tests/actual_output.txt` (produced by
`python3 tools/run.py --input tests/my_cases.jsonl`).
Where the answer is not unique, it is checked by its properties, not by exact match.

| # | Test | Expected (by hand) | Actual | Result |
|---|------|--------------------|--------|--------|
| 1 | dsu, n=5, repeated and self unions | `[[4,2],[3,2],[3,2],[3,2],[2,4],[2,4]]`. Merges at [0,1], [2,3], [0,3]; the other operations change nothing. | `[[4,2],[3,2],[3,2],[3,2],[2,4],[2,4]]` | Match |
| 2 | dsu, n=1, self-union | `[[1,1]]` | `[[1,1]]` | Match |
| 3 | dsu, n=0 | `states=[]` | `[]` | Match |
| 4 | kruskal, parallel edges, self-loop, negative weights, isolated vertex 5 | components=3, 3 edges, cost 1. Edge 1 (weight 3), edge 3 (-3), and edge 4 or 5 (weight 1). Self-loop 2 must not be chosen. | cost 1, ids {3,4,1}, components 3 | Match (valid optimum) |
| 5 | prim, same graph | cost 1, components 3, ids may differ | cost 1, ids {1,3,5}, components 3 | Match (different but equally optimal ids: edges 4 and 5 both have weight 1) |
| 6 | kruskal, n=0 | cost 0, no edges, 0 components | as expected | Match |
| 7 | prim, n=0 | cost 0, no edges, 0 components | as expected | Match |
| 8 | kruskal, 5 edges of weight 10^9 | cost 5000000000 (more than 2^32), ids {0,1,2,3,4}, 1 component | as expected | Match |
| 9 | prim, same graph | same as test 8 | as expected | Match |
| 10 | scc, duplicate edge and self-loops | SCCs {0,1,2}, {3,4}, {5}. One condensation pair, from edge 2->3. | labels [1,1,1,2,2,0], condensation [[1,2]] | Match (labels are arbitrary, groups are correct) |
| 11 | scc, n=0 | both arrays empty | as expected | Match |
| 12 | topo, acyclic, duplicate edge | acyclic=true, 0 before 1 and 2, both before 3, 3 before 4 | order [0,2,1,3,4] | Match (valid order) |
| 13 | topo, cycle 2->3->4->2 | acyclic=false, order=[] | as expected | Match |
| 14 | topo, n=0 | acyclic=true, order=[] | as expected | Match |
| 15 | dag, source 1, vertex 0 unreachable | distance [null,0,-4,-3,-13], parent_edge [-1,-1,1,3,4]. Vertex 3: via vertex 2 gives -3, better than direct edge (5). | as expected | Match |
| 16 | dag, cycle 2<->3 not reachable from source 0 | acyclic=false, empty arrays (the whole graph is checked) | as expected | Match |
| 17 | dag, parallel and tied edges | distance [0,5,10]. Parent of vertex 1 is edge 0 or 1. Parent of vertex 2 is edge 2 or 3 (tie: 10 = 5+5). | distance [0,5,10], parent_edge [-1,1,2] | Match (one of the valid tied answers) |
| 18 | dag, n=1 | distance [0], parent_edge [-1] | as expected | Match |