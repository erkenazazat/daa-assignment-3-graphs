import json, sys
from collections import deque


def find(p, x):
    while p[x] != x:
        p[x] = p[p[x]]
        x = p[x]
    return x


def kahn(n, edges):
    indeg = [0] * n
    adj = [[] for _ in range(n)]
    for e in edges:
        adj[e[0]].append(e[1])
        indeg[e[1]] += 1
    q = deque(i for i in range(n) if indeg[i] == 0)
    order = []
    while q:
        u = q.popleft()
        order.append(u)
        for v in adj[u]:
            indeg[v] -= 1
            if indeg[v] == 0:
                q.append(v)
    return order if len(order) == n else None


def ref_scc(n, edges):
    adj = [[] for _ in range(n)]
    for u, v in edges:
        adj[u].append(v)
    index = [-1] * n
    low = [0] * n
    on = [False] * n
    comp = [-1] * n
    st, idx, c = [], 0, 0
    for s in range(n):
        if index[s] != -1:
            continue
        index[s] = low[s] = idx
        idx += 1
        st.append(s)
        on[s] = True
        work = [(s, 0)]
        while work:
            v, i = work.pop()
            if i < len(adj[v]):
                work.append((v, i + 1))
                w = adj[v][i]
                if index[w] == -1:
                    index[w] = low[w] = idx
                    idx += 1
                    st.append(w)
                    on[w] = True
                    work.append((w, 0))
                elif on[w]:
                    low[v] = min(low[v], index[w])
            else:
                if low[v] == index[v]:
                    while True:
                        w = st.pop()
                        on[w] = False
                        comp[w] = c
                        if w == v:
                            break
                    c += 1
                if work:
                    par = work[-1][0]
                    low[par] = min(low[par], low[v])
    return comp


def check_dsu(req, out):
    n = req["n"]
    p = list(range(n))
    sz = [1] * n
    comps, largest, exp = n, (1 if n else 0), []
    for u, v in req["operations"]:
        a, b = find(p, u), find(p, v)
        if a != b:
            if sz[a] < sz[b]:
                a, b = b, a
            p[b] = a
            sz[a] += sz[b]
            comps -= 1
            largest = max(largest, sz[a])
        exp.append([comps, largest])
    return None if out.get("states") == exp else "states differ from reference"


def check_mst(req, out):
    n, edges = req["n"], req["edges"]
    p = list(range(n))
    best = cnt = 0
    for u, v, w in sorted(edges, key=lambda e: e[2]):
        a, b = find(p, u), find(p, v)
        if a != b:
            p[a] = b
            best += w
            cnt += 1
    comps = n - cnt
    ids = out.get("edge_ids")
    if out.get("components") != comps:
        return "wrong number of components"
    if len(set(ids)) != len(ids):
        return "repeated edge id"
    if len(ids) != n - comps:
        return "wrong number of selected edges"
    q = list(range(n))
    total = 0
    for i in ids:
        if not (0 <= i < len(edges)):
            return "edge id out of range"
        u, v, w = edges[i]
        if u == v:
            return "self-loop selected"
        a, b = find(q, u), find(q, v)
        if a == b:
            return "selected edges contain a cycle"
        q[a] = b
        total += w
    if out.get("cost") != total:
        return "cost is not the sum of the selected edges"
    if total != best:
        return "forest is not optimal"
    return None


def check_scc(req, out):
    n, edges = req["n"], req["edges"]
    ref = ref_scc(n, edges)
    comp = out["component"]
    if len(comp) != n or any(x < 0 for x in comp):
        return "bad component array"
    fwd, bwd = {}, {}
    for v in range(n):
        if fwd.setdefault(comp[v], ref[v]) != ref[v]:
            return "one label covers two different SCCs"
        if bwd.setdefault(ref[v], comp[v]) != comp[v]:
            return "one SCC is split over two labels"
    expected = {(comp[u], comp[v]) for u, v in edges if comp[u] != comp[v]}
    got = [tuple(x) for x in out["condensation"]]
    if len(got) != len(set(got)):
        return "duplicate condensation pair"
    if set(got) != expected:
        return "condensation differs"
    return None


def check_topo(req, out):
    n, edges = req["n"], req["edges"]
    if kahn(n, edges) is None:
        ok = out.get("acyclic") is False and out.get("order") == []
        return None if ok else "cyclic graph needs acyclic=false and order=[]"
    if out.get("acyclic") is not True:
        return "graph is acyclic but answer says it is not"
    order = out["order"]
    if sorted(order) != list(range(n)):
        return "order is not a permutation"
    pos = [0] * n
    for i, v in enumerate(order):
        pos[v] = i
    for u, v in edges:
        if pos[u] >= pos[v]:
            return "edge %d->%d goes backwards" % (u, v)
    return None


