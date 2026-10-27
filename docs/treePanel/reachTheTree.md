[Documentation](../README.md) › [The tree panel](main.md) › UC-TREE-PANEL-001

# UC-TREE-PANEL-001: Open the panel and reach the tree

> **No key.** The panel opens from the **Testin** button on the IDE's tool
> window bar, on the left.

**As a** tester, **I want** the panel to show my tree, or tell me what to do
first, **so that** I am never stuck on an empty window.

This page says what the panel shows when the tester opens it. There are six
screens, and the tree is one of them. Every one of the other five offers a way
forward, because a screen a tester cannot leave is the thing this page exists to
prevent.

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
- **Rule-TREE-PANEL-014** — The panel never opens on its own when the IDE
  starts.
- **Rule-TREE-PANEL-015** — A code project does not have to name a test project.
  If it names none, and exactly one test project exists in the Testin folder,
  Testin binds it to that one without asking.
- **Rule-TREE-PANEL-097** — Opening the tree panel puts the keyboard in the
  tree. Without that, the IDE has nothing to give focus to, and it stays in
  whatever was there before, so keys pressed over the tree are answered by the
  editor beside it.
- **Rule-TREE-PANEL-100** — A test project that is not active is shown in the
  tree and holds nothing. It is indexed as a node so the tree can say what it
  is, drawn with Inactive beside its name like any other status. Its test sets,
  test cases and test runs are not read, because a project nobody is working on is
  not worth the walk.
- **Rule-TREE-PANEL-104** — A menu entry that cannot work on the selected row is
  gray, and says why when the pointer rests on it.
- **Rule-TREE-PANEL-117** — One thing decides whether text is an address a
  repository can be cloned from, and testin.yml is not it. The file keeps the
  address it was given, with any account or token taken out of it; whether that
  text is an address at all is asked once, at the point where something is
  about to act on it. A file holding an address Testin does not recognize keeps it, so the
  tester can see it and correct it, and is offered no clone.
- **Rule-TREE-PANEL-118** — Until the first test cases are read the panel
  says it is reading, in gray, and nothing else. A test project is found in the
  index, so before the index exists no name resolves - and the screen for a name
  that resolves to nothing is a red line about a project that is merely not read
  yet. The panel says what is happening instead, and leaves that screen by
  itself when indexing ends.
- **Rule-TREE-PANEL-119** — A testin.yml that cannot be read is said once, in a
  notification naming the file, and the panel then answers as though the file
  were absent: the test project this machine picked, or the choose screen. A
  file is corrected in an editor and there is no button that does it, so a
  screen holding only that sentence is a screen with no way off it.
- **Rule-TREE-PANEL-124** — A `location` that is neither `local` nor `remote` is
  read as `local`, and `location: remote` with no `repoUrl` is read as it is
  written. Neither refuses the file, and each is noted in Testin's log.
- **Rule-TREE-PANEL-127** — A test set, a test run and a test project that is not
  active show no expand arrow. A test set and a test run hold test cases and run
  items rather than nodes, and an inactive test project holds nothing
  (Rule-TREE-PANEL-100), so the tree has nothing to open under them.
- **Rule-TREE-PANEL-142** — The tree is drawn as soon as a test project's test
  cases are read, and again when its test runs are. Until its test runs are read
  for the first time - when the IDE starts, and on Refresh - the Test Runs
  folder says Reading test runs in gray and holds nothing. A read canceled or
  stopped before them leaves it saying Not read, until a read gets them in.

Rule-TREE-PANEL-100 also holds here. It says a test project that is not **Active** is drawn in the tree and holds
nothing. It is written on
[UC-TREE-PANEL-018](retireNode.md).

## No Testin folder

```
┌────────────────────────────────────────────────────────────────────────────┐
│                                                                            │
│  Welcome to Testin                                                         │
│                                                                            │
│  The new awesome test management tool                                      │
│                                                                            │
│  By                                                                        │
│  Muteb AlMughyiri                                                          │
│                                                                            │
│                                                                            │
│  [gear]  Configure Testin settings                                 (1)     │
│                                                                            │
└────────────────────────────────────────────────────────────────────────────┘
```

