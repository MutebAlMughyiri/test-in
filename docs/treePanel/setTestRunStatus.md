[Documentation](../README.md) › [The tree panel](main.md) › UC-TREE-PANEL-020

# UC-TREE-PANEL-020: Set a test run's status

> **No key opens it.** On the menu: **Set Status**. Inside the popup,
> **`1`** is **Assigned**, **`2`** is **Completed** and **`3`** is
> **Closed**.

**As a** tester, **I want** to mark a test run **Assigned**, **Completed** or **Closed** from the tree, **so that** the
test run's place in its life is
visible without opening it.

The status says how far the test run has got.

## Rules

- **Rule-TREE-PANEL-001** — The panel shows exactly one test project. It is the
  one this repository is bound to. There is no list of test projects in the
  tree.
- **Rule-TREE-PANEL-002** — **Test Cases** and **Test Runs** are fixed
  containers. They cannot be created, renamed, moved, copied or removed. They
  come with the test project and go with it.
- **Rule-TREE-PANEL-003** — The tree has two sides, and nothing moves between
  them. Test sets and test set packages live under **Test Cases**. Test runs and
  test run packages live under **Test Runs**.
- **Rule-TREE-PANEL-004** — Two nodes under one parent cannot share a name. This
  holds whether the node is created, renamed, pasted or dropped. A paste or a
  drop counts a name that differs only in capitals as the same name, on every
  system.
- **Rule-TREE-PANEL-005** — A node name is never empty.
- **Rule-TREE-PANEL-006** — Removing, moving or copying asks first. Nothing in
  the tree changes until the tester confirms.
- **Rule-TREE-PANEL-007** — When something changes, Testin says so once, in the
  past tense. The tester sees *Created*, *Renamed* or *Removed*. Several changes
  made together confirm with one message and a count: the tester sees *Removed 4*, never four messages. Looking at
  something confirms nothing.
- **Rule-TREE-PANEL-008** — A retired node keeps everything inside it. Retired
  means a **Deprecated** test set or an **Archived** package. It is drawn gray.
  It sorts after live nodes. It is not offered when a test run is created.
- **Rule-TREE-PANEL-009** — A test run that is **Completed** or **Closed** keeps
  its test cases and configuration: Edit Test Run refuses it. Its run item
  statuses can still be given and corrected until it is **Committed**. A
  Committed test run changes no more.
- **Rule-TREE-PANEL-010** — Siblings are shown in one order:
    1. live nodes, then retired ones
    2. the number the tester gave the node
    3. the date the node was created
    4. the name
- **Rule-TREE-PANEL-011** — A removed node goes to the desktop's recycle bin. It
  can be put back from the tree.
- **Rule-TREE-PANEL-012** — A test case is not a node in this tree. It is
  reached by opening its test set.
- **Rule-TREE-PANEL-013** — Nodes move only within one test project. Nothing cut
  in one test project can be pasted into another.
- **Rule-TREE-PANEL-099** — A node says its status beside its name when the status
  is not active. An inactive test project, a deprecated test set and an archived
  package each say which they are, in gray; a node in current work says nothing,
  because "Active" on every name is a word read a hundred times and needed
  never. A test run always says its status, because where a cycle stands is what
  the tree is read for.
- **Rule-TREE-PANEL-067** — Completing or closing a test run turns every test
  case still **Pending** into **Untested**. Its run item statuses can still be
  given and corrected until it is **Committed** (Rule-TREE-PANEL-009).
- **Rule-TREE-PANEL-068** — A tester sets **Assigned**, **Completed** and **Closed**. **Created** and **In Progress**
  are the test run's own record of
  itself.
- **Rule-TREE-PANEL-092** — The popup offers the statuses this test run can be
  moved to and no others: the three a tester sets, and only those ahead of where
  the test run is now. A test run is never sent backwards through its own life.
- **Rule-TREE-PANEL-091** — A test run's status is set on the test run, not on
  whichever surface set it. The tree row and any open editor of that test run
  both follow it, whichever of them the tester used.
