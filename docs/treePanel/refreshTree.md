[Documentation](../README.md) › [The tree panel](main.md) › UC-TREE-PANEL-025

# UC-TREE-PANEL-025: Refresh the tree from disk

> **No key.** Press **Refresh** at the top of the panel.

**As a** tester, **I want** to reload the tree after something changed outside
the IDE, **so that** the tree shows what is on disk. Getting changes from Git,
running a sync, or editing files by hand all change the tree from outside.

Testin reads the folders again and draws the tree from what it finds.

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
- **Rule-TREE-PANEL-081** — Refresh checks again which test project this code
  project uses, first. So a test project changed by hand, or changed by a branch
  switch, is picked up.
- **Rule-TREE-PANEL-082** — Editors on a node that no longer exists are closed.
  The other editors are reloaded, unless a tester is in the middle of something.
- **Rule-TREE-PANEL-083** — Only one refresh runs at a time. A second request
  while one is running waits for it, and runs once it is done. A branch switch
  is one of those requests: the branch is already checked out by the time it
  asks, so dropping it would leave the files on disk and the index belonging to
  different branches. Two waiting requests are one refresh, reporting whichever
  arrived last.
- **Rule-TREE-PANEL-100** — A test project that is not active is shown in the
  tree and holds nothing. It is indexed as a node so the tree can say what it
  is, drawn with Inactive beside its name like any other status. Its test sets,
  test cases and test runs are not read, because a project nobody is working on is
  not worth the walk.
- **Rule-TREE-PANEL-104** — A menu entry that cannot work on the selected row is
  gray, and says why when the pointer rests on it.

## What the tester sees

Refresh opens no screen of its own. A progress bar reads *Testin indexing -
\<project\>* while Testin reads the test project. The tree then redraws, with
the same rows open as before. *Refreshed* shows above the status bar at the
bottom right of the IDE.

## Main flow

1. The tester presses the **Refresh** button in the panel header.
2. Testin checks again which test project this code project uses.
3. Testin reads the test project again, showing a progress bar reading *Testin
   indexing - \<project\>*.
4. Editors on nodes that are gone are closed, and the rest are reloaded.
5. The tree redraws, with the same rows expanded.
6. Testin shows *Refreshed* when it finishes.
7. In the background, Testin asks GitHub again for the state of every bug the
   test project holds, and the view panel shows them beside their links
   ([Rule-VIEW-PANEL-092](../viewPanel/readRunItem.md)).

## What Testin refuses

**If a refresh is already running** — the request waits, and runs when that one
finishes. Pressing Refresh twice is one refresh, because nothing changed by
pressing it again; a branch switch that lands in that window is not lost.

**If Testin cannot read the test project again** — a *Refresh Failed*
notification says what went wrong. The tree stays as it was, and the button
works again.

---

[Documentation](../README.md) › [The tree panel](main.md)