def check_dag(req, out):
    n, edges, s = req["n"], req["edges"], req["source"]
    order = kahn(n, edges)
    if order is None:
        ok = (out.get("acyclic") is False and out.get("distance") == []
              and out.get("parent_edge") == [])
        return None if ok else "cyclic graph needs acyclic=false and empty arrays"
    if out.get("acyclic") is not True:
        return "graph is acyclic but answer says it is not"
    adj = [[] for _ in range(n)]
    for u, v, w in edges:
        adj[u].append((v, w))
    dist = [None] * n
    dist[s] = 0
    for u in order:
        if dist[u] is None:
            continue
        for v, w in adj[u]:
            if dist[v] is None or dist[u] + w < dist[v]:
                dist[v] = dist[u] + w
    if out["distance"] != dist:
        return "distances differ from reference"
    pe = out["parent_edge"]
    if len(pe) != n:
        return "parent_edge has wrong length"
    # In an acyclic graph, edges with dist[u]+w == dist[v] cannot form a loop,
    # so every parent chain ends at the only vertex without a parent: the source.
    for v in range(n):
        if v == s or dist[v] is None:
            if pe[v] != -1:
                return "source and unreachable vertices need parent_edge=-1"
            continue
        e = pe[v]
        if not (0 <= e < len(edges)):
            return "parent edge id out of range at vertex %d" % v
        u, x, w = edges[e]
        if x != v:
            return "parent edge of %d does not end at %d" % (v, v)
        if dist[u] is None or dist[u] + w != dist[v]:
            return "parent edge of %d is not on a shortest path" % v
    return None


CHECKERS = {"dsu": check_dsu, "kruskal": check_mst, "prim": check_mst,
            "scc": check_scc, "topo": check_topo, "dag": check_dag}


def selftest():
    g = {"task": "kruskal", "n": 6,
         "edges": [[0, 1, 4], [0, 1, 3], [1, 1, -5], [2, 3, -3], [3, 4, 1], [2, 4, 1], [2, 3, 7]]}
    s = {"task": "scc", "n": 6,
         "edges": [[0, 1], [1, 2], [2, 0], [2, 3], [3, 4], [4, 3], [3, 3], [1, 2], [5, 5]]}
    t = {"task": "topo", "n": 5, "edges": [[0, 1], [0, 2], [1, 3], [2, 3], [3, 4], [0, 1]]}
    d = {"task": "dag", "n": 3, "source": 0,
         "edges": [[0, 1, 5], [0, 1, 5], [0, 2, 10], [1, 2, 5]]}
    d2 = {"task": "dag", "n": 5, "source": 1,
          "edges": [[0, 1, 2], [1, 2, -4], [1, 3, 5], [2, 3, 1], [3, 4, -10]]}
    cases = [
        ("kruskal: valid optimum {1,3,4}", g,
         {"cost": 1, "edge_ids": [3, 4, 1], "components": 3}, True),
        ("kruskal: ALTERNATIVE optimum {1,3,5}", g,
         {"cost": 1, "edge_ids": [1, 3, 5], "components": 3}, True),
        ("kruskal: non-optimal forest", g,
         {"cost": 2, "edge_ids": [0, 3, 4], "components": 3}, False),
        ("kruskal: self-loop selected", g,
         {"cost": -7, "edge_ids": [2, 3, 4], "components": 3}, False),
        ("kruskal: wrong cost claimed", g,
         {"cost": 5, "edge_ids": [1, 3, 4], "components": 3}, False),
        ("scc: valid labels", s,
         {"component": [1, 1, 1, 2, 2, 0], "condensation": [[1, 2]]}, True),
        ("scc: ALTERNATIVE labels", s,
         {"component": [7, 7, 7, 3, 3, 9], "condensation": [[7, 3]]}, True),
        ("scc: duplicate pair", s,
         {"component": [7, 7, 7, 3, 3, 9], "condensation": [[7, 3], [7, 3]]}, False),
        ("scc: two SCCs under one label", s,
         {"component": [0, 0, 0, 0, 0, 1], "condensation": []}, False),
        ("topo: valid order", t, {"acyclic": True, "order": [0, 2, 1, 3, 4]}, True),
        ("topo: ALTERNATIVE order", t, {"acyclic": True, "order": [0, 1, 2, 3, 4]}, True),
        ("topo: edge 0->1 goes backwards", t, {"acyclic": True, "order": [1, 0, 2, 3, 4]}, False),
        ("dag: valid parents", d,
         {"acyclic": True, "distance": [0, 5, 10], "parent_edge": [-1, 1, 2]}, True),
        ("dag: ALTERNATIVE parents (tie)", d,
         {"acyclic": True, "distance": [0, 5, 10], "parent_edge": [-1, 0, 3]}, True),
        ("dag: parent not on a shortest path", d2,
         {"acyclic": True, "distance": [None, 0, -4, -3, -13], "parent_edge": [-1, -1, 1, 2, 4]}, False),
    ]
    bad = 0
    for name, req, out, should_pass in cases:
        err = CHECKERS[req["task"]](req, out)
        ok = (err is None) == should_pass
        bad += not ok
        print(("OK  " if ok else "BUG ") + name + ("" if err is None else "  -> rejected: " + err))
    print("selftest:", "all behaved as expected" if bad == 0 else "%d problems" % bad)
    return bad


def main():
    if sys.argv[1] == "--selftest":
        sys.exit(1 if selftest() else 0)
    reqs = [json.loads(l) for l in open(sys.argv[1]) if l.strip()]
    outs = [json.loads(l) for l in open(sys.argv[2]) if l.startswith("{")]
    if len(reqs) != len(outs):
        print("line count mismatch: %d requests, %d outputs" % (len(reqs), len(outs)))
        sys.exit(1)
    bad = 0
    for i, (req, out) in enumerate(zip(reqs, outs), 1):
        err = CHECKERS[req["task"]](req, out)
        bad += err is not None
        print("%s line %d (%s)%s" % ("PASS" if err is None else "FAIL", i, req["task"],
                                     "" if err is None else ": " + err))
    sys.exit(1 if bad else 0)


main()