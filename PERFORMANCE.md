# Performance

**Goal.** Check that the two claims in DECISIONS.md hold up: reading a big tree stays fast with flat memory (streaming), and the cycle check on create stays cheap on a deep tree.

**Setup.** This was done on a signle instance on a local network, so short runs and curl insteal of a load tool, so numbers should be read as relative... ideally a proper run would use tool like gatling.

Load came from curl in parallel, 10 at once (4 for the big trees). Data was seeded straight into the table:

- the tree from the brief, 6 nodes
- a wide tree, 100k nodes with 4 children each (about 9 levels)
- a chain 10k levels deep

**Reads.** Small subtrees and `maxDepth=2` sit at about 3.5 ms p50 and 10 ms p99. The full 100k tree takes about 140 ms one request at a time, and about 800 ms p50 / 1.1 s p99 with 4 in parallel, so roughly 5 full trees a second. The first byte comes back in about 4 ms whatever the size, so the response starts before the query is done. The 10k deep chain is about 90 ms.

**Writes.** A new leaf is about 11 ms p50 / 32 ms p99 at 10 in parallel. Every create takes the advisory lock, so creates run one after the other. That is on purpose (correct tree over write throughput), but its the first limit for write-heavy use. The worst cycle check, attaching under the node 10k levels deep, is about 41 ms p50 / 70 ms p99. Deletes, 404s and 409s are under 5 ms p50.

No errors or unexpected status codes across about 8,300 requests.

**What i would watch.**

- Full reads of very large trees under concurrency, the cost grows with the rows returned. A server side cap on rows or `maxDepth` is the lever.
- Write throughput is capped by the one global lock. If that becomes a problem, lock per tree instead.
