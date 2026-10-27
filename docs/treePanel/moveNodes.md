[Documentation](../README.md) › [The tree panel](main.md) › UC-TREE-PANEL-013

# UC-TREE-PANEL-013: Move nodes

> **`Ctrl+X`** cuts, **`Ctrl+V`** pastes, and **`Escape`** takes the gray off.
> Dragging a node onto a folder moves it too. On the menu: **Actions → Cut** and
> **Paste**.

**As a** tester, **I want** to move a node into another folder, **so that** the
tree can be reorganized without building anything again.

The node keeps everything inside it. Only where it sits changes.

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
- **Rule-TREE-PANEL-043** — A node lands only where its kind belongs. A test set
  or a test set package lands under **Test Cases** or under a test set package.
  A test run or a test run package lands under **Test Runs** or under a test run
  package. (Rule-TREE-PANEL-003)
- **Rule-TREE-PANEL-044** — Nothing can be pasted or dropped into a test set or
  into a test run. Neither of them holds nodes.
- **Rule-TREE-PANEL-045** — A node cannot land on itself, inside itself, or in
  the folder it already sits in.
- **Rule-TREE-PANEL-046** — Cut then paste moves. Dragging moves. Dragging with
  the copy key held copies instead, which is [UC-TREE-PANEL-014](copyNodes.md).
- **Rule-TREE-PANEL-047** — A move can be undone.
- **Rule-TREE-PANEL-048** — Moving a test set or a test set package moves its
  automation code with it, so the test cases stay runnable.
- **Rule-TREE-PANEL-049** — Nodes drop onto a node, never between two. Position
  is set by ordering, in [UC-TREE-PANEL-015](orderNodes.md), not by dragging.
- **Rule-TREE-PANEL-050** — `Escape` empties the clipboard of the nodes the
  tester cut or copied. Nothing is left waiting to be pasted.
- **Rule-TREE-PANEL-098** — A move that the tree refuses leaves the generated
  code where it was. The Java moves before the node, because the old path is
  what finds it, so a node move that fails puts the code back rather than
  leaving the test set in one package and its class in another.
- **Rule-TREE-PANEL-100** — A test project that is not active is shown in the
  tree and holds nothing. It is indexed as a node so the tree can say what it
  is, drawn with Inactive beside its name like any other status. Its test sets,
  test cases and test runs are not read, because a project nobody is working on is
  not worth the walk.
- **Rule-TREE-PANEL-104** — A menu entry that cannot work on the selected row is
  gray, and says why when the pointer rests on it.
- **Rule-TREE-PANEL-105** — A node dragged out of the IDE, onto the desktop or a
  file manager, carries nothing. Test data is moved only by Testin, so the tree
  and the folders on disk cannot disagree.

## The Paste, Move and Copy dialog

```
┌──────────────────────────────────────────────────────────────┐
│  Paste                                                       │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌────────────────────────────────────────────────────────┐  │
│  │  Move 'Login' into 'Smoke'?                       (1)  │  │
│  │                                                        │  │
│  │  Demo › Test Cases → Demo › Test Cases › Smoke    (2)  │  │
│  └────────────────────────────────────────────────────────┘  │
│                                              [ Move ]   (3)  │
├──────────────────────────────────────────────────────────────┤
│  Enter Move    Escape Cancel                            (4)  │
└──────────────────────────────────────────────────────────────┘
```

1. **The question** — says the verb and the names. The verb is *Move* after a
   cut. It is *Copy* after a copy. It sits on a card (Rule-INTERNAL-099).
2. **Where it is going** — one row, not two labeled blocks: where the nodes are
   now, an arrow, then where they land (Rule-INTERNAL-109). Both are said as
   places in the test project rather than as paths on disk (Rule-INTERNAL-108).
   The segment the destination gains is in ordinary text while the rest stays
   gray, so the tester reads the difference instead of comparing two long lines.
   When nodes were cut from several folders, the left side names only the first
   folder.
3. **The button** — colored, and named for the verb (Rule-INTERNAL-105).
4. The key that confirms is named for the verb too.

After a paste, the title reads **Paste**. After a drop, the title reads the
verb, **Move** or **Copy**. Everything else on the dialog is the same.

## Main flow

**Cut and paste**

1. The tester selects one or more test sets, packages or test runs.
2. The tester presses `Ctrl+X`, or chooses **Actions → Cut**. The nodes are
   drawn gray, and Testin shows *Cut*, or *Cut N* for several.
3. The tester selects a folder that can hold them: **Test Cases**, **Test
   Runs**, or a package of the right family.
4. The tester presses `Ctrl+V`, or chooses **Actions → Paste**.
5. The **Paste** dialog asks *Move '\<name\>' into '\<folder\>'?*, with one row
   saying where the nodes are and where they land.
6. The tester presses `Enter`, or the **Move** button. The nodes move, and Testin shows *Moved*, or *Moved N*.

**Drag and drop**

1. The tester drags one or more nodes. A small label follows the mouse. It
   reads *'\<name\>'*, or *N nodes* for several.
2. Over a row that cannot take them, the pointer turns into the no-entry sign.
   The row does not light up.
3. The tester releases on a folder that can hold them. The **Move** dialog asks
   the same question, and `Enter` moves them.
4. Nothing can be dropped between two rows.

**Canceling a cut**

1. The tester presses `Escape` in the tree.
2. The gray comes off the nodes.

## What Testin refuses

**If the selected node cannot take what is on the clipboard** — nothing moves,
and *Select a folder* is shown in red. Six places cannot take it:

- a test set or a test run
- the node being pasted
- something inside the node being pasted
- the folder the node already sits in
- a node on the other side of the tree
- a node in another test project

On a test project or a test set, **Paste** is gray already. Nothing happens at
all there.

**If the destination already holds a node with the same name** — that node stays
where it is, and *'\<name\>' already exists in '\<folder\>'* is shown in red,
or *N nodes already exist in '\<folder\>'*. The other nodes in the same paste
still move.

**If nodes were cut in one test project and the code project was then bound to
another** — *Select a folder* is shown, and nothing moves.

**If the move fails on disk** — an IDE notification titled *Move Failed* stays
in the notification log, reading *Operation failed:* and the reason, or *Could
not find source or target path on disk.*

**If the test project or a container is selected** — **Copy** and **Cut** are
gray. Such a node cannot be moved or copied.

**If several rows are selected** — **Paste** stays black and pastes into the
first of them.

**If a node is dragged out of the IDE** — onto the desktop, a file manager or
another application — nothing is dropped there. Test data is moved only by
Testin (Rule-TREE-PANEL-105).

**If the Java plugin is not installed** — the first move in the project shows *Java Plugin Not Available*. The move
still happens.

**After a move** — the clipboard is empty and **Paste** is gray again. The
nodes were moved once; offering to move them a second time from a folder they
have left would find nothing there. A copy is different: it stays on the
clipboard and can be pasted as often as the tester likes.

**After `Escape`** — the clipboard is empty too, and the faded rows come back
to normal. Nothing is left waiting.

**After a drag** — nothing changes about the cut. A drag is its own move and
says nothing about the clipboard, so rows cut and not yet pasted stay faded and
stay pastable.

---

[Documentation](../README.md) › [The tree panel](main.md)