- **Rule-TREE-PANEL-100** — A test project that is not active is shown in the
  tree and holds nothing. It is indexed as a node so the tree can say what it
  is, drawn with Inactive beside its name like any other status. Its test sets,
  test cases and test runs are not read, because a project nobody is working on is
  not worth the walk.
- **Rule-TREE-PANEL-104** — A menu entry that cannot work on the selected row is
  gray, and says why when the pointer rests on it.
- **Rule-TREE-PANEL-135** — Committed is the last test run status, and only
  Testin sets it. A Committed test run is read-only: no status, run item status
  or edit changes it.
- **Rule-TREE-PANEL-136** — When a test run of a test project under Git becomes
  Completed, a notification says to commit it to keep it as the record, and its
  View Pending Commits button opens the review for that test project. It stays
  in the Notifications list. A test project not under Git gets no such
  notification.

## The Set Test Run Status popup

```
┌──────────────────────────────────────────────────────────────┐
│  Set Test Run Status                                         │
├──────────────────────────────────────────────────────────────┤
│  > !  Assigned                                      1   (1)  │
│    *  Completed                                     2        │
│    x  Closed                                        3        │
├──────────────────────────────────────────────────────────────┤
│  [k]  Enter Confirm      ↑↓ Select      Escape Cancel   (3)  │
└──────────────────────────────────────────────────────────────┘
```

1. **One row for each status the test run can move to** — each with its icon,
   the same icon the tree draws for a test run in that status. Drawn here for a
   test run that is *Created*, which can move to any of the three. From *Assigned* or *In Progress* there are two rows,
   Completed and Closed.
2. **Only forward, and only what a tester sets.** *Created* and *In Progress*
   are the test run's own record of itself and are never rows; nor is the status
   the test run already has, because moving to it would say nothing. The keys
   belong to the statuses rather than to the positions: **Assigned** is `1`, **Completed** is `2`, **Closed** is `3`,
   whichever of them the popup is
   showing.
3. **The status bar says how to choose.** The popup opens in the middle of the
   IDE window, not at the pointer, with the first row selected. The tester
   chooses with `↑` `↓` and `Enter`, with a key, or with a click.

## Main flow

1. The tester selects a test run that is **Created**, **In Progress** or **Assigned**.
2. The tester chooses **Set Status**.
3. The **Set Test Run Status** popup lists the statuses a tester sets. Three of them
   carry a key: `1` **Assigned**, `2` **Completed**, `3` **Closed**.
4. The tester presses that key, or moves with `↑` `↓` and presses `Enter`, or
   clicks a row.
5. Testin writes the status. In the tree, the test run's icon and its gray
   status word both change. Testin shows the new status word.
6. Setting **Completed** or **Closed** signs the test run off. Any execution
   still running on it stops. Every test case
   still **Pending** becomes **Untested**, except one removed from its test set,
   which stays as the test run recorded it. Testin records the time the test run
   finished, but only if it had been started. From then on **Set Status**, **Edit Test Run** and **Run Tests** are gray
   on it.
7. Setting **Completed** shows a notification instead of the status word: *Commit
   <name> in View Pending Commits to keep it as the record.* Its **View Pending
   Commits** button opens the review, and the commit makes the test run **Committed** (Rule-TREE-PANEL-136,
   Rule-SHARE-130).

**Signing off a test run cannot be undone.** A status change is not on the
tree's history (Rule-TREE-PANEL-060). Testin asks for no confirmation before it.
Every **Pending** run item status becomes **Untested** at that moment, except on
a test case removed from its test set.

## What Testin refuses

**If the tester clicks outside the popup** — it closes, and nothing changes.

**If several rows are selected** — **Set Status** stays black and acts on the
last row the tester clicked.

> **An open editor of that test run follows.** Its status, its run item status
> counts and the rows that just became **Untested** all redraw where they are
> (Rule-TREE-PANEL-091). The same holds the other way: a status the editor sets
> redraws the tree row.

**Not decided** — see question 1 and question 2 on
[the tree panel page](main.md#not-decided).

---

[Documentation](../README.md) › [The tree panel](main.md)
