[Documentation](../README.md) › [The tree panel](main.md) › UC-TREE-PANEL-003

# UC-TREE-PANEL-003: Import a test project that already exists

> **No key.** Press **New Test Project** at the top of the panel, and paste
> the address instead of a name.

**As a** tester, **I want** to bring a test project that already exists
somewhere else onto this machine, **so that** I can work on it without building
it again by hand.

This copies a test project from Git onto this machine.

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
  It sorts after live nodes. In Create Test Run and Edit Test Run it is shown
  gray, and no box in it can be changed.
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
- **Rule-TREE-PANEL-100** — A test project that is not active is shown in the
  tree and holds nothing. It is indexed as a node so the tree can say what it
  is, drawn with Inactive beside its name like any other status. Its test sets,
  test cases and test runs are not read, because a project nobody is working on is
  not worth the walk.
- **Rule-TREE-PANEL-104** — A menu entry that cannot work on the selected row is
  gray, and says why when the pointer rests on it.
- **Rule-TREE-PANEL-107** — Cloning needs the Git plugin, and nothing else: a
  code project clones a test project whether it has a `testin.yml` or not. The
  clone is named after its repository, the last part of the address without
  `.git` and without a trailing `-test-cases`, in capitals: *nafath-test-cases*
  becomes *NAFATH*. That name is kept as it is when Testin accepts it as a test project's
  name, made into one otherwise, and numbered when the Testin folder already
  has it. The one exception is the address `testin.yml` gives: that clone takes
  the name the file gives beside it.

## What the tester sees

The **Create Test Project** dialog opens. It is the same one-field dialog a new test
project uses, and it is drawn under
[UC-TREE-PANEL-002](createTestProject.md). While Git copies the repository, a
progress bar reads *Cloning repository*, with the line *Cloning into
\<name\>...* under it. The tree then appears, and *Cloned* shows above
the status bar at the bottom right of the IDE.

## Main flow

There is a second way in. The code project may name a test project that is not
on this machine. The panel then offers the link **Clone \<name\>**. That link is
drawn under [UC-TREE-PANEL-001](reachTheTree.md), and it does the same thing.

**From a Git address**

1. The Git plugin is installed. This code project needs no `testin.yml`.
2. The tester presses the **New Test Project** button in the panel header.
3. The tester pastes the repository address instead of a name, and presses
   `Enter`.
4. Testin copies the repository into the Testin folder. The new folder is
   named after the repository: `https://github.com/acme/nafath-test-cases.git`
   becomes `NAFATH`, and a second clone of it `NAFATH2`.
   A progress bar reads *Cloning repository*, with the line *Cloning into
   \<name\>...*. It cannot be canceled.
5. Testin chooses it for this code project on this machine, and the tree
   appears. Nothing is written into the code project (Rule-TREE-PANEL-106).
6. Testin shows *Cloned*. The name can be changed afterward with **Rename**
   ([UC-TREE-PANEL-011](renameNode.md)).

## What Testin refuses

**If the address is the one `testin.yml` gives, and a folder of the name it
gives is already in the Testin folder** — nothing is cloned, and *\<name\>
Already Exists* is shown in red. That folder is the one the file means.

**If the Git plugin is not installed** — nothing is cloned. Testin shows *Git
Plugin Not Available*, reading *Git synchronization and cloning require the Git
plugin, which is not available in this IDE.* Testin checks this before anything
else.

**If no Testin folder is set** — the **New Test Project** button is gray, and
says *Set the Testin folder in Settings first.* (Rule-TREE-PANEL-115)

---

[Documentation](../README.md) › [The tree panel](main.md)
