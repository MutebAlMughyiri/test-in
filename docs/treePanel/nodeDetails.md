[Documentation](../README.md) › [The tree panel](main.md) › UC-TREE-PANEL-027

# UC-TREE-PANEL-027: See what a node holds

> **No key.** On the menu: **Details**.

**As a** tester, **I want** to see a node's counts, dates, status and run item
status breakdown without opening anything, **so that** I can see how big a part
of the tree is at a glance.

It is a read-only window. Nothing in it can be changed.

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
  holds whether the node is created, renamed, pasted or dropped.
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
- **Rule-TREE-PANEL-087** — Opening **Details** changes nothing, so Testin says
  nothing. (Rule-TREE-PANEL-007)
- **Rule-TREE-PANEL-088** — Testin counts what a node holds when the tester
  asks. It never saves the number.
- **Rule-TREE-PANEL-100** — A test project that is not active is shown in the
  tree and holds nothing. It is indexed as a node so the tree can say what it
  is, drawn with Inactive beside its name like any other status. Its test sets,
  test cases and test runs are not read, because a project nobody is working on is
  not worth the walk.
- **Rule-TREE-PANEL-104** — A menu entry that cannot work on the selected row is
  gray, and says why when the pointer rests on it.
- **Rule-TREE-PANEL-125** — A test set's **Updated** row changes whenever one of
  its test cases is added, saved, moved in or out, reordered or removed, and
  names the tester who did it.
- **Rule-TREE-PANEL-126** — Every run item status that at least one test case
  carries takes at least a sliver of the ring, so a single failure among a
  thousand passes is still there to see.
- **Rule-TREE-PANEL-137** — The Details dialog closes on Escape or on a click
  anywhere outside it: it only reads, so there is nothing to lose.

## The Details dialog

```
┌──────────────────────────────────────────────────────────────┐
│  Details                                                     │
├──────────────────────────────────────────────────────────────┤
│  NAME                                                   (1)  │
│  cycle-2                                                     │
│  PATH                                                        │
│  C:\Testin\Demo\Test Runs\cycle-2                            │
│  CREATED BY                                             (2)  │
│  Muteb                                                       │
│  CREATED AT                                                  │
│  12 Aug 2026 09:14                                           │
│  UPDATED BY                                                  │
│  Muteb                                                       │
│  UPDATED AT                                                  │
│  14 Aug 2026 16:02                                           │
│  STATUS                                                 (3)  │
│  In Progress                                                 │
│  EXECUTION STARTED                                      (4)  │
│  12 Aug 2026 10:00                                           │
│  EXECUTION ENDED                                             │
│  12 Aug 2026 13:42                                           │
│  EXECUTION TIME                                              │
│  03:42:00                                                    │
│  PLATFORM                                               (5)  │
│  Web                                                         │
│  COMPONENT                                                   │
│  Frontend                                                    │
│  TOTAL                                                  (6)  │
│  14                                                          │
│                                                              │
│      ╭───╮         Passed     9                         (7)  │
│      │75%│         Failed     2                              │
│      ╰───╯         Blocked    1                              │
│                    Untested   2                              │
│                    Removed    0                              │
├──────────────────────────────────────────────────────────────┤
│  Escape Close                                           (8)  │
└──────────────────────────────────────────────────────────────┘
```

1. **Name and Path** — **Path** is the node's full path on disk.
2. **Who and when** — who created the node, who last changed it, and the dates.
3. **Status** — left out on **Test Cases** and **Test Runs**, which have none.
4. **The execution rows** — **Execution Started**, **Execution Ended** and **Execution Time**, which is how long the
   test run took. Only a test run has
   them, and a test run that never started shows none of them. A test run that
   started and has not ended shows the first alone: there is no length yet.
5. **The settings** — every answer the tester gave when the test run was made.
   Only a test run has them.
6. **The counts** — what the node holds. Which counts appear depends on the
   kind of node. The table under **Main flow** says which.
7. **The run item status chart** — a ring with the pass rate inside it, and the
   five run item statuses beside it. Each run item status has a color and a
   count. Only a test run has this chart.
8. **The status bar** — `Escape` closes the dialog.

A row with nothing in it is not drawn. So a test set shows **Name**, **Path**,
the four rows about who and when, **Status** and **Test cases**, and nothing
else.

## Main flow

1. The tester selects one node. With several selected, **Details** is still
   offered, and shows the first of them.
2. The tester chooses **Details**.
3. The **Details** dialog opens, titled **Details**, with `Escape Close` on its
   status bar.
4. It shows **Name** and **Path**. **Path** is the node's full path on disk.
5. It shows **Created By**, **Created At**, **Updated By** and **Updated At**.
6. It shows **Status**, except on **Test Cases** and **Test Runs**, which have
   none.
7. It shows what the node holds. The counts are worked out when the tester
   asks, and never saved. Which counts appear depends on the kind of node:

   | Node             | Counts                                                     |
      |------------------|------------------------------------------------------------|
   | Test project     | **Test sets**, **Packages**, **Test cases**, **Test runs** |
   | **Test Cases**   | **Test sets**, **Packages**, **Test cases**                |
   | **Test Runs**    | **Packages**, **Test runs**                                |
   | Test set package | **Test sets**, **Packages**, **Test cases**                |
   | Test run package | **Packages**, **Test runs**                                |
   | Test set         | **Test cases**                                             |
   | Test run         | **Total**                                                  |

8. **A test run, and only a test run**, also shows a run item status chart. It
   is a ring, with **Passed**, **Failed**, **Blocked**, **Untested** and **Removed**
   listed beside it. Each one has a color and a count. Inside the ring is the
   pass rate. It reads **Not run** when nothing in the test run has been
   executed.
9. A test run also shows **Execution Started**, **Execution Ended** and **Execution Time**. It shows every setting the
   tester gave when the test run
   was made: **Test Type**, **Change Log**, **Commit**, **Platform**, **Component**, **Language**, **Browser** and
   **Device Type**.
10. A row with nothing in it is not drawn at all. A test run that never started
    shows none of the three execution rows, and one still running shows only
    the first.
11. The tester presses `Escape`. It closes. Nothing was changed, and nothing is
    announced.

## What Testin refuses

**Nothing.** **Details** is never gray, and it opens on every kind of node. With
several rows selected, it opens on the first of them and says nothing about the
rest.

---

[Documentation](../README.md) › [The tree panel](main.md)
