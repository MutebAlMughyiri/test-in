[Documentation](../README.md) › [The tree panel](main.md) › UC-TREE-PANEL-009

# UC-TREE-PANEL-009: Create a test run

> Right-click **Test Runs** or a test run package, choose **Create**, then pick
> *Test Run*. There is no key for this.

**As a** tester, **I want** to start a test run over the test cases I choose, **so that** a pass through the product is
recorded on its own.

A test run is one round of testing, with a run item status for each test case.

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
- **Rule-TREE-PANEL-029** — A test run needs at least one test case. It cannot
  be created empty.
- **Rule-TREE-PANEL-030** — Three things are not offered when a test run is
  created: a retired test set, anything under an **Archived** package, and an
  empty test set. (Rule-TREE-PANEL-008)
- **Rule-TREE-PANEL-031** — A new test run starts as **Created**. Every test
  case in it starts **Pending**.
- **Rule-TREE-PANEL-100** — A test project that is not active is shown in the
  tree and holds nothing. It is indexed as a node so the tree can say what it
  is, drawn with Inactive beside its name like any other status. Its test sets,
  test cases and test runs are not read, because a project nobody is working on is
  not worth the walk.
- **Rule-TREE-PANEL-104** — A menu entry that cannot work on the selected row is
  gray, and says why when the pointer rests on it.
- **Rule-TREE-PANEL-120** — A configuration row is shown only while the answers
  above it call for it: pick a kind of test run that needs no commit and the
  Commit row goes, and comes back the moment the answer changes. A row that
  is not shown is not asked, so what is saved holds nothing for it.
- **Rule-TREE-PANEL-121** — Every configuration row that is shown is answered
  before a test run is created. The answers a row offers are the only answers it
  has: nothing can be typed into one, and none of them offers a blank.
- **Rule-TREE-PANEL-122** — The change log takes as many lines as the tester
  needs: Ctrl+Enter inserts a line break, the same bargain a grid cell makes.
  Enter does nothing at all here - it neither breaks the line nor creates the
  test run - whichever field holds the keyboard.
- **Rule-TREE-PANEL-129** — The test case tree carries the editor's Filter
  button, and its menu is the editor's menu: a group, priority, module,
  automation or status filter picks the same test cases here as in an editor.
  Run Item Status is not on it: the tree holds test cases, and every run item of
  a new test run starts Pending. Test Set offers the test sets the tree holds.
- **Rule-TREE-PANEL-130** — A filter changes which test cases the tree shows,
  never which are ticked. A ticked test case the filter hides stays in the test
  run, and the line beside the button counts it as hidden.
- **Rule-TREE-PANEL-131** — Ticking a row while a filter is on ticks only the
  test cases it shows.
- **Rule-TREE-PANEL-138** — A **Disabled** test case is shown in the test cases
  grayed, with Disabled beside its description, and its box cannot be ticked, so
  a new test run never covers it. Ticking its test set leaves it out, and
  Re-create leaves out one the source test run covered. Its method carries
  enabled = false, so TestNG would never run it (Rule-CODEGEN-047).
- **Rule-TREE-PANEL-140** — Beside the filter, one button ticks every test case
  the filter shows, or unticks them all once every one of them is ticked, and
  its picture and tooltip say which it will do. A Disabled test case keeps its
  tick (Rule-TREE-PANEL-138), and a test case the filter hides keeps its own
  (Rule-TREE-PANEL-131).
- **Rule-TREE-PANEL-144** — While *Platform* is **Integration**, *Component* is
  **Backend**: **Frontend** is gray, and resting the pointer on it says an
  Integration test run covers the backend only. Picking **Web** or **Mobile**
  again lets **Frontend** be picked, and *Component* stays **Backend** until the
  tester changes it.
- **Rule-TREE-PANEL-145** — Between the tick button and the filter, one button
  opens and closes the tree's folders. While every folder is open it reads
  *Collapse All*, and closes them all, so only the folders right under
  **Test Cases** show; otherwise it reads *Expand All*, and opens them all. Its
  picture and tooltip say which it will do, and opening or closing a folder by
  hand changes it too. Opening and closing never tick or untick a test case. The
  tree opens fully expanded, and opens fully again when a filter changes what it
  shows.

