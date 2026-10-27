[Documentation](../README.md) › [The tree panel](main.md) › UC-TREE-PANEL-022

# UC-TREE-PANEL-022: Edit a test run

> **`F2`** in the tree, or **Edit Test Run** on the menu. Saving is final:
> **`Ctrl+Z`** does not take it back.
>
> `F2` changes the thing in front of the tester, and which thing that is depends
> on where they are standing: Update Test Case answers it in the test case
> editor, Failed Test Case Details in the test run editor, and this one in the
> tree. Each is gray where the others answer, so only one can ever take the key.

**As a** tester, **I want** to change which test cases a test run covers, its
name and its configuration, **so that** a test run can be corrected without
being recreated.

This changes a test run that is not signed off yet.

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
- **Rule-TREE-PANEL-073** — A signed-off test run cannot be edited. (Rule-TREE-PANEL-009)
- **Rule-TREE-PANEL-074** — Removing a test case from a test run drops
  everything that test case recorded in that test run. Adding a test case adds
  it as **Pending**.
- **Rule-TREE-PANEL-076** — A test case this dialog does not show is not one the
  tester chose to remove, and saving the dialog never removes it. A test case in
  a test set deprecated, or under a package archived, since the test run was
  made is shown gray and its box cannot be changed: one the test run covers
  stays ticked, so what the test run recorded about it is kept. (Rule-TREE-PANEL-008)
- **Rule-TREE-PANEL-093** — The dialog opens with every row saying what the test
  run actually covers, folders included. A folder is ticked only when everything
  under it is ticked, so a folder is never ticked over test cases the test run
  does not cover.
- **Rule-TREE-PANEL-100** — A test project that is not active is shown in the
  tree and holds nothing. It is indexed as a node so the tree can say what it
  is, drawn with Inactive beside its name like any other status. Its test sets,
  test cases and test runs are not read, because a project nobody is working on is
  not worth the walk.
- **Rule-TREE-PANEL-104** — A menu entry that cannot work on the selected row is
  gray, and says why when the pointer rests on it.
- **Rule-TREE-PANEL-128** — Saving changes the test run as it is when the save
  lands, not as it was when the dialog opened. A run item status that arrived
  while the dialog was open, from a pull or from the test run being worked on,
  is kept.
- **Rule-TREE-PANEL-134** — Edit Test Run lists the run items whose test case
  was deleted under a Deleted test cases folder, ticked; unticking one and
  saving removes it from the test run.
- **Rule-TREE-PANEL-139** — A **Disabled** test case is shown grayed and its box
  cannot be changed. One the test run already covers stays in it, so editing
  never throws away what it recorded; one it does not cover cannot be added.

## What the tester sees

The **Edit Test Run** dialog opens. It is the dialog drawn under
[UC-TREE-PANEL-009](createTestRun.md), with two differences. Its title reads **Edit Test Run**, and its button reads
**Save**. It arrives filled in with the
test run's own name, its test cases and its configuration. Its **Filter** works
the same way, and a test case the filter hides is not taken out of the test run (Rule-TREE-PANEL-130). After **Save**,
the
tree refreshes and *Updated* shows above the status bar at the bottom right of
the IDE.

## Main flow

1. The tester selects a test run that is not signed off.
2. The tester chooses **Edit Test Run**.
3. The **Edit Test Run** dialog opens. It shows the test run's name, its test
   cases already ticked, and its configuration already answered. Test cases
   added to a test set since the test run was created appear unticked. A test run
   created before a row had to be answered holds nothing for it, and that row
   opens with no answer picked. (Rule-TREE-PANEL-121)
4. The tester changes any of it and presses **Save**.
5. If the name was changed, Testin closes the test run's editor first. It then
   renames the folder, and only then saves the test run.
6. Testin removes any test case the tester unticked, with everything the test
   run recorded about it. A newly ticked test case is added as **Pending**.
7. The tree refreshes, and Testin shows *Updated*.
8. The edit is final. It is not put on the tree's history, so `Ctrl+Z` does not
   take it back (Rule-TREE-PANEL-060).

## What Testin refuses

**If the test run is Completed or Closed** — **Edit Test Run** is gray.

**If the test run was signed off from its editor while the dialog was open** —
the dialog stays open, and *'\<test run\>' was Completed while this was open -
nothing saved* is shown in red.

**If the test run was removed while the dialog was open** — *'\<test run\>' no
longer exists - nothing saved* is shown in red.

**If the name is emptied** — *A test run needs a name* is shown in red, and the
dialog stays open.

**If the name is a path** — a name holding a slash, a backslash or `..` is
refused, reading *'\<name\>' is a path, not a name - a node is one folder inside
the one selected*. The dialog stays open, and nothing is saved.
(Rule-TREE-PANEL-095)

**If the new name is already used by a sibling** — *\<name\> Already Exists* is
shown in red, and the dialog stays open. Keeping the test run's own name is not
a clash.

**If the last test case is unticked** — **Save** is disabled. **Create** does
the same on an empty new test run.

**If a row on the form has no answer** — **Save** is disabled, and *Pick a
\<field\>* beside it names the first row that has none. The test run's own file
is not touched until **Save**, so a test run left unanswered is left exactly as
it was. (Rule-TREE-PANEL-121)

**If several rows are selected** — **Edit Test Run** is gray. It needs exactly one.

---

[Documentation](../README.md) › [The tree panel](main.md)
