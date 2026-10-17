[Documentation](../README.md) › [The tree panel](main.md) › UC-TREE-PANEL-018

# UC-TREE-PANEL-018: Retire a test project, a test set or a package

> **No key.** On the menu: **Actions**, then the status. A test project has
> **Deactivate**. A test set has **Mark Deprecated**. A package has **Archive**.

**As a** tester, **I want** to mark old work retired, **so that** it stays for
its history without getting in the way of what I am testing now.

Retiring deletes nothing. It only moves the node out of the way.

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
- **Rule-TREE-PANEL-062** — Retiring deletes nothing. (Rule-TREE-PANEL-008)
- **Rule-TREE-PANEL-063** — A test project that is not **Active** shows nothing
  under it.
- **Rule-TREE-PANEL-064** — An **Inactive** test project is still opened, and
  still bound: retiring it does not let go of it. What it holds is
  Rule-TREE-PANEL-100's. Until #66 there was an **Archived** status as well, which
  was not opened at all.
- **Rule-TREE-PANEL-065** — A status is set on one node at a time. The status a
  node already has is offered gray, so the tester can see what it is now.
- **Rule-TREE-PANEL-100** — A test project that is not active is shown in the
  tree and holds nothing. It is indexed as a node so the tree can say what it
  is, drawn with Inactive beside its name like any other status. Its test sets,
  test cases and test runs are not read, because a project nobody is working on is
  not worth the walk.
- **Rule-TREE-PANEL-104** — A menu entry that cannot work on the selected row is
  gray, and says why when the pointer rests on it.

## What the tester sees

Retiring opens no screen of its own, and it asks no question. A retired test set
or package turns gray at once and moves to the bottom of its folder. A test
project that is not **Active** shows no children instead. The new status word
shows above the status bar at the bottom right of the IDE. It reads *Inactive*, *Archived* or *Deprecated*.

## Main flow

1. The tester selects exactly one node.
2. The tester opens **Actions**. It shows the status entries for that node's
   kind. The status the node already has is gray.

   | Node         | Retire it with      | Statuses it can have |
      |--------------|---------------------|----------------------|
   | Test project | **Deactivate**      | Active, Inactive     |
   | Test set     | **Mark Deprecated** | Active, Deprecated   |
   | Package      | **Archive**         | Active, Archived     |

3. The tester chooses one.
4. Testin writes the status, refreshes the tree, and shows the new status word:
   *Inactive*, *Archived* or *Deprecated*.

   A test project takes **Deactivate** and nothing else. Until #66 it took **Archive** as well, which put it aside the
   same way and asked the tester to
   tell two kinds of "not now" apart.

**What retiring does.** A **Deprecated** test set or an **Archived** package is
drawn gray, and it sorts last. **Expand All** leaves it closed. It is not
offered when a test run is created. Its test cases are skipped when a parent is
run. Nothing inside it is deleted.

**What an inactive test project does.** It is still bound, still drawn in the
tree, and still says **Inactive** beside its name. What it does not do is hold
anything: its row shows no children, because a project nobody is working on is
not read (Rule-TREE-PANEL-100).

To bring one back, see [UC-TREE-PANEL-019](reactivateNode.md).

## What Testin refuses

**If the status could not be written** — Testin says *Unable to update status to
\<status\>*, whichever kind of node it was, and nothing changes. One sentence for
the three, because what failed is the same thing each time and the node is in
front of the tester who asked.

**If more than one node is selected** — the status entries are not offered.

---

[Documentation](../README.md) › [The tree panel](main.md)