Rule-TREE-PANEL-032 holds here too. It says what can be created under **Test
Runs**, and it is on [UC-TREE-PANEL-010](createTestRunPackage.md).

## The Create Test Run dialog

```
┌────────────────────────────────────────────────────────────────────────────┐
│  Create Test Run                                                           │
├────────────────────────────────────────────────────────────────────────────┤
│ ┌────────────────────────────────────────────────────────────────────────┐ │
│ │ v Configuration details                                Collapse  (1)  │ │
│ │   TEST RUN NAME:  [ cycle-2          ]  COMMIT [ 9f3c1ab         ] (2)│ │
│ │   CHANGE LOG      [ Story-002 (register new user), Story-003...     ] │ │
│ │   TEST TYPE                             PLATFORM                      │ │
│ │     (o) Functional Test ( ) Performance   (o) Web  ( ) Mobile          │ │
│ │                                           ( ) Integration             │ │
│ │   COMPONENT                             LANGUAGE                      │ │
│ │     (o) Frontend  ( ) Backend             (o) English ( ) Arabic       │ │
│ │                                           ( ) French                  │ │
│ │   BROWSER                                                        (3)  │ │
│ │     ( ) Chrome (o) Firefox ( ) Safari ( ) Edge                        │ │
│ └────────────────────────────────────────────────────────────────────────┘ │
│ ┌────────────────────────────────────────────────────────────────────────┐ │
│ │ Test Cases                  [tick] (7)  [collapse] (8)  [Filter] (6)  │ │
│ │ [x] v Accounts                                                   (4)  │ │
│ │ [x]     Login                                                         │ │
│ │ [x]     Registration                                                  │ │
│ │ [ ]   Checkout                                                        │ │
│ └────────────────────────────────────────────────────────────────────────┘ │
│                                                     [ Create ]      (5)    │
├────────────────────────────────────────────────────────────────────────────┤
│  Tab Navigate    Space Check    Escape Cancel                              │
└────────────────────────────────────────────────────────────────────────────┘
```

1. **Configuration details** — a section the tester can open and close. The gray
   hint on its right reads *Collapse*, and *Expand* once it is closed. Closing
   it takes every field below with it, the name included.
2. **Test Run name** — filled in with what the tester typed in the first
   dialog. When it is emptied, its gray hint text reads *Cycle-1*. The keyboard
   does not start here. It starts in **Change Log**, which takes as many lines as
   the tester needs: `Ctrl+Enter` inserts a line break, and `Enter` does nothing at
   all - it neither breaks the line nor creates the test run. (Rule-TREE-PANEL-122)
3. **The fields** — one to a row, with every answer on the row. A row shows all of
   its answers and one of them has to be picked: no row offers a blank answer, and
   nothing can be typed into one. Each row carries the icon of the field it asks
   about. **Browser** is on the form only while *Platform* is **Web** and *Component* is **Frontend**. **Device Type**
   takes its place only while *Platform* is **Mobile** and *Component* is **Frontend**. While *Platform* is
   **Integration**, *Component* is **Backend** and **Frontend** cannot be picked (Rule-TREE-PANEL-144). A field that is not on
   the form is not asked and is saved empty. So switching *Platform* from **Web**
   to **Mobile** drops the browser that was picked. **Browser** arrives with *Chrome* already picked, because a test run
   on the web is on Chrome until the
   tester says otherwise; every other row arrives with nothing picked. The
   answers are:

   | Field       | Offers                                |
      |-------------|---------------------------------------|
   | Test Type   | *Functional Test*, *Performance Test* |
   | Platform    | *Web*, *Mobile*, *Integration*        |
   | Component   | *Frontend*, *Backend*                 |
   | Language    | *English*, *Arabic*, *French*         |
   | Browser     | *Chrome*, *Firefox*, *Safari*, *Edge* |
   | Device Type | *iPhone*, *Samsung*, *Huawei*         |

4. **The test cases** — the whole folder tree, with a tick box on every row.
   It starts with the packages and test sets right under **Test Cases**, and
   under them come their packages, test sets and test cases. The tree opens
   fully expanded, and **Collapse All** (8) closes it down to the folders right
   under **Test Cases**. Ticking a folder
   ticks everything under it. An empty test set is left out, and so is a package
   that holds only empty test sets. While a filter is on, ticking a row ticks
   only the test cases it shows. (Rule-TREE-PANEL-131)