1. **The header** — these five lines start every welcome screen. The panel has
   five of them, and each one begins the same way.
2. **The one link.** Nothing else is offered. Nothing else can work until a
   Testin folder is set. **Select Test Project** and **New Test Project** in the
   toolbar are gray.

## Choose a project

```
┌────────────────────────────────────────────────────────────────────────────┐
│                                                                            │
│  testin.yml names Payments, which is not in the Testin folder       (1)    │
│                                                                            │
│  [dir]  Demo  Active                                                (2)    │
│  [dir]  Mobile  Active                                                     │
│  [dir]  Legacy  Archived                                                   │
│                                                                            │
└────────────────────────────────────────────────────────────────────────────┘
```

1. **The reason** — in red. It is shown when this code project named a test
   project Testin could not use. The reason is one of:
    - *not in the Testin folder*
    - *could not be read*
2. **One link per test project** — the name, then the status. The tester
   clicks a link. This code project is then set to use that test project, and
   the tree appears. With more than six test projects, Testin shows one link
   instead of a list. That link opens the **Select Test Project** dialog.

Three more screens follow. The sixth is the tree itself.

- *Reading test projects...*, a gray line and nothing else. It is what a cold
  start shows while the first index is being built, and it goes when the index
  is done. Nothing is offered on it because the only thing to do is wait (Rule-TREE-PANEL-118).
- *\<name\> is not on this machine yet*, a gray line, then the link **Clone \<name\>**, and under it **Choose another
  test project** - or **Create your first test project** when the Testin folder holds none. The
  clone is what the screen is for, and it is not always an offer: without the
  Git plugin it is gray, and the name came from a file a colleague committed.
- **Create your first test project**, a link with no gray line above it.

## Main flow

The panel shows one of six screens. It checks for them in the order of the
table below. Each screen starts with the same header, drawn above. After the
header come the screen's own line and its own links.

The choose screen holds one link per test project, up to six of them. The others
hold one link each.

| If                                                                                                                                         | The panel shows                                                                                                                                                                      |
|--------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| No Testin folder is set                                                                                                                    | *Welcome to Testin* and the link **Configure Testin settings**                                                                                                                       |
| The bound test project is found                                                                                                            | The tree                                                                                                                                                                             |
| The first index has not finished                                                                                                           | *Reading test projects...*, in gray                                                                                                                                                  |
| This code project's `testin.yml` names the test project the tree would show, it is not on this machine, and the file gives its Git address | *\<name\> is not on this machine yet*, the link **Clone \<name\>**, and **Choose another test project**                                                                              |
| No test project exists in the Testin folder                                                                                                | The link **Create your first test project**                                                                                                                                          |
| Otherwise                                                                                                                                  | One link per test project, showing its name and then **Active** or **Inactive**. With more than six test projects, one link instead: **Select the test project for this repository** |

## What Testin refuses

**If the Git plugin is not installed** — the clone line still shows, gray,
reading *Clone \<name\> (needs the Git plugin)*, and clicking it does nothing (Rule-TREE-PANEL-104).

**If the tester chose a project the file does not name, and it is missing** -
no clone is offered: the file's address is for the project it names, not for
the tester's pick. The panel lists the test projects to choose from instead,
or offers to create the first one when there are none.

**If the project file cannot be read** — the panel shows, in red, *testin.yml
names \<name\>, which could not be read*.

**If `testin.yml` is malformed** — Testin says so once, in a notification
titled *testin.yml could not be read* and reading *Fix the file and press
Refresh - the reason is in the Testin log*. The panel then shows whatever it
would show if the file were not there at all: the test project this machine
picked, or the choose screen (Rule-TREE-PANEL-119). It used to be a screen of
its own, holding those two lines and nothing else - and a file is corrected in
an editor, so that screen had no way off it.

**Testin writes `testin.yml` only when the tester presses Save to testin.yml**
(Rule-TREE-PANEL-112). It reads it when the code project has one, and goes on
without it when it does not - only the automation code stays off (Rule-INTERNAL-089, Rule-CODEGEN-082).

---

[Documentation](../README.md) › [The tree panel](main.md)
