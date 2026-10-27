[Documentation](../README.md) › [The tree panel](main.md) › UC-TREE-PANEL-007

# UC-TREE-PANEL-007: Create a test set

> Right-click **Test Cases** or a test set package, choose **Create**, then pick
> *Test Set*. There is no key for this.

**As a** tester, **I want** to add a test set under **Test Cases** or under a
package, **so that** the test cases I am about to write have somewhere to live.

A test set is the folder that holds test cases. This makes one.

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
- **Rule-TREE-PANEL-024** — Under **Test Cases** or a test set package, only a
  test set or a test set package can be created. The dialog offers nothing else.
- **Rule-TREE-PANEL-025** — Nothing can be created directly under the test
  project, under a test set, or under a test run.
- **Rule-TREE-PANEL-026** — A new test set opens in its editor at once. Its
  automation code is written where the Java plugin allows it.
- **Rule-TREE-PANEL-096** — Create Testin Node is gray on a node that cannot
  hold one, and the entry says which node kinds can. A test project, a test set
  and a test run hold what they hold; the two directories under a test project
  and the two package kinds are what a new node is created under.
- **Rule-TREE-PANEL-100** — A test project that is not active is shown in the
  tree and holds nothing. It is indexed as a node so the tree can say what it
  is, drawn with Inactive beside its name like any other status. Its test sets,
  test cases and test runs are not read, because a project nobody is working on is
  not worth the walk.
- **Rule-TREE-PANEL-104** — A menu entry that cannot work on the selected row is
  gray, and says why when the pointer rests on it.

## The Create Test Node dialog

```
┌──────────────────────────────────────────────────────────────┐
│  Create Test Node                                            │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│  [set]  set name...                                     (1)  │
│                                                              │
│  [set]  Test Set          Holds test cases              (2)  │
│  [pkg]  Test Set Package  Groups test sets                   │
│                                                              │
├──────────────────────────────────────────────────────────────┤
│  Enter Confirm    ↑ ↓ Select    Escape Cancel           (3)  │
└──────────────────────────────────────────────────────────────┘
```

1. **The name** — its gray hint text reads *set name...*. If the tester presses
   `Enter` with the box empty, the hint turns red and the cursor stays here.
2. **The two kinds** — each with a hint beside it. `↑` and `↓` move between
   them. The icon at the front of the name box changes to match. Clicking a row
   confirms straight away.
3. **The status bar** — every key this dialog answers to.

**Create Test Run Node** is the same dialog on the test run side. Two things
differ. Its kinds are *Test Run* (*Records execution results*) and *Test Run
Package* (*Groups test runs*). Its hint text reads *set name, like Sprint 3
Cycle 1...*.

## Main flow

1. The tester selects **Test Cases** or a test set package.
2. The tester right-clicks it and chooses **Create**.
3. The **Create Test Node** dialog opens. Its first row is selected, and reads *Test Set*, with the hint *Holds test
   cases* beside it.
4. The tester types a name and presses `Enter`.
5. Testin creates the test set under the selected parent, refreshes the tree,
   and shows *Created*.
6. The test set opens in its editor at once.
7. Where the Java plugin is installed, Testin also writes its automation code.

To group test sets instead of adding one, see
[UC-TREE-PANEL-008](createTestSetPackage.md).

## What Testin refuses

**If the name is empty** — the dialog stays open, the gray hint text turns red,
and the cursor stays in the box.

**If the name is a path** - a name holding a slash, a backslash or `..` is
refused, reading *is a path, not a name - a node is one folder inside the one
selected*. Nothing is created.

**If a node with that name already exists under the parent** — the dialog
closes, nothing is created, and *\<name\> Already Exists* is shown in red.

**If the test project, a test set or a test run is selected** — **Create** is
gray. The entry itself reads **Create Testin Node (a test set holds what it holds)**, naming
the node that
cannot take a child. A popup never shows a gray entry's description, so the
reason is in the entry.

**If the IDE project has no test source folder** — the Testin Help mark turns
red: *This project has no test source folder, so* the file *was not created*.
The test set is still created.

**If the Java plugin is not installed** — *Java Plugin Not Available* says that
automation code and navigation need it. Testin says this once per project. The
test set is still created.

**If several rows are selected** — **Create** stays black and creates under the
first of them.

**If the selected package is retired** — **Create** is not gray. The test set is
created inside an **Archived** package. No test run will ever be offered it
there.

---

[Documentation](../README.md) › [The tree panel](main.md)