5. **Create** — enabled only while at least one test case is ticked and every row
   on the form is answered. While it is disabled, hovering over it says what is
   missing: *Check at least one test case*, or *Pick a Platform*, naming the first
   row with no answer. `Enter` does nothing here. The button is the only way to
   confirm. A click outside the dialog does not close it either. Only `Escape`
   closes it. The line to its left counts what is ticked, and while a filter
   hides ticked test cases it says how many: *1 hidden by the filter*. They
   stay in the test run. (Rule-TREE-PANEL-130)
6. **Filter** — the editor's own Filter button, opening the editor's own menu,
   drawn on [UC-EDITOR-PANEL-020](../editorPanel/filterTestCases.md). Priority,
   Automation, Group and Module narrow the tree as they narrow an editor. **Status** narrows it by the test case's own
   status. There is no **Run Item
   Status**: every run item of a new test run starts Pending. **Test Set** offers the test sets the tree holds, each
   named by its path
   under **Test Cases**, such as *pkg / Checkout*. Every opening starts with no
   filter. (Rule-TREE-PANEL-129)
7. **Select All** — ticks every test case the filter shows, or unticks them all
   once every one of them is ticked. Its picture and tooltip say which:
   *Select All* or *Unselect All*. (Rule-TREE-PANEL-140)
8. **Collapse All** — closes every folder, leaving only those right under
   **Test Cases**, and reads *Expand All* while any folder is closed, opening
   them all again. No tick changes either way. (Rule-TREE-PANEL-145)

**Edit Test Run** is this same dialog with the button **Save**. It opens filled
with the test run's own name, test cases and settings.

## Main flow

1. The tester selects **Test Runs** or a test run package.
2. The tester right-clicks it and chooses **Create**.
3. A dialog titled **Create Test Run Node** opens. Its first row is selected, and
   reads *Records execution results*. Its gray hint text reads *set name, like
   Sprint 3 Cycle 1...*.
4. The tester types a name and presses `Enter`.
5. The **Create Test Run** dialog opens. It holds the typed name in *Test Run
   name*. It also holds the *Configuration details* form, and a tree of test
   sets with every test case ticked but a Disabled one, which is unticked and
   cannot be ticked (Rule-TREE-PANEL-138). Three things are left out of that tree:
   retired test sets, anything under an **Archived** package, and empty test
   sets.
6. The tester ticks and unticks with `Space`, moves with `Tab`, and presses **Create**.
   To build a test run for one purpose, the tester first opens **Filter**,
   chooses **Group**, then **Smoke**, and the tree shows only the Smoke test
   cases.
7. Testin writes the test run. A progress bar reads *Creating test run
   \<name\>*, and it cannot be canceled. Every ticked test case is **Pending**.
   The test run's status is **Created**.
8. Its test run editor opens, and Testin shows *Created*.

## What Testin refuses

**If no test case is ticked** — the **Create** button is disabled, and hovering
over it reads *Check at least one test case*.

**If a row on the form has no answer** — the **Create** button is disabled, and
hovering over it reads *Pick a \<field\>*, naming the first row that has none. (Rule-TREE-PANEL-121)

**If the name has been emptied** — the dialog stays open, and *A test run needs
a name* is shown in red.

**If the name is a path** - a name holding a slash, a backslash or `..` is
refused, reading *'\<name\>' is a path, not a name - a node is one folder inside
the one selected*. In the first dialog or in **Create Test Run**, nothing is
created. (Rule-TREE-PANEL-095)

**If a test run with that name already exists** — *\<name\> Already Exists* is
shown in red. Typing it in the first dialog closes that dialog and the second
one never opens. Typing it in **Create Test Run** leaves the dialog open with
everything still in it.

**If the parent folder was removed while the dialog was open** — the dialog
stays open, and *'\<parent\>' no longer exists - test run not created* is shown
in red.

**If writing the test run fails** — an IDE notification titled *Test Run Not
Created* stays in the notification log, with the reason under it.

**If several nodes are selected** — **Create** works on the first of them.

---

[Documentation](../README.md) › [The tree panel](main.md)
