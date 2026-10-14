[Documentation](../README.md) › [The tree panel](main.md) › UC-TREE-PANEL-014

# UC-TREE-PANEL-014: Copy nodes

> **`Ctrl+C`** copies and **`Ctrl+V`** pastes. Dragging with `Ctrl` held copies
> too. On the menu: **Actions → Copy** and **Paste**.

**As a** tester, **I want** to copy a node into another folder, **so that** I
can start from something that already exists instead of writing it again.

The copy is new and separate. Changing it never changes the original.

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
- **Rule-TREE-PANEL-051** — A copied test case is a new test case, with its own
  UUID. Editing the copy never changes the original.
- **Rule-TREE-PANEL-052** — A copy cannot be undone. To take one back, remove
  it, which is [UC-TREE-PANEL-012](removeNode.md).
- **Rule-TREE-PANEL-053** — A copy carries everything the original had, as well
  as its test cases: its order number, and its **Deprecated** or **Archived**
  status. A copy of a retired test set is retired too. A copied test run keeps
  the results the original recorded.
- **Rule-TREE-PANEL-100** — A test project that is not active is shown in the
  tree and holds nothing. It is indexed as a node so the tree can say what it
  is, drawn with Inactive beside its name like any other status. Its test sets,
  test cases and test runs are not read, because a project nobody is working on is
  not worth the walk.
- **Rule-TREE-PANEL-104** — A menu entry that cannot work on the selected row is
  gray, and says why when the pointer rests on it.

Rule-TREE-PANEL-043, Rule-TREE-PANEL-044, Rule-TREE-PANEL-045 and
Rule-TREE-PANEL-049 hold here too. They say where a node can land, and they are
on [UC-TREE-PANEL-013](moveNodes.md).

## What the tester sees

Nothing in the tree changes when the tester copies. The nodes stay black, and *Copied* shows above the status bar at the
bottom right of the IDE. Pasting then
opens the dialog drawn under [UC-TREE-PANEL-013](moveNodes.md). Its title reads **Paste**, and it asks *Copy N nodes
into '\<folder\>'?*.

## Main flow

1. The tester selects one or more test sets, packages or test runs.
2. The tester presses `Ctrl+C`, or chooses **Actions → Copy**. Nothing in the
   tree changes, and Testin shows *Copied*, or *Copied N* for several.
3. The tester selects a folder that can hold them.
4. The tester presses `Ctrl+V`, or chooses **Actions → Paste**.
5. The **Paste** dialog asks what will be copied, and where to.
6. The tester presses `Enter`. The nodes are duplicated, and Testin shows *Pasted*, or *Pasted N*.
7. Every test case in the copy gets its own UUID, and the copy is selected in the
   tree.

**By dragging.** The tester holds `Ctrl` while releasing a dragged node. The
dialog is then titled **Copy**, and `Enter` copies instead of moving.

## What Testin refuses

The same refusals as a move, and they are on
[UC-TREE-PANEL-013](moveNodes.md#what-testin-refuses).

**If the copy fails on disk** — an IDE notification titled *Copy Failed* stays
in the notification log, with the reason under it.

**If one copied test case cannot be given its new identity** — its file under
the new UUID cannot be written — the file it was copied as is kept rather than
removed, and the writer says that writing it failed. That file still carries the
original's identity, so the next scan reports the two as one test case in two
files, which the tester can resolve; removing it would have left the copy one
test case short with nothing said (Rule-TREE-PANEL-051).

**If the Java plugin is installed** — new automation code is written for the
copy. Without the plugin, the copy has none. The first copy in the project then
says *Java Plugin Not Available*.

---

[Documentation](../README.md) › [The tree panel](main.md)
