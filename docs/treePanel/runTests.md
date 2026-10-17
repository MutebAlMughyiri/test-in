[Documentation](../README.md) › [The tree panel](main.md) › UC-TREE-PANEL-023

# UC-TREE-PANEL-023: Run the automation for everything a node holds

> **No key.** On the menu: **Run Tests**.

**As a** tester, **I want** to run every automated test case under a test set, a
package or **Test Cases**, **so that** a whole area runs in one gesture.

Testin hands the test cases to the IDE's own test runner.

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
- **Rule-TREE-PANEL-077** — Running from a parent skips retired branches.
  Running a retired test set directly still runs it. (Rule-TREE-PANEL-008)
- **Rule-TREE-PANEL-078** — A node with no test cases to run says so. It runs
  nothing.
- **Rule-TREE-PANEL-079** — Running needs the TestNG plugin. Without it, the
  item stays on the menu, grayed, reading *(needs the TestNG plugin)*.
- **Rule-TREE-PANEL-080** — **Run Tests** is offered on **Test Cases**, a test
  set package, a test set and a test run. It is not offered on the test project
  row, on **Test Runs**, or on a test run package.
- **Rule-TREE-PANEL-100** — A test project that is not active is shown in the
  tree and holds nothing. It is indexed as a node so the tree can say what it
  is, drawn with Inactive beside its name like any other status. Its test sets,
  test cases and test runs are not read, because a project nobody is working on is
  not worth the walk.
- **Rule-TREE-PANEL-104** — A menu entry that cannot work on the selected row is
  gray, and says why when the pointer rests on it.
- **Rule-TREE-PANEL-141** — Run Tests on a test run is gray, and says why, while
  that test run's Start Manual Execution is on or its Run Tests is already executing
  (Rule-EDITOR-PANEL-290).

## What the tester sees

Running opens no screen of Testin's own. *Running*, or *Running N*, shows above
the status bar at the bottom right of the IDE. The IDE's own Run window then
opens at the bottom, on a test configuration named after the generated class.

## Main flow

**From a folder of test cases**

1. The TestNG plugin is installed.
2. The tester selects **Test Cases**, a test set package or a test set.
3. The tester chooses **Run Tests**.
4. Every test case under the node runs. Retired branches under it are skipped.
   A retired node the tester selects and runs directly still runs everything in
   it.
5. Testin shows *Running* for one test case, or *Running N*, where N is how many
   actually started. Test cases already running are not counted.
6. The IDE's own Run window opens. Its test configuration is named after the
   generated class. Where the selection spans several classes, the name reads *\<class\> and N more*.

**From a test run**

1. The tester selects a test run and chooses **Run Tests**.
2. Its test run editor opens, or comes forward if it is already open.
3. Every test case in that test run with a generated method is run, judged or not (Rule-EDITOR-PANEL-184).

## What Testin refuses

**If the node holds no test case that can run** — nothing runs, and *\<name\>
has no test cases to run* is shown in red.

**If the test run's Start Manual Execution is on, or its Run Tests is executing** — the
entry is gray, and says *Start Manual Execution is on; stop it first* or *Run Tests is
executing; stop it first* (Rule-TREE-PANEL-141).

**If a test case has no generated automation code** — it is dropped, and Testin
names it: *\<test case\> has no generated code yet*. The rest still run. If none
of them can run, nothing starts.

**If the IDE is indexing** — nothing runs, and the IDE says *Tests cannot run
while the IDE is indexing. Wait a moment and run them again.* If indexing starts
while the execution is being prepared, it says *Indexing interrupted the test
run. Run it again.*

**If every test case under the node was already running** — nothing starts, and
Testin says nothing at all.

**If the TestNG plugin is not installed** — **Run Tests** is still in the menu,
grayed, reading *(needs the TestNG plugin)*.

---

[Documentation](../README.md) › [The tree panel](main.md)
