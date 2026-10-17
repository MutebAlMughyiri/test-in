[Documentation](../README.md) › [The tree panel](main.md) › UC-TREE-PANEL-026

# UC-TREE-PANEL-026: Switch the Git branch of the test project

> **No key.** Pick a branch from the box above the tree, or reach it with `Tab`,
> move with the arrow keys and press `Enter`.

**As a** tester, **I want** to switch the test project's branch from the panel, **so that** the tree follows the branch
I am testing.

The box above the tree appears for a test project whose folder is a Git
repository.

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
- **Rule-TREE-PANEL-085** — Switching with uncommitted changes asks first.
  Switching never loses them.
- **Rule-TREE-PANEL-086** — A switch that succeeds does a full refresh: it
  re-reads which test project is bound, reads the test project again, reloads
  every open Testin editor, and **closes any editor whose node the new branch
  does not have**.
- **Rule-TREE-PANEL-100** — A test project that is not active is shown in the
  tree and holds nothing. It is indexed as a node so the tree can say what it
  is, drawn with Inactive beside its name like any other status. Its test sets,
  test cases and test runs are not read, because a project nobody is working on is
  not worth the walk.
- **Rule-TREE-PANEL-104** — A menu entry that cannot work on the selected row is
  gray, and says why when the pointer rests on it.
- **Rule-TREE-PANEL-108** — The branch box appears when the test project's
  folder is a Git repository, with or without a `testin.yml`.
- **Rule-TREE-PANEL-132** — The branch box takes the keyboard. The arrow keys
  move through the branches and Enter switches, so moving through the list never
  checks a branch out. A screen reader hears it as Branch.

## The Uncommitted Changes dialog

```
┌──────────────────────────────────────────────────────────────┐
│  Uncommitted Changes                                         │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│  3 changes in this test project are not committed.       (1) │
│  Switching does not leave them behind - they come with       │
│  you, and can be committed onto release by mistake.          │
│                                                              │
│  From:  main                                             (2) │
│  To:    release                                              │
│                                                              │
├──────────────────────────────────────────────────────────────┤
│  [ Review Changes ]  [ Switch Anyway ]                   (3) │
│  Enter Switch Anyway   Escape Cancel                         │
└──────────────────────────────────────────────────────────────┘
```

1. **How many changes** — and the one sentence that matters. Switching does
   not leave the changes behind. They come with the tester. They can be
   committed onto the wrong branch by mistake.
2. **From** and **To** — the branch the tester is on, and the branch they
   picked.
3. **Two buttons** — **Switch Anyway**, also `Enter`, switches anyway. **Review Changes** opens the review instead.

## Main flow

1. The test project's folder is a Git repository.
   A drop-down box above the tree lists its branches in alphabetical order. The
   current branch is selected. Remote branches are listed too, without their
   `remotes/` prefix.
2. The tester picks another branch. A progress bar reads *Checking branch
   \<branch\>*, then *Checking out branch: \<branch\>*.
3. Testin checks out the branch. A remote branch with no local branch yet gets
   one, and the new local branch follows it. The message then names the local
   branch.
4. Testin does a full refresh, and shows *Switched to \<branch\>*. An editor
   whose node the new branch does not have is closed.

The box fills itself from what is on the machine, showing *Reading branches*.
That is the whole answer to almost every question anyone asks it, and it needs
no network.

Three things also ask the remote, and then the box fills a second time showing *Fetching from remote*, so the list can
grow a moment after it appears:
**Refresh**, a branch switch, and opening a different test project. Nothing else
does. A rename, a removal or a status change rebuilds the panel without going
near the remote. It cannot have moved a branch, and a fetch can stop to ask for
credentials or sit on a host that is not reachable.

**With uncommitted changes**

1. The box goes back to the branch the tester was on.
2. The **Uncommitted Changes** dialog says how many changes there are, and that
   they would move to the new branch too.
3. `Enter`, which is **Switch Anyway**, runs the switch as above.
4. The **Review Changes** button opens the list of changes not yet committed
   instead.
5. `Escape` changes nothing.

## What Testin refuses

**If the test project's folder is not a Git repository** — there is no box, and *Not a Git repository* is shown in its
place.

**If the Git plugin is not installed** — there is no box, the same as for a
folder that is not a Git repository: without the plugin no folder is asked.

**If Git refuses the checkout** — the box goes back to the branch the tester was
on. An IDE notification titled *Branch Not Switched* opens. It says *\<branch\>
was not checked out. There are uncommitted changes in this test project that
switching would overwrite - commit them first.* It carries a **Review Changes**
link.

**If the branches cannot be read** — the box reads *Failed to load branches* and
cannot be opened. An IDE notification titled *Git Error* gives the reason.

**If the repository has no branches** — the box reads *No branches found* and
cannot be opened.

**If Git is on no branch at all** — the box lists the branches and shows none of
them selected. A checkout of a tag or a commit made outside Testin, bisecting, or
a rebase stopped partway all leave the repository like this, and there is no
branch to name. Picking one from the list switches to it, which is the way out.

**While the branches are still loading** — the box reads *Loading branches...*
and cannot be opened.

---

[Documentation](../README.md) › [The tree panel](main.md)
