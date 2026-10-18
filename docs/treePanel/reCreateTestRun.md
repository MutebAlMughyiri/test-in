[Documentation](../README.md) › [The tree panel](main.md) › UC-TREE-PANEL-021

# UC-TREE-PANEL-021: Re-create a test run

> **No key.** On the menu: **Actions → Re-Create Test Run**.

**As a** tester, **I want** to make the next cycle from a finished test run, **so that** starting the next round of
testing takes one step. The new one has
the same test cases and settings and no run item statuses, so nobody builds the
whole test run again by hand.

This copies a test run's test cases and settings into a new, empty one.

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
- **Rule-TREE-PANEL-069** — Re-create works on a test run in any status,
  including a signed-off one. That is what it is for.
- **Rule-TREE-PANEL-070** — Only the test cases and the configuration are
  carried over. Run item statuses, durations and failure details start fresh.
- **Rule-TREE-PANEL-071** — The next name is suggested by counting up. *cycle-1*
  becomes *cycle-2*, and a name with no number on it gets one: *smoke* becomes *smoke-2*. A name already taken is
  skipped.
- **Rule-TREE-PANEL-072** — The new test run is created in the same folder as
  the one it was made from.
- **Rule-TREE-PANEL-100** — A test project that is not active is shown in the
  tree and holds nothing. It is indexed as a node so the tree can say what it
  is, drawn with Inactive beside its name like any other status. Its test sets,
  test cases and test runs are not read, because a project nobody is working on is
  not worth the walk.
- **Rule-TREE-PANEL-104** — A menu entry that cannot work on the selected row is
  gray, and says why when the pointer rests on it.

## What the tester sees

The **Create Test Run** dialog opens. It is the dialog drawn under
[UC-TREE-PANEL-009](createTestRun.md), with the same title and the same **Create** button. It arrives filled in. The
next name is in *Test Run name*,
the same test cases are ticked, and the same configuration is set. After **Create**, the new test run's editor opens and
*Created* shows above the status
bar at the bottom right of the IDE.

## Main flow

1. The tester selects a test run in any status.
2. The tester chooses **Actions → Re-Create Test Run**.
3. The **Create Test Run** dialog opens, with the next name suggested: *cycle-1*
   becomes *cycle-2*. The same test cases are ticked, and the same configuration
   is filled in.
4. A test case removed from its test set since the last test run is simply not
   there.
5. The tester presses **Create**.
6. Testin writes a new test run, with every ticked test case **Pending**.
7. Its editor opens, and Testin shows *Created*.

## What Testin refuses

These refusals apply as when a test run is created.

**If no test case is ticked** — the **Create** button is disabled.

**If a row on the form has no answer** — the **Create** button is disabled, and
hovering over it reads *Pick a \<field\>*, naming the first row that has none. (Rule-TREE-PANEL-121)

**If the name is empty** — *A test run needs a name* is shown, and the dialog
stays open with everything typed still in it.

**If the name is a path** — a name holding a slash, a backslash or `..` is
refused, reading *'\<name\>' is a path, not a name - a node is one folder inside
the one selected*. The dialog stays open with everything typed still in it.
(Rule-TREE-PANEL-095)

**If the name is already used** — *\<name\> Already Exists* is shown, and the
dialog stays open with everything typed still in it.

**If the folder was removed while the dialog was open** — *'\<parent\>' no
longer exists - test run not created* is shown in red.

**If several rows are selected** — **Re-create** is gray. It needs exactly one.

---

[Documentation](../README.md) › [The tree panel](main.md)
