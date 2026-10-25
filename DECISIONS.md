# Design decisions

Decisons made while building the Edge Tree API: the context, the options considered, what was chosen and when to revisit it.

Example tree used throughout (from the brief):

```text
1 → 2, 1 → 3, 2 → 4, 2 → 5, 3 → 6
```

`from_id` is the parent, `to_id` is the child.

---

## 1. Tree rules on edge creation

**Context.** The brief says to treat the data as "a tree-like structure instead of a graph", wasnt also given the tree type so i went with an N-ary tree(My Assumpotion), so the API rejects:

- Self Link - a node cant link or be its own parent
- Second Parent - Every has one one parent.
- Cycle - we do the above, so its difficult to have cycles but then there is no prevention from have a connection to the tree root(not sub tree)

**Out of scope.** Didnt consider patching or moving a node to a different tree, so today that means deleting the old edge, then creating the new one.

---

## 2. Cycle detection

**Context.** After the parent check passes, from the cycle consideration above, to handle this issue i made some research on an approach and then what works best for now and surely can be revisited and these are the options I considered form my checks:

- Walk from the `from` up until root, when the `to` has children, issue here is that as the treen length deepens the slower it gets(an O(N)problem).
- Reject unless `to` is new, we had not instruction on tree must be built so it totally works if the tree is build top-down but imagine an organization under us maybe have sub orgs? with differnt supply chain? that was the thinking around here.
- Store each node's root - yeah but attaching or detaching a subtree rewrites every node in it, delete becomes expensive and sync considerations.

**Decision.** Walk up from `from` with one recursive SQL query, using the `to_id` index. Skip the walk entirely when `to` has no children, which covers the common case of attaching a new node, becuase it accepts every valid edge in any order, keeps writes to one row and needs no extra tables. The brief already mandates the `edge` table.

A considered cost as seen above is in a chain 1,000,000 levels deep, attaching the root under its deepest node costs about one index lookup per level. Supply-chain trees are typically wide rather than deep.

**Revisit if** (when or if there is real data):

- edge creation p99 latency exceeds a agree Xms, or
- trees deeper than about 10,000 levels appear -- in this case i think we can maybe establish a creation rule would say top-down and its cleaner.
