[Documentation](../README.md) › [The tree panel](main.md) › UC-TREE-PANEL-029

# UC-TREE-PANEL-029: Save the test project to testin.yml

> **Save to testin.yml**, the eighth button at the top of the panel. It has no
> key.

**As a** tester, **I want** to name the open test project in `testin.yml` with
one button, **so that** its automation code is generated here and a colleague
who opens this code project finds the same test project.

This is the only thing in Testin that writes `testin.yml`. Opening, picking,
creating, cloning, renaming, committing and pushing never do.

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
- **Rule-TREE-PANEL-100** — A test project that is not active is shown in the
  tree and holds nothing. It is indexed as a node so the tree can say what it
  is, drawn with Inactive beside its name like any other status. Its test sets,
  test cases and test runs are not read, because a project nobody is working on is
  not worth the walk.
- **Rule-TREE-PANEL-104** — A menu entry that cannot work on the selected row is
  gray, and says why when the pointer rests on it.
- **Rule-TREE-PANEL-112** — Only **Save to testin.yml** writes `testin.yml`.
  Opening, picking, creating, cloning, renaming, committing and pushing never
  do.
- **Rule-TREE-PANEL-113** — **Save to testin.yml** names the test project open
  in the tree, and where it is cloned from when its folder is a Git repository
  with a remote - without any account or token. It shows the file and each line
  first, and writes nothing until the tester presses **Save**.
- **Rule-TREE-PANEL-114** — It changes only `testinProject`, `location` and
  `repoUrl`. Every other line of the file - comments, `bugRepoUrl`, keys Testin
  does not know - stays as it was.

## The preview

```
┌──────────────────────────────────────────────────────────────┐
│  Save to testin.yml                                          │
├──────────────────────────────────────────────────────────────┤
│  C:\Users\...\testin_example\testin.yml                  (1) │
│                                                              │
│  testinProject: NAFATH             was Checkout          (2) │
│  location: remote                  was local                 │
│  repoUrl: https://github.com/acme/nafath-test-cases.git  new │
│                                                              │
│  Everything else in the file stays as it is.                 │
│  The file is committed: this names NAFATH for everyone who   │
│  opens this code project.                                (3) │
├──────────────────────────────────────────────────────────────┤
│  Enter Save    Escape Cancel                                 │
└──────────────────────────────────────────────────────────────┘
```

1. **The file** - the one there is, `testin.yml` or `testin.yaml`, or
   `testin.yml` in the code project's folder when there is none.
2. **Each line as it will be**, and what that changes: *was ...*, *new* or *unchanged*.
3. **The reminder** that the file is the team's: it is committed.

## Main flow

1. The tester presses **Save to testin.yml** at the top of the Testin panel.
2. Testin works out the lines. `testinProject` is the test project open in the
   tree. When its folder is a Git repository with a remote, `location: remote`
   and `repoUrl`, the remote's address without any account or token; when it
   has no remote, `location: local`.
3. The preview opens.
4. The tester presses `Enter`. Testin writes the lines, reads the file again,
   and shows *Saved*.
5. Code turns on for this test project: generating, **Automate Test Case**, **Navigate to Test Method**, **Run Tests**,
   the gutter icons and the automated marks (Rule-CODEGEN-082).

## When code is off

Code is off while `testin.yml` does not name the test project open in the tree:
there is no file, it has no `testinProject`, or it names another. The first time
a change in the tree would have written code, a notification titled *Code Not
Generated* says why, with **Save to testin.yml**. It says so once per project.

## What Testin refuses

**If no test project is open** - the button is gray, and says *Open a test
project first: its name is what the file gets.*

**If testin.yml cannot be read** - the button is gray, and says *testin.yml
cannot be read - correct it by hand first.* Lines written into a broken file
could leave it broken, and make code look on.

**If the Git plugin is not installed** - only `testinProject` is written;
`location` and `repoUrl` stay as they are, rather than calling a Git project
local.

**If the file cannot be written** - nothing changes, and *testin.yml could not
be saved. The log says why.* is shown in red.

**If the tester presses Escape** - nothing is written.

---

[Documentation](../README.md) › [The tree panel](main.md)
