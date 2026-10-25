import json, random

random.seed(12345)
N, M, W = 100000, 250000, 10**9
chain = [[i, i + 1] for i in range(N - 1)]
cases = []

# DSU: 250000 random unions
cases.append({"task": "dsu", "n": N,
              "operations": [[random.randrange(N), random.randrange(N)] for _ in range(250000)]})

# Kruskal and Prim on exactly the same random graph
mst_edges = [[random.randrange(N), random.randrange(N), random.randint(-W, W)] for _ in range(M)]
cases.append({"task": "kruskal", "n": N, "edges": mst_edges})
cases.append({"task": "prim", "n": N, "edges": mst_edges})

# SCC: deep chain (100000 singletons), one giant cycle, random graph
cases.append({"task": "scc", "n": N, "edges": chain})
cases.append({"task": "scc", "n": N, "edges": chain + [[N - 1, 0]]})
cases.append({"task": "scc", "n": N,
              "edges": [[random.randrange(N), random.randrange(N)] for _ in range(M)]})

# Topo: deep chain, and the same chain closed into a cycle
cases.append({"task": "topo", "n": N, "edges": chain})
cases.append({"task": "topo", "n": N, "edges": chain + [[N - 1, 0]]})

# DAG: deep chain of weight -1e9 (total about -1e14, beyond 32 bits) plus random forward edges
dag_edges = [[i, i + 1, -W] for i in range(N - 1)]
while len(dag_edges) < M:
    a = random.randrange(N - 1)
    b = random.randrange(a + 1, N)
    dag_edges.append([a, b, random.randint(-W, W)])
cases.append({"task": "dag", "n": N, "source": 0, "edges": dag_edges})

with open("tests/large_cases.jsonl", "w") as f:
    for c in cases:
        f.write(json.dumps(c, separators=(",", ":")) + "\n")
print("wrote", len(cases), "cases")