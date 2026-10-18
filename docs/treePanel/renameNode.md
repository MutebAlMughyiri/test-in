[Documentation](../README.md) › [The tree panel](main.md) › UC-TREE-PANEL-011

# UC-TREE-PANEL-011: Rename a node

> **`Shift+F6`**, with a test project, a test set, a package or a test run
> selected. On the menu: **Actions → Rename**.

**As a** tester, **I want** to rename a test project, a test set, a package or
a test run, **so that** the tree says what things are called now. A project
cloned under its repository's name can take the name the team uses.

This changes the name of one node. Nothing inside it moves.

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
- **Rule-TREE-PANEL-036** — Renaming a test project, a test set or a test set
  package renames its automation code with it, so the test case stays runnable.
- **Rule-TREE-PANEL-037** — A rename can be undone.
- **Rule-TREE-PANEL-100** — A test project that is not active is shown in the
  tree and holds nothing. It is indexed as a node so the tree can say what it
  is, drawn with Inactive beside its name like any other status. Its test sets,
  test cases and test runs are not read, because a project nobody is working on is
  not worth the walk.
- **Rule-TREE-PANEL-104** — A menu entry that cannot work on the selected row is
  gray, and says why when the pointer rests on it.
- **Rule-TREE-PANEL-110** — Renaming a test project never writes `testin.yml`.
  The test project chosen for this code project follows the rename. When
  `testin.yml` still names the old name, Testin says so once and opens the file
  on request.
- **Rule-TREE-PANEL-111** — A rename closes every editor open on the node or
  under it, light mode included, before anything moves. While one of them is
  busy - a test run executing, a cell being edited - **Rename** is gray and says
  why.
- **Rule-TREE-PANEL-123** — The rename dialog says what it is renaming and where
  it sits, above the field, so the name being replaced can still be read once
  typing has begun.
- **Rule-TREE-PANEL-133** — A rename whose folder cannot be renamed changes
  nothing. The automation code takes its old name back and the test project
  keeps its name, and the tester sees Rename Failed.

## The Rename dialog

```
┌──────────────────────────────────────────────────────────────┐
│  Rename                                                      │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌────────────────────────────────────────────────────────┐  │
│  │  RENAMING              IN                         (1)  │  │
│  │  [set] Accounts        Demo › Test Cases               │  │
│  │                                                        │  │
│  │  NEW NAME ─────────────────────────────────────── (2)  │  │
│  │  [edit]  Accounts                                 (3)  │  │
│  └────────────────────────────────────────────────────────┘  │
│                                            [ Rename ]   (4)  │
├──────────────────────────────────────────────────────────────┤
│  Enter Rename    Escape Cancel                          (5)  │
└──────────────────────────────────────────────────────────────┘
```

1. **What is being renamed, and where it sits** — two captioned facts side by
   side: the node's own kind of icon with its name, and its place in the test
   project. The tester can still read the name being replaced after typing has
   started (Rule-TREE-PANEL-123). The place leaves the drive, the download folder
   and the Testin folder off, with a chevron between the segments (Rule-INTERNAL-108).
2. **The caption over the field**, carrying the one hairline a dialog draws (Rule-INTERNAL-087).
3. **One field**, filled in with the current name. Its gray hint text reads *set new name...*. The cursor sits after the
   name. So typing **adds to the
   name** instead of replacing it. `Ctrl+A` selects the whole name. Spaces at
   either end are dropped before anything else happens.
4. **The button** — colored, and named for what it does (Rule-INTERNAL-105).
5. The key that confirms is named for the same thing the button is.

## Main flow

1. The tester selects a test project, a test set, a package or a test run.
2. The tester presses `Shift+F6`, or chooses **Actions → Rename**.
3. The **Rename** dialog opens. It says what it is renaming and where that sits,
   with the current name filled in below.
4. The tester types the new name and presses `Enter`, or the **Rename** button.
5. Testin closes every open editor on the node or under it, whatever kind it
   is, and light mode with them.
6. For a test project, a test set or a test set package, Testin renames the
   automation code next. It does so only while code is on: the Java plugin, and
   `testin.yml` naming the test project by its old name or its new one (Rule-CODEGEN-082).
7. Testin renames the folder.
8. For a test project, the project chosen for this code project follows the new
   name. When `testin.yml` still names the old one, a notification that stays in
   the log says *testin.yml still names \<old\>. Change testinProject to
   \<new\> there, or press Save to testin.yml in the Testin panel*, with **Save to testin.yml** and **Open testin.yml**.
   The rename itself never
   writes the file; only Save to testin.yml does (Rule-TREE-PANEL-112).
9. Testin refreshes the tree and shows *Renamed*.
10. `Ctrl+Z` puts the old name back, and Testin shows *Undone*. The automation
    code goes back with it - for the test project `testin.yml` names as well.

## What Testin refuses

**If the name is empty** — the gray hint text turns red, and the dialog stays
open.

**If the name is a path** - a name holding a slash, a backslash or `..` is
refused, reading *is a path, not a name - a node is one folder inside the one
selected*. Nothing is renamed.

**If the name is unchanged** — the dialog closes and nothing happens, silently.
Renaming `Accounts` to `  Accounts  ` counts as unchanged, because the spaces
are dropped first.

**If a sibling already has the new name** — nothing is renamed, and *\<name\>
Already Exists* is shown in red. For a test project the siblings are the other
folders in the Testin folder. A rename that only changes the capitals of the name is
not in its own way.

**If the automation code already has the package the new name makes** — nothing
is renamed, neither the folder nor the code, and *Package \<package\> Already
Exists* is shown in red (Rule-CODEGEN-080).

**If the IDE is indexing** — renaming a test project, a package or a test set is
refused with *Rename needs the IDE to finish indexing first*, and nothing is
renamed (Rule-CODEGEN-081). A test run has no code, and renames as usual.

**If the name cannot be a Java package** — the dialog stays open with the name
still in the box, and *'\<name\>' cannot name a Java package* is shown in red.
`New`, `Class` and `Import` are the usual ones: Java keeps those words for
itself. (Rule-TREE-PANEL-095) Only the test project and the test set
packages are asked: a test set's name becomes the class, which always ends in
`Test`, and a test run generates no code at all.

**If a container is selected** — **Rename** is gray, and `Shift+F6` does
nothing (Rule-TREE-PANEL-002).

**If an editor under the node is busy** — a test run executing, or a cell being
edited: **Rename** is gray, and says *A test run in it is executing, or a cell
is being edited. Finish it first* (Rule-TREE-PANEL-111).

**If the folder cannot be renamed on disk** — nothing is renamed and no *Renamed* is shown. An IDE notification titled
*Rename Failed* stays in the
notification log, reading *Operation failed:* and the reason, or *Could not find
path on disk:* and the path. The other refusals above are balloons that fade.
This one is not.

**If several rows are selected** — **Rename** is gray.

**If the Java plugin is not installed** — the first rename in the project shows *Java Plugin Not Available*, reading
*Automation code generation and navigation
require the Java plugin, which is not available in this IDE.* The rename still
happens. Testin says this once per project.

**If the folder cannot be renamed after the automation code was** — the code
already has the new name. Renaming again once the folder is free finishes the
job: there is no code left to move, and the folder follows.

---

[Documentation](../README.md) › [The tree panel](main.md)
