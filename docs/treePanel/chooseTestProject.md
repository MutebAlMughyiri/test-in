[Documentation](../README.md) › [The tree panel](main.md) › UC-TREE-PANEL-004

# UC-TREE-PANEL-004: Choose which test project this code project uses

> **No key.** Press **Select Test Project** at the top of the panel.

**As a** tester, **I want** to point this code project at a different test
project in the same Testin folder, **so that** one machine can serve several
products.

One code project uses one test project. This is how the tester changes which
one.

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
- **Rule-TREE-PANEL-106** — The choice is kept on this machine and never
  written into the code project. It wins over the test project `testin.yml`
  names until the file names a different one, so the tester is never held by
  the file and a colleague's change to it still arrives. That holds whichever
  way the choice was made: the picker, creating a test project, cloning one, or
  the welcome screen.

## The Select Test Project dialog

```
┌──────────────────────────────────────────────────────────────┐
│  Select Test Project                                         │
├──────────────────────────────────────────────────────────────┤
│  Test Project                         Status            (1)  │
│  Demo                                 Active            (2)  │
│  Mobile                               Active                 │
│  Legacy                               Inactive               │
├──────────────────────────────────────────────────────────────┤
│  Enter Select    Escape Cancel                          (3)  │
└──────────────────────────────────────────────────────────────┘
```

1. **Two columns**, headed **Test Project** and **Status**.
2. **One row per test project** in the Testin folder, whatever its status. The
   one this code project already uses is selected.
3. **The status bar** — every key this dialog answers to. A click selects a
   row, but it never confirms. Only `Enter` confirms.

## Main flow

1. The tester presses the **Select Test Project** button in the panel header.
2. The **Select Test Project** dialog lists every test project in the Testin
   folder, with its status. The current one is selected.
3. The tester selects one and presses `Enter`.
4. Testin keeps the choice on this machine. Nothing is written into the code
   project.
5. The tree reloads on that test project.
6. Testin shows *Bound*.

## What Testin refuses

**If no test project folder exists in the Testin folder** — no dialog opens, and
the message *No Test Projects*, with the line *Create one in the Testin folder
first*, is shown in red.

**If no row is selected** — `Enter` does nothing, and says nothing. That happens
when nothing is bound yet, or when the bound name matches no row.

**If the chosen test project is Inactive** — Testin binds to it and says *Bound*
all the same, and the tree shows it with **Inactive** beside its name. It holds
nothing: a project nobody is working on is not read (Rule-TREE-PANEL-100).

**If `testin.yml` names a project** — the choice wins over it all the same, and
keeps winning until the file names a different project than it did when the
choice was made (Rule-TREE-PANEL-106).

**If `testin.yml` is edited by hand** — the tree does not notice. Testin reads
that file when the project opens. After that it reads the file again only when
the tester presses **Refresh** or **Save to testin.yml**, and before **Report
Bug** sends.

---

[Documentation](../README.md) › [The tree panel](main.md)
