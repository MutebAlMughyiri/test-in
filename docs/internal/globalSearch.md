[Documentation](../README.md) › [Inside Testin](main.md) › UC-INTERNAL-001

# UC-INTERNAL-001: Find anything in the test project

> **`Ctrl+Alt+F`**, and **`Cmd+Alt+F`** on a Mac. It works anywhere in the
> IDE, not only in the tree. There is also a **Search Test Project** button
> at the top of the tree panel.

**As a** tester, **I want** to type part of a name, a step or a test case number
and jump to it, **so that** a large tree is never a place to scroll.

This is the fastest way to reach one test set, one test run or one test case.

## Rules

- **Rule-INTERNAL-001** — Nodes are found by name, from the first character
  typed. Test cases are found from the second character on, and every field of a
  test case is searched:
    1. its description, its UUID, its steps and its expected result
    2. its priority, its status, its groups and its module
    3. its preconditions, its reference and its test data
    4. where it sits in the tree, and its generated code
    5. who created or last changed it, and when

  Capitals do not matter, and spaces at either end are dropped.
- **Rule-INTERNAL-002** — Choosing a result always takes the tree to it. Only a
  test set and a test run open an editor.
- **Rule-INTERNAL-066** — A key a tester can rebind is declared to the IDE, so
  Find Action offers the action by name and Settings > Keymap holds the key; a
  key that is a surface's own gesture - Enter, Delete, the clipboard three, a
  bare letter - is registered on that surface instead, because a declared
  shortcut is dispatched before a component's own bindings and would take the
  key away from the tree, the card list or the grid. Which keys are which is
  written out on the shortcuts page.
- **Rule-INTERNAL-068** — A key a surface gives an action is bound on a copy of
  that action, never on the one the IDE holds. The Keymap page keeps showing
  what it shipped with, and the tooltip that prints the key reads it from the
  same place the binding does.
- **Rule-INTERNAL-069** — Where Testin takes a default key the IDE also uses,
  and the IDE's action and Testin's can fire in the same place, the
  descriptor says which and why beside the binding. There are two. `F12` on
  Automate Test Case is also Jump to Last Window; `CTRL+F12` was worse, because
  it is FileStructurePopup's, and so is `Cmd+F12` on a Mac, which left
  generating code unreachable by key there. The Mac key is `Ctrl+Alt+Cmd+F12`.
  `Ctrl+Alt+F` on Search is also Introduce Field anywhere in a Java file, and
  [every shortcut](../shortcuts.md) explains why it is kept. When two actions
  claim one keystroke the IDE runs whichever is enabled and says nothing, so the
  note is what tells the next reader the clash was chosen.
- **Rule-INTERNAL-071** — A default key carries one action. Two may share one
  only where their conditions cannot both hold at once, which has to be argued
  rather than assumed: `F2` works because each of its three needs a selection in
  its own surface, and `Ctrl+M` did not, because creating a test case needs only
  an open editor and so was live in the tree as well. Where two are enabled the
  IDE runs whichever is declared first and says nothing.
- **Rule-INTERNAL-072** — What kind of thing a row is, its icon says and
  nothing else does. It is the icon the tree draws for that node, so a test set
  looks in the search as it looks in the tree. A word beside the name was built
  and taken out again: the icon already carries it, and the row is for reading
  the name.
- **Rule-INTERNAL-073** — The search says how many things matched, beside what
  was typed. That is not how many it shows: the list stops at fifty.
- **Rule-INTERNAL-074** — Nothing in a search row is lined up into a column.
  The path sits against the name it belongs to, whatever length the name is.
- **Rule-INTERNAL-098** — A test case UUID also finds the test runs that ran it:
  one row for each test run holding a run item status for a test case that
  matched, after the test cases themselves. The row carries the test run's icon
  and the test run's path, because Rule-INTERNAL-072 says the icon is the tree's
  icon for the node the row opens, and the path is what tells one test run from
  another. A test run that does not cover the test case is not a row.

The tree panel's own rules do not govern this dialog. It opens from anywhere in
the IDE, and the tree is only where it lands.

## The Search Test Project dialog

```
┌────────────────────────────────────────────────────────────────────────────┐
│  Search Test Project                                                       │
├────────────────────────────────────────────────────────────────────────────┤
│                                                                            │
│  [find]  login                                     49 found      (1)       │
│                                                                            │
│  [set]  Login                                                    (2)       │
│         Demo > Test Cases > Accounts > Login                               │
│  [run]  cycle-2                                                            │
│         Demo > Test Runs > Sprint 7 > cycle-2                              │
│  [tc]   Sign in with a correct username and password             (3)       │
│         Demo > Test Cases > Accounts > Login                               │
│  [run]  Sign in with a correct username and password             (4)       │
│         Demo > Test Runs > Sprint 7 > cycle-2                              │
│                                                                            │
├────────────────────────────────────────────────────────────────────────────┤
│  Enter Go To    ↑ ↓ Select    Escape Cancel                                │
└────────────────────────────────────────────────────────────────────────────┘
```

1. **The field** — its gray hint text reads *Go to a test set or test run, or
   search for anything...*. With nothing typed, the list holds test sets and
   test runs only. Those are the ones with something to open. Once the tester
   types, packages, the two containers and the test project row match by name
   too — and the end of the field says how many matched, in gray.
2. **A result row** — the icon, the name, then the path after it in gray. The
   icon is what says which kind of thing the row is: the same one the tree draws
   for it. Twelve rows are visible and the rest scroll. The icon at the front of
   the field changes to the icon of whichever row is selected.
3. **Nothing is lined up into columns.** The path sits against the name it
   belongs to, whatever length the name is.
4. **A test run that ran it.** Searching a test case UUID lists the test case,
   then one row for every test run that recorded a run item status for it. The
   row reads the same as the test case's own, so the path under it is what says
   which test run it was, and the icon is the test run's.
5. **At most 50 rows in all.** Nodes are taken first, so 50 matching nodes leave
   no room for test cases. The count beside the field is what matched, so a
   common word reads *612 found* over a list of fifty — which is how the tester
   can tell there is more. The top row is selected as soon as the list refills,
   so `Enter` goes there without pressing `↓`. `↑` and `↓` stop at the ends.

## Main flow

1. The tester presses `Ctrl+Alt+F` anywhere in the IDE, or `Cmd+Alt+F` on a
   Mac, or presses the **Search Test Project** button in the panel header.
2. The **Search Test Project** dialog opens. Before anything is typed, it lists
   every test set and test run.
3. The tester types.
4. Testin lists every node whose name holds what was typed, from the first
   character on. It lists test cases from the second character on. Nodes come
   first, shortest name first. Test cases come after them, and after those, a
   row for every test run holding a run item status for a test case that
   matched.
5. The tester moves with `↑` `↓` and presses `Enter`, or clicks a row.
6. The panel comes forward, and the tree expands to that node and selects it.
7. A test set or a test run opens in its editor. Any other node is only revealed
   in the tree.
8. A test case opens its test set in the test set editor with that test case
   selected, and fills the view panel on the right with it.
9. A test run row opens that test run in the test run editor instead, with the
   same test case selected, so the panel shows what that test run recorded
   rather than the test case alone.

## What Testin refuses

**If nothing matches** — the list is empty. `Enter` does nothing, and the dialog
stays open.

**If no Testin folder is set, or nothing is indexed yet** — the dialog still
opens. It lists nothing. The key and the button are gray only while no code
project is open in the IDE.

**If a package, a container or the test project row is chosen** — the tree goes
to it and nothing opens. Testin says nothing.

---

[Documentation](../README.md) › [Inside Testin](main.md)
