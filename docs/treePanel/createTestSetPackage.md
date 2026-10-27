[Documentation](../README.md) › [The tree panel](main.md) › UC-TREE-PANEL-008

# UC-TREE-PANEL-008: Create a test set package

> Right-click **Test Cases** or another test set package, choose **Create**, then
> pick *Test Set Package*. There is no key for this.

**As a** tester, **I want** a folder to group test sets, **so that** a tree with
many test sets still reads the way the product is organized.

A test set package is a folder that holds test sets. This makes one.

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
- **Rule-TREE-PANEL-027** — A test set package is created empty. Nothing opens,
  and no automation code is written for it.
- **Rule-TREE-PANEL-028** — A test set package can hold another test set
  package, as deep as the tester needs.
- **Rule-TREE-PANEL-095** — A node name is one folder inside the one selected. A
  name carrying a slash, a backslash or `..` is a path, not a name, and is
  refused for every kind of node: it would put the folder wherever that path
  led. A folder that becomes a Java package also will not take a name Java
  refuses - New, Class, Import - whether it is being created or renamed. The
  tester is told which of the two they met, while they are still looking at what
  they typed, rather than finding a folder somewhere else or a package named
  something else.
- **Rule-TREE-PANEL-100** — A test project that is not active is shown in the
  tree and holds nothing. It is indexed as a node so the tree can say what it
  is, drawn with Inactive beside its name like any other status. Its test sets,
  test cases and test runs are not read, because a project nobody is working on is
  not worth the walk.
- **Rule-TREE-PANEL-104** — A menu entry that cannot work on the selected row is
  gray, and says why when the pointer rests on it.

Rule-TREE-PANEL-024 and Rule-TREE-PANEL-025 hold here too. They say what can be
created where, and they are on [UC-TREE-PANEL-007](createTestSet.md).

## What the tester sees

The **Create Test Node** dialog opens. It is the same dialog a test set uses,
and it is drawn under [UC-TREE-PANEL-007](createTestSet.md). The tester moves to
the second row, which reads *Groups test sets*. After `Enter`, a new folder row
appears in the tree. *Created* shows above the status bar at the bottom right of
the IDE. Nothing opens.

## Main flow

1. The tester selects **Test Cases** or another test set package.
2. The tester right-clicks it and chooses **Create**.
3. The **Create Test Node** dialog opens. The tester moves to *Test Set Package*
   with `↓`, and beside it the dialog says *Groups test sets*.
4. The tester types a name and presses `Enter`.
5. Testin creates the package under the selected parent, refreshes the tree, and
   shows *Created*.
6. Nothing opens. The package is empty until the tester puts something in it.

## What Testin refuses

**If the name is empty** — the dialog stays open, the gray hint text turns red,
and the cursor stays in the box.

**If the name is a path** - a name holding a slash, a backslash or `..` is
refused, reading *is a path, not a name - a node is one folder inside the one
selected*. Nothing is created.

**If a node with that name already exists under the parent** — the dialog
closes, nothing is created, and *\<name\> Already Exists* is shown in red.

**If the name cannot be a Java package** — the dialog stays open with the name
still in the box, and *'\<name\>' cannot name a Java package* is shown in red.
`New`, `Class` and `Import` are the usual ones: Java keeps those words for
itself. (Rule-TREE-PANEL-095)

**If the test project, a test set or a test run is selected** — **Create** is
gray. The entry itself reads **Create Testin Node (a test set holds what it holds)**, naming
the node that
cannot take a child. A popup never shows a gray entry's description, so the
reason is in the entry.

---

[Documentation](../README.md) › [The tree panel](main.md)
