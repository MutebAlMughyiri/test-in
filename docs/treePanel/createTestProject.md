[Documentation](../README.md) › [The tree panel](main.md) › UC-TREE-PANEL-002

# UC-TREE-PANEL-002: Create a test project

> **No key.** Press **New Test Project** at the top of the panel, or the
> welcome link **Create your first test project**.

**As a** tester, **I want** to create a test project by name, **so that** a new
product under test has a place before any test is written.

A test project is the folder that holds all the test work for one product.

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
- **Rule-TREE-PANEL-016** — A test project is a folder directly under the Testin
  folder. Any other folder there is ignored.
- **Rule-TREE-PANEL-017** — Creating a test project binds this code project to
  it.
- **Rule-TREE-PANEL-100** — A test project that is not active is shown in the
  tree and holds nothing. It is indexed as a node so the tree can say what it
  is, drawn with Inactive beside its name like any other status. Its test sets,
  test cases and test runs are not read, because a project nobody is working on is
  not worth the walk.
- **Rule-TREE-PANEL-104** — A menu entry that cannot work on the selected row is
  gray, and says why when the pointer rests on it.

## The Create Test Project dialog

```
┌──────────────────────────────────────────────────────────────┐
│  Create Test Project                                         │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│  [tp]  set name or paste url...                         (1)  │
│                                                              │
├──────────────────────────────────────────────────────────────┤
│  Enter Confirm    Escape Cancel                         (2)  │
└──────────────────────────────────────────────────────────────┘
```

1. **One field** — its gray hint text reads *set name or paste url...*. What the
   tester types decides what happens. A Git address is cloned, which is
   [UC-TREE-PANEL-003](importTestProject.md). Anything else is the name of a new
   test project. There is no list to pick from. A list would only ask the
   tester to repeat what they just typed.
2. **The status bar** — every key this dialog answers to.

The **New Test Project** button opens it. So does the welcome link **Create your
first test project**. That link is drawn under
[UC-TREE-PANEL-001](reachTheTree.md).

## Main flow

1. The tester presses the **New Test Project** button in the panel header, or
   the welcome link **Create your first test project**.
2. The tester types a name and presses `Enter`.
3. Testin creates the test project folder in the Testin folder.
4. Testin chooses it for this code project on this machine, and the tree
   appears. Nothing is written into the code project (Rule-TREE-PANEL-106).
5. Testin shows *Created*.

To copy a test project that already exists somewhere else, paste its address
instead of a name. That is [UC-TREE-PANEL-003](importTestProject.md).

## What Testin refuses

**If the name is empty** — the gray hint text turns red, and the dialog stays
open.

**If a folder with that name already exists in the Testin folder** — nothing is
created, and *\<name\> Already Exists* is shown in red.

**If the name cannot be a Java package** — the dialog stays open with the name
still in the box, and *'\<name\>' cannot name a Java package* is shown in red.
`New`, `Class` and `Import` are the usual ones: Java keeps those words for
itself. (Rule-TREE-PANEL-095) A repository address is never asked to be
one: the clone is named after its repository, and a repository name Java
refuses is made into one it accepts (Rule-TREE-PANEL-107).

**If no Testin folder is set** — the **New Test Project** button is gray, and
says *Set the Testin folder in Settings first.* (Rule-TREE-PANEL-115)

---

[Documentation](../README.md) › [The tree panel](main.md)
