[Documentation](../README.md) › [Inside Testin](main.md) › UC-INTERNAL-006

# UC-INTERNAL-006: Count what a node holds

**As a** tester, **I want** the numbers on a node to be true at the moment I
read them, **so that** I can decide from them, especially in the dialog that
asks me to confirm a removal.

These are the counts on the Details dialog and on the removal confirmation.

There is no key for this. The counts are worked out when a screen that shows
them opens.

## Rules

- **Rule-INTERNAL-046** — A count is worked out when it is asked for, and stored
  nowhere. A number that is never saved cannot be stale.
- **Rule-INTERNAL-047** — A container is the sum of everything beneath it, at
  any depth.
- **Rule-INTERNAL-048** — A test run is counted from the run item statuses it
  recorded, not from the test cases it was made from.
- **Rule-INTERNAL-049** — A test run's numbers come from one place, so its
  Details and its report can never disagree.
- **Rule-INTERNAL-050** — Retired test sets and archived packages are counted.
  That a branch is out of current work is already visible in the tree, which
  draws it gray and sorts it last.
- **Rule-INTERNAL-051** — A test run that could not be read counts as nothing,
  rather than failing. The tree already draws the node, so its Details still
  says what the node is.
- **Rule-INTERNAL-052** — A test run nobody has judged shows *Not run*, never *0%*.
- **Rule-INTERNAL-065** — When a new test run would not take every test case
  beneath a node, the count says how many it would. Both numbers are right, and
  they answer different questions, so the one on screen names the other rather
  than leaving a tester to find the difference by making a test run.

## Where the counts appear

| Screen                           | What it shows                                                                                              |
|----------------------------------|------------------------------------------------------------------------------------------------------------|
| The Details popup on a tree node | A row for each count, and a ring for a test run. Drawn on [UC-TREE-PANEL-027](../treePanel/nodeDetails.md) |
| The removal confirmation         | One line saying what goes with the node. Drawn on [UC-TREE-PANEL-012](../treePanel/removeNode.md)          |

## What each node counts

| The node           | The counts it shows                        |
|--------------------|--------------------------------------------|
| A test project     | Test sets, Packages, Test cases, Test runs |
| **Test Cases**     | Test sets, Packages, Test cases            |
| **Test Runs**      | Packages, Test runs                        |
| A test set package | Test sets, Packages, Test cases            |
| A test run package | Packages, Test runs                        |
| A test set         | Test cases                                 |
| A test run         | Total, and a ring of its run item statuses |

The run item statuses on the ring are **Passed**, **Failed**, **Blocked**, **Pending** or **Untested**, and **Removed**.
The test cases nobody has judged read **Pending** while the test run is open and **Untested** once it is signed off, as
in its editor (Rule-EDITOR-PANEL-176). The pass rate sits in the middle of it.

## The screen

The counts sit at the bottom of the Details dialog. The rows above them are
drawn on [UC-TREE-PANEL-027](../treePanel/nodeDetails.md).

```
┌──────────────────────────────────────────────────────────────┐
│  Details                                                     │
├──────────────────────────────────────────────────────────────┤
│  NAME                                                        │
│  Accounts                                                    │
│  STATUS                                                      │
│  Active                                                      │
│                                                              │
│  TEST SETS                                              (1)  │
│  2                                                           │
│  PACKAGES                                                    │
│  1                                                           │
│  TEST CASES                                                  │
│  14                                               (2)        │
├──────────────────────────────────────────────────────────────┤
│  Escape Close                                                │
└──────────────────────────────────────────────────────────────┘
```

1. **A count row** — one row for each kind of thing this node can hold. Which
   rows appear depends on the kind of node.
2. **The number** — worked out the moment the dialog opened. It is never saved.

A test run is different. It shows one row reading **Total**, and a ring beside
it. The ring has one color for each run item status. The pass rate sits in the
middle. A test run nobody has judged reads **Not run** there instead.

The removal confirmation shows the same counts as one line. That dialog is
drawn on [UC-TREE-PANEL-012](../treePanel/removeNode.md).

```
┌──────────────────────────────────────────────────────────────┐
│  Confirm Removing                                            │
├──────────────────────────────────────────────────────────────┤
│  Remove 'Accounts'?                                          │
│  Holds 2 test sets, 14 test cases and 0 test runs       (1)  │
└──────────────────────────────────────────────────────────────┘
```

1. **The Holds line** — one line, and only when the node holds something.
   Packages are not counted on it.

## Main flow

1. The tester opens Details on a node, or presses `Delete` on it.
2. Testin walks everything beneath that node, in memory.
3. Testin groups what it finds by kind, and adds up each kind.
4. Test cases are counted from how many the test set holds. They are not
   sorted first. Sorting 2,770 test cases to make a number nobody reads is work
   for nothing.
5. A test run is not walked. Its recorded run item statuses are added up
   instead.
6. The screen draws the rows. The removal confirmation draws one line, reading *Holds*, then the test sets, the test
   cases and the test runs.
7. Nothing is saved. The next screen that asks counts again.

## What Testin refuses

**If the node holds nothing** — the removal confirmation shows no *Holds* line
at all, rather than a line of zeros.

**If a test run has no run item statuses yet** — the middle of the ring reads *Not run*. A *0%* there would read as
every test case having failed.

**If a test run cannot be read** — every count is zero and nothing fails. The
node is still in the tree, so Details still opens on it.

**If the branch holds retired test sets** — they are counted. A deprecated test
set still holds its test cases. The row also says how many a new test run would
take, which is the same count without them: *40 (31 for a new test run)*.

## Why it works this way

The counts used to be found by matching the start of each path against the
node's own path. A node's path starts with itself. So removing a test set that
held 12 test cases read *Holds 1 test set, 12 test cases and 0 test runs*. And
removing a test run said it held one test run. That was in the one dialog whose
whole job is to say what an unrecoverable removal takes with it.

---

[Documentation](../README.md) › [Inside Testin](main.md)
