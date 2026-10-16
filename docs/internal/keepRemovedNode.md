[Documentation](../README.md) › [Inside Testin](main.md) › UC-INTERNAL-005

# UC-INTERNAL-005: Keep a removed node so it can come back

**As a** tester, **I want** what I removed to be recoverable twice over, **so that** a wrong `Delete` costs me a
keypress and not a morning of rewriting
test cases.

This is what lets `Ctrl+Z` bring a removed node back.

`Delete` removes. `Ctrl+Z` puts it back. Removing is
[UC-TREE-PANEL-012](../treePanel/removeNode.md) and undo is
[UC-TREE-PANEL-016](../treePanel/undoChange.md). This page is what happens
between them.

## Rules

- **Rule-INTERNAL-036** — A removed node goes to the desktop recycle bin, so the
  tester can find it without Testin.
- **Rule-INTERNAL-037** — Testin also keeps its own copy, so `Ctrl+Z` has
  something to put back. The recycle bin can be filled and not emptied, so it
  cannot serve.
- **Rule-INTERNAL-038** — The kept copy never lives under the Testin folder. It
  would be read, committed and synced to every machine the tester works on.
- **Rule-INTERNAL-039** — Each removal gets its own place to be kept. Two test
  sets removed under the same name are two things to put back, not one over the
  other.
- **Rule-INTERNAL-040** — One press of `Ctrl+Z` puts back everything one
  `Delete` removed.
- **Rule-INTERNAL-041** — A removal whose copy could not be made still happens.
  It simply cannot be undone.
- **Rule-INTERNAL-063** — One press raises one message. A `Ctrl+Z` that could
  not put everything back says only what went wrong; it does not also say *Undone*.
- **Rule-INTERNAL-042** — Putting something back never writes over something
  that is there.
- **Rule-INTERNAL-043** — Kept copies are thrown away when the removal falls off
  the undo history, which holds 20 changes.
- **Rule-INTERNAL-044** — Every kept copy is thrown away at the next IDE start.
- **Rule-INTERNAL-045** — Undo history belongs to the surface it was made in.
  The tree keeps one, and each open editor keeps its own.
- **Rule-INTERNAL-143** — Testin never deletes a tester's test data or
  automation code for good. Whatever it removes - a node, a test case, a run
  item, a screenshot, a class file or a package folder - goes to the desktop
  recycle bin. What the bin does not take, because the desktop has none or a
  file in it is open in another program, stays where it is, and Testin says so
  in a notification titled *Not Removed*. Only the files Testin makes for itself
  are deleted outright: a file it is still writing, the copies it keeps for
  undo, and a file it hands to git, gh or an agent.

## Where a removed node goes

| Where                                            | What it is for                                                    |
|--------------------------------------------------|-------------------------------------------------------------------|
| The desktop recycle bin                          | The tester's copy. Found and restored without Testin              |
| A folder the IDE owns, outside the Testin folder | Testin's copy, so `Ctrl+Z` can reach it                           |
| Nowhere                                          | Only a node that was already gone (Rule-INTERNAL-143)             |

## The screen

When a node cannot be put back, a second message arrives with the first.

```
┌──────────────────────────────────────────────────────────────┐
│  Undo Incomplete                                       [ X ] │
│                                                              │
│  1 of 1 could not be put back                                │
└──────────────────────────────────────────────────────────────┘
```

1. **The title** — always these two words.
2. **The count** — how many of the kept copies did not go back.

The confirmation the tester sees before removing anything is drawn on
[UC-TREE-PANEL-012](../treePanel/removeNode.md).

## Main flow

1. The tester selects one or more nodes and presses `Delete`.
2. Testin counts what each node holds and shows the confirmation.
3. The tester chooses **Remove**.
4. Testin closes any editor open on those nodes.
5. Testin copies each node to the place it keeps copies.
6. Testin removes each node, into the desktop recycle bin.
7. Testin forgets the node only when the removal actually succeeded.
8. The tree redraws. The tester sees *Removed*, or *Removed 4* for several.
9. The whole gesture becomes one entry on the undo history.
10. The tester presses `Ctrl+Z`.
11. Testin copies each kept node back to where it was.
12. Testin reads that test project again, on its own, with nothing else reading
    at the same time.
13. The tree redraws. The tester sees *Undone*.

## What Testin refuses

**If the node cannot be copied aside** — the removal still happens. A message
titled *Cannot Be Undone* says so at that moment, naming how many could not be
copied aside. `Ctrl+Z` puts back the nodes whose copy was kept.

**If nothing at all could be copied aside** — the removal is still on the undo
history, so `Ctrl+Z` answers for it and says nothing was kept to put back. It
never takes back the change before it instead (Rule-TREE-PANEL-041).

**If the recycle bin does not take a node** — because the desktop has none, or
a file in it is open in another program — the node stays where it is, in the
tree and on disk. A notification titled *Not Removed* names it and stays in the
notification log. Nothing is deleted instead (Rule-INTERNAL-143).

**If something already occupies the place a node came from** — Testin refuses to
put it back. Writing over it would destroy one thing to restore another. The
tester sees **Undo Incomplete**.

**If an undo could not put everything back** — the press is not spent. `Ctrl+Z`
still offers the same change, and `Ctrl+Y` is not given a change the tester
never got back.

**If putting a node back fails** — the tester sees one message, **Undo
Incomplete**, saying how many did not make it. *Undone* is not raised, because
the press did not undo what it said it would.

**If the removal has fallen off the undo history** — the kept copy went with it.
The recycle bin still has the tester's copy.

**If the IDE was restarted** — every kept copy was thrown away at startup. The
recycle bin still has the tester's copy.

**If the tester presses `Ctrl+Z` in an editor** — the editor's own history
answers, not the tree's. A removal made in the tree is taken back in the tree.

## Why it works this way

The copies are thrown away at startup, not at shutdown. The copies that matter
are the ones a shutdown never reached. An IDE that crashed leaves them behind,
and the next start is the one moment certain to come.

A kept copy is deleted outright. It is not sent to the recycle bin. The bin
already took the tester's copy when the node was removed. A second copy
arriving later is a duplicate nobody asked for.

---

[Documentation](../README.md) › [Inside Testin](main.md)
